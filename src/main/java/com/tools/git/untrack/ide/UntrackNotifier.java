package com.tools.git.untrack.ide;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import git4idea.repo.GitRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class UntrackNotifier {
    private static final String GROUP = "Git Untrack";
    private static final int LISTED_FILES = 10;

    private UntrackNotifier() {
    }

    static String files(int count) {
        return count == 1 ? "1 file" : count + " files";
    }

    static void untracked(Project project, Map<GitRepository, List<String>> untracked, Runnable undo) {
        int count = count(untracked);
        create("Untracked " + files(count),
                (count == 1 ? "It will be" : "They will be") + " deleted from the repository on the next commit and kept on disk.",
                NotificationType.INFORMATION)
                .addAction(NotificationAction.createSimpleExpiring("Undo", undo))
                .notify(project);
    }

    static void refused(Project project, Map<GitRepository, List<String>> refused, Runnable forceUntrack) {
        int count = count(refused);
        String content = listFiles(refused)
                + "<br>" + (count == 1 ? "Its" : "Their") + " staged content differs from both HEAD and the file on disk. "
                + "Force untrack discards the staged changes; files on disk are not touched.";
        create(files(count) + " not untracked", content, NotificationType.ERROR)
                .addAction(NotificationAction.createSimpleExpiring("Force untrack", forceUntrack))
                .notify(project);
    }

    static void nothingToUntrack(Project project) {
        create("Nothing to untrack", "The selection contains no tracked files.", NotificationType.INFORMATION).notify(project);
    }

    static void undone(Project project, int count) {
        create("Tracking restored", files(count) + " added back to the index.", NotificationType.INFORMATION).notify(project);
    }

    static void undoUnavailable(Project project) {
        create("Cannot undo untrack",
                "Some of the files were deleted from disk or tracked again since they were untracked.",
                NotificationType.ERROR).notify(project);
    }

    static void failed(Project project, String title, Map<GitRepository, String> errors) {
        String content = errors.entrySet().stream()
                .map(entry -> (errors.size() > 1 ? "<b>" + escape(entry.getKey().getRoot().getName()) + "</b>: " : "")
                        + escape(entry.getValue()))
                .collect(Collectors.joining("<br>"));
        create(title, content, NotificationType.ERROR).notify(project);
    }

    private static Notification create(String title, String content, NotificationType type) {
        return NotificationGroupManager.getInstance().getNotificationGroup(GROUP).createNotification(title, content, type);
    }

    private static int count(Map<GitRepository, List<String>> files) {
        return files.values().stream().mapToInt(List::size).sum();
    }

    private static String listFiles(Map<GitRepository, List<String>> files) {
        boolean multiRepo = files.size() > 1;
        List<String> all = files.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(file -> multiRepo ? entry.getKey().getRoot().getName() + "/" + file : file))
                .toList();
        String listed = all.stream().limit(LISTED_FILES).map(UntrackNotifier::escape).collect(Collectors.joining("<br>"));
        return all.size() > LISTED_FILES ? listed + "<br>and " + (all.size() - LISTED_FILES) + " more" : listed;
    }

    private static String escape(String text) {
        return StringUtil.escapeXmlEntities(text);
    }
}
