package com.tools.git.untrack.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UntrackPlanTest {

    private static IndexSnapshot index(Set<String> tracked, Set<String> staged, Set<String> unstaged, Set<String> missing) {
        return new IndexSnapshot(tracked, staged, unstaged, true, path -> !missing.contains(path));
    }

    @Test
    void untracksSelectedTrackedFiles() {
        UntrackPlan plan = UntrackPlan.of(List.of("a.txt"), index(Set.of("a.txt", "b.txt"), Set.of(), Set.of(), Set.of()), false);

        assertEquals(List.of("a.txt"), plan.untrack());
        assertEquals(List.of(), plan.refused());
    }

    @Test
    void expandsFoldersRecursively() {
        IndexSnapshot index = index(Set.of("src/b/C.java", "src/A.java", "srcgen/X.java", "README"), Set.of(), Set.of(), Set.of());

        UntrackPlan plan = UntrackPlan.of(List.of("src"), index, false);

        assertEquals(List.of("src/A.java", "src/b/C.java"), plan.untrack());
    }

    @Test
    void selectingTheRootCoversEverything() {
        UntrackPlan plan = UntrackPlan.of(List.of(""), index(Set.of("b", "a/c"), Set.of(), Set.of(), Set.of()), false);

        assertEquals(List.of("a/c", "b"), plan.untrack());
    }

    @Test
    void skipsUntrackedFilesSilently() {
        UntrackPlan plan = UntrackPlan.of(List.of("new.txt", "ignored.log"), index(Set.of("a.txt"), Set.of(), Set.of(), Set.of()), false);

        assertTrue(plan.isEmpty());
    }

    @Test
    void overlappingSelectionCountsEachFileOnce() {
        UntrackPlan plan = UntrackPlan.of(List.of("src", "src/A.java"), index(Set.of("src/A.java"), Set.of(), Set.of(), Set.of()), false);

        assertEquals(List.of("src/A.java"), plan.untrack());
        assertEquals(1, plan.size());
    }

    @Test
    void skipsMissingFilesByDefault() {
        IndexSnapshot index = index(Set.of("gone.txt", "here.txt"), Set.of(), Set.of("gone.txt"), Set.of("gone.txt"));

        UntrackPlan plan = UntrackPlan.of(List.of(""), index, false);

        assertEquals(List.of("here.txt"), plan.untrack());
    }

    @Test
    void includesMissingFilesWhenEnabledAndNeverRefusesThem() {
        IndexSnapshot index = index(Set.of("gone.txt"), Set.of("gone.txt"), Set.of("gone.txt"), Set.of("gone.txt"));

        UntrackPlan plan = UntrackPlan.of(List.of(""), index, true);

        assertEquals(List.of("gone.txt"), plan.untrack());
        assertEquals(List.of(), plan.refused());
    }

    @Test
    void refusesFilesStagedDifferentlyFromBothHeadAndDisk() {
        IndexSnapshot index = index(
                Set.of("partial.txt", "staged.txt", "modified.txt"),
                Set.of("partial.txt", "staged.txt"),
                Set.of("partial.txt", "modified.txt"),
                Set.of());

        UntrackPlan plan = UntrackPlan.of(List.of(""), index, false);

        assertEquals(List.of("modified.txt", "staged.txt"), plan.untrack());
        assertEquals(List.of("partial.txt"), plan.refused());
        assertEquals(3, plan.size());
    }

    @Test
    void withoutHeadEveryEntryCountsAsStaged() {
        IndexSnapshot index = new IndexSnapshot(Set.of("new.txt", "edited.txt"), Set.of(), Set.of("edited.txt"), false, path -> true);

        UntrackPlan plan = UntrackPlan.of(List.of(""), index, false);

        assertEquals(List.of("new.txt"), plan.untrack());
        assertEquals(List.of("edited.txt"), plan.refused());
    }
}
