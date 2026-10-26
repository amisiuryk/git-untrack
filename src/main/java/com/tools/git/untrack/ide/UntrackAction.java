package com.tools.git.untrack.ide;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import com.tools.git.untrack.core.Availability;
import com.tools.git.untrack.core.Availability.Entry;
import com.tools.git.untrack.core.Availability.Kind;
import com.tools.git.untrack.core.Availability.Status;
import com.tools.git.untrack.settings.GitUntrackSettings;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class UntrackAction extends DumbAwareAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || GitRepositoryManager.getInstance(project).getRepositories().isEmpty()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        List<Entry> entries = selection(e).stream().map(path -> entry(project, path)).toList();
        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled(Availability.isAvailable(entries, GitUntrackSettings.getInstance().isIncludeMissingFiles()));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Set<FilePath> selection = selection(e);
        if (project != null && !selection.isEmpty()) {
            new Untracker(project).untrack(selection);
        }
    }

    private static Set<FilePath> selection(AnActionEvent e) {
        Set<FilePath> paths = new LinkedHashSet<>();
        Iterable<VirtualFile> files = e.getData(VcsDataKeys.VIRTUAL_FILES);
        if (files != null) {
            files.forEach(file -> paths.add(VcsUtil.getFilePath(file)));
        } else {
            VirtualFile[] array = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
            if (array != null) {
                for (VirtualFile file : array) {
                    paths.add(VcsUtil.getFilePath(file));
                }
            }
        }
        // Changes reach files missing from disk, which have no VirtualFile.
        Change[] changes = e.getData(VcsDataKeys.CHANGES);
        if (changes != null) {
            for (Change change : changes) {
                paths.add(ChangesUtil.getFilePath(change));
            }
        }
        return paths;
    }

    private static Entry entry(Project project, FilePath path) {
        if (GitRepositoryManager.getInstance(project).getRepositoryForFileQuick(path) == null) {
            return new Entry(Kind.FILE, Status.OUTSIDE_REPOSITORY);
        }
        ChangeListManager changes = ChangeListManager.getInstance(project);
        VirtualFile file = path.getVirtualFile();
        if (file == null || !file.exists()) {
            return new Entry(Kind.MISSING, status(changes.getStatus(path)));
        }
        Kind kind = file.isDirectory() ? Kind.DIRECTORY : Kind.FILE;
        if (changes.isIgnoredFile(file)) {
            return new Entry(kind, Status.IGNORED);
        }
        if (changes.isUnversioned(file)) {
            return new Entry(kind, Status.UNVERSIONED);
        }
        return new Entry(kind, kind == Kind.FILE ? status(changes.getStatus(file)) : Status.TRACKED);
    }

    private static Status status(FileStatus status) {
        if (status == FileStatus.UNKNOWN) {
            return Status.UNVERSIONED;
        }
        if (status == FileStatus.IGNORED) {
            return Status.IGNORED;
        }
        return Status.TRACKED;
    }
}
