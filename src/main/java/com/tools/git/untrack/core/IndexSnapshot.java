package com.tools.git.untrack.core;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Git state of one repository, limited to the selection. All paths are root-relative.
 *
 * @param tracked        paths present in the index ({@code git ls-files})
 * @param staged         paths whose index entry differs from HEAD ({@code git diff --cached}); ignored when there is no HEAD
 * @param unstaged       paths whose working tree differs from the index ({@code git diff})
 * @param hasHead        whether the repository has a HEAD commit
 * @param existsOnDisk   whether a path exists in the working tree
 */
public record IndexSnapshot(
        Set<String> tracked,
        Set<String> staged,
        Set<String> unstaged,
        boolean hasHead,
        Predicate<String> existsOnDisk
) {
    /** Whether git would add this file to the index differently from HEAD. Without HEAD every entry is staged. */
    boolean isStaged(String path) {
        return !hasHead || staged.contains(path);
    }
}
