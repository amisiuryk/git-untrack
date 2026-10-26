package com.tools.git.untrack.core;

import java.util.Collection;

/**
 * Quick decision whether Untrack is available for a selection, from cached VCS status only
 * (no git calls). Folders count when they are inside a repository and not wholly unversioned or
 * ignored; the exact file list is computed later from the index.
 */
public final class Availability {
    private Availability() {
    }

    public enum Kind { FILE, DIRECTORY, MISSING }

    public enum Status { TRACKED, UNVERSIONED, IGNORED, OUTSIDE_REPOSITORY }

    public record Entry(Kind kind, Status status) {
    }

    public static boolean isAvailable(Collection<Entry> selection, boolean includeMissing) {
        return selection.stream().anyMatch(entry -> qualifies(entry, includeMissing));
    }

    private static boolean qualifies(Entry entry, boolean includeMissing) {
        if (entry.status() != Status.TRACKED) {
            return false;
        }
        return entry.kind() != Kind.MISSING || includeMissing;
    }
}
