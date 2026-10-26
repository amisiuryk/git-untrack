package com.tools.git.untrack.core;

import com.tools.git.untrack.core.Availability.Entry;
import com.tools.git.untrack.core.Availability.Kind;
import com.tools.git.untrack.core.Availability.Status;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailabilityAndUndoTest {

    @Test
    void availableWhenAnyTrackedFileOrFolderIsSelected() {
        assertTrue(Availability.isAvailable(List.of(
                new Entry(Kind.FILE, Status.UNVERSIONED),
                new Entry(Kind.FILE, Status.TRACKED)), false));
        assertTrue(Availability.isAvailable(List.of(new Entry(Kind.DIRECTORY, Status.TRACKED)), false));
    }

    @Test
    void unavailableWhenNothingQualifies() {
        assertFalse(Availability.isAvailable(List.of(
                new Entry(Kind.FILE, Status.UNVERSIONED),
                new Entry(Kind.FILE, Status.IGNORED),
                new Entry(Kind.DIRECTORY, Status.UNVERSIONED),
                new Entry(Kind.FILE, Status.OUTSIDE_REPOSITORY)), false));
        assertFalse(Availability.isAvailable(List.of(), false));
    }

    @Test
    void missingFilesQualifyOnlyWhenEnabled() {
        List<Entry> missing = List.of(new Entry(Kind.MISSING, Status.TRACKED));

        assertFalse(Availability.isAvailable(missing, false));
        assertTrue(Availability.isAvailable(missing, true));
    }

    @Test
    void undoRequiresEveryFileStillUnversioned() {
        List<String> untracked = List.of("a.txt", "b.txt");

        assertTrue(UndoCheck.canUndo(untracked, Set.of(), path -> true));
        assertFalse(UndoCheck.canUndo(untracked, Set.of("b.txt"), path -> true));
        assertFalse(UndoCheck.canUndo(untracked, Set.of(), path -> !path.equals("a.txt")));
    }
}
