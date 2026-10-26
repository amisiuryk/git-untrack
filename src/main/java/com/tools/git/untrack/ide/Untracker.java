package com.tools.git.untrack.ide;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DoNotAskOption;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsFileUtil;
import com.intellij.vcsUtil.VcsUtil;
import com.tools.git.untrack.core.RepoPaths;
import com.tools.git.untrack.core.UndoCheck;
import com.tools.git.untrack.core.UntrackPlan;
import com.tools.git.untrack.git.GitIndexCommands;
import com.tools.git.untrack.settings.GitUntrackSettings;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Untrack, force untrack and undo untrack, across all repositories of a selection. */
public final class Untracker {
    private final Project project;

    public Untracker(Project project) {
        this.project = project;
    }

    /** Collects the files to untrack in the background, confirms, then untracks them. */
    public void untrack(Collection<FilePath> selection) {
        boolean includeMissing = GitUntrackSettings.getInstance().isIncludeMissingFiles();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Collecting files to untrack", true) {
            private final Map<GitRepository, UntrackPlan> plans = new LinkedHashMap<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
                Map<String, GitRepository> repositories = new LinkedHashMap<>();
                Map<String, FilePath> selected = new LinkedHashMap<>();
                selection.forEach(path -> selected.put(path.getPath(), path));
                Map<String, List<String>> byRoot = RepoPaths.groupByRoot(selected.keySet(), path -> {
                    GitRepository repository = manager.getRepositoryForFile(selected.get(path));
                    if (repository == null) {
                        return null;
                    }
                    repositories.put(repository.getRoot().getPath(), repository);
                    return repository.getRoot().getPath();
                });
                Map<GitRepository, String> errors = new LinkedHashMap<>();
                byRoot.forEach((root, relative) -> {
                    indicator.checkCanceled();
                    GitRepository repository = repositories.get(root);
                    try {
                        UntrackPlan plan = UntrackPlan.of(relative, new GitIndexCommands(project, repository).snapshot(relative), includeMissing);
                        if (!plan.isEmpty()) {
                            plans.put(repository, plan);
                        }
                    } catch (VcsException e) {
                        errors.put(repository, e.getMessage());
                    }
                });
                if (!errors.isEmpty()) {
                    UntrackNotifier.failed(project, "Could not read the Git index", errors);
                }
            }

            @Override
            public void onSuccess() {
                int total = plans.values().stream().mapToInt(UntrackPlan::size).sum();
                if (total == 0) {
                    UntrackNotifier.nothingToUntrack(project);
                } else if (confirm(total)) {
                    execute(plans);
                }
            }
        });
    }

    private boolean confirm(int total) {
        GitUntrackSettings settings = GitUntrackSettings.getInstance();
        if (!settings.isConfirmBeforeUntracking()) {
            return true;
        }
        String files = UntrackNotifier.files(total);
        return MessageDialogBuilder.okCancel(
                        "Untrack " + files + "?",
                        "The " + (total == 1 ? "file" : "files") + " will be deleted from the repository on the next commit, "
                                + "for everyone who pulls it. " + (total == 1 ? "It stays" : "They stay") + " on your disk.")
                .yesText("Untrack")
                .noText(Messages.getCancelButton())
                .asWarning()
                .doNotAsk(new DoNotAskOption.Adapter() {
                    @Override
                    public void rememberChoice(boolean isSelected, int exitCode) {
                        if (isSelected && exitCode == Messages.OK) {
                            settings.setConfirmBeforeUntracking(false);
                        }
                    }
                })
                .ask(project);
    }

    private void execute(Map<GitRepository, UntrackPlan> plans) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Untracking files", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                Map<GitRepository, List<String>> untracked = new LinkedHashMap<>();
                Map<GitRepository, List<String>> refused = new LinkedHashMap<>();
                Map<GitRepository, String> errors = new LinkedHashMap<>();
                plans.forEach((repository, plan) -> {
                    if (!plan.refused().isEmpty()) {
                        refused.put(repository, plan.refused());
                    }
                    if (plan.untrack().isEmpty()) {
                        return;
                    }
                    try {
                        new GitIndexCommands(project, repository).untrack(plan.untrack(), false);
                        untracked.put(repository, plan.untrack());
                    } catch (VcsException e) {
                        errors.put(repository, e.getMessage());
                    } finally {
                        markDirty(repository, plan.untrack());
                    }
                });
                if (!untracked.isEmpty()) {
                    UntrackNotifier.untracked(project, untracked, () -> undo(untracked));
                }
                if (!refused.isEmpty()) {
                    UntrackNotifier.refused(project, refused, () -> forceUntrack(refused));
                }
                if (!errors.isEmpty()) {
                    UntrackNotifier.failed(project, "Untrack failed", errors);
                }
            }
        });
    }

    private void forceUntrack(Map<GitRepository, List<String>> refused) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Force untracking files", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                Map<GitRepository, List<String>> untracked = new LinkedHashMap<>();
                Map<GitRepository, String> errors = new LinkedHashMap<>();
                refused.forEach((repository, files) -> {
                    try {
                        new GitIndexCommands(project, repository).untrack(files, true);
                        untracked.put(repository, files);
                    } catch (VcsException e) {
                        errors.put(repository, e.getMessage());
                    } finally {
                        markDirty(repository, files);
                    }
                });
                if (!untracked.isEmpty()) {
                    UntrackNotifier.untracked(project, untracked, () -> undo(untracked));
                }
                if (!errors.isEmpty()) {
                    UntrackNotifier.failed(project, "Force untrack failed", errors);
                }
            }
        });
    }

    private void undo(Map<GitRepository, List<String>> untracked) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Undoing untrack", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                for (Map.Entry<GitRepository, List<String>> entry : untracked.entrySet()) {
                    GitIndexCommands git = new GitIndexCommands(project, entry.getKey());
                    try {
                        if (!UndoCheck.canUndo(entry.getValue(), git.tracked(entry.getValue()), git::existsOnDisk)) {
                            UntrackNotifier.undoUnavailable(project);
                            return;
                        }
                    } catch (VcsException e) {
                        UntrackNotifier.failed(project, "Undo untrack failed", Map.of(entry.getKey(), e.getMessage()));
                        return;
                    }
                }
                Map<GitRepository, String> errors = new LinkedHashMap<>();
                int restored = 0;
                for (Map.Entry<GitRepository, List<String>> entry : untracked.entrySet()) {
                    try {
                        new GitIndexCommands(project, entry.getKey()).track(entry.getValue());
                        restored += entry.getValue().size();
                    } catch (VcsException e) {
                        errors.put(entry.getKey(), e.getMessage());
                    } finally {
                        markDirty(entry.getKey(), entry.getValue());
                    }
                }
                if (restored > 0) {
                    UntrackNotifier.undone(project, restored);
                }
                if (!errors.isEmpty()) {
                    UntrackNotifier.failed(project, "Undo untrack failed", errors);
                }
            }
        });
    }

    private void markDirty(GitRepository repository, List<String> files) {
        String root = repository.getRoot().getPath();
        List<FilePath> paths = new ArrayList<>(files.size());
        for (String file : files) {
            paths.add(VcsUtil.getFilePath(root + "/" + file, false));
        }
        VcsFileUtil.markFilesDirty(project, paths);
    }
}
