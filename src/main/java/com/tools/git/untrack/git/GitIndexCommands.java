package com.tools.git.untrack.git;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsFileUtil;
import com.tools.git.untrack.core.IndexSnapshot;
import com.tools.git.untrack.core.NulSeparated;
import com.tools.git.untrack.core.RepoPaths;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Runs the git commands Untrack needs in one repository. Paths are root-relative and taken literally. */
public final class GitIndexCommands {
    private final Project project;
    private final GitRepository repository;

    public GitIndexCommands(Project project, GitRepository repository) {
        this.project = project;
        this.repository = repository;
    }

    public IndexSnapshot snapshot(Collection<String> selection) throws VcsException {
        List<String> pathspecs = selection.stream().map(RepoPaths::toPathspec).toList();
        Set<String> tracked = query(GitCommand.LS_FILES, List.of("-z", "--cached"), pathspecs);
        Set<String> unstaged = query(GitCommand.DIFF, List.of("-z", "--name-only", "--no-renames", "--no-ext-diff"), pathspecs);
        boolean hasHead = repository.getCurrentRevision() != null;
        Set<String> staged = hasHead
                ? query(GitCommand.DIFF, List.of("-z", "--name-only", "--no-renames", "--no-ext-diff", "--cached"), pathspecs)
                : Set.of();
        return new IndexSnapshot(tracked, staged, unstaged, hasHead, this::existsOnDisk);
    }

    public Set<String> tracked(Collection<String> files) throws VcsException {
        return query(GitCommand.LS_FILES, List.of("-z", "--cached"), List.copyOf(files));
    }

    public void untrack(Collection<String> files, boolean force) throws VcsException {
        List<String> options = force ? List.of("--cached", "--quiet", "-f") : List.of("--cached", "--quiet");
        query(GitCommand.RM, options, List.copyOf(files));
    }

    public void track(Collection<String> files) throws VcsException {
        // -f: the files were tracked before, even if a .gitignore now matches them.
        query(GitCommand.ADD, List.of("-f"), List.copyOf(files));
    }

    public boolean existsOnDisk(String relativePath) {
        Path path = Path.of(repository.getRoot().getPath()).resolve(relativePath);
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
    }

    private Set<String> query(GitCommand command, List<String> options, List<String> paths) throws VcsException {
        Set<String> result = new LinkedHashSet<>();
        for (List<String> chunk : VcsFileUtil.chunkArguments(paths)) {
            GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), command);
            handler.addCustomEnvironmentVariable("GIT_LITERAL_PATHSPECS", "1");
            handler.setSilent(true);
            handler.addParameters(options);
            handler.endOptions();
            handler.addParameters(chunk);
            String output = Git.getInstance().runCommand(handler).getOutputOrThrow();
            result.addAll(NulSeparated.parse(output));
        }
        return result;
    }
}
