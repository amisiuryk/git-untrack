package com.tools.git.untrack.core;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

/** Undo untrack is possible only while every untracked file is still unversioned: on disk and not in the index. */
public final class UndoCheck {
    private UndoCheck() {
    }

    public static boolean canUndo(Collection<String> untracked, Set<String> trackedNow, Predicate<String> existsOnDisk) {
        return untracked.stream().allMatch(file -> existsOnDisk.test(file) && !trackedNow.contains(file));
    }
}
