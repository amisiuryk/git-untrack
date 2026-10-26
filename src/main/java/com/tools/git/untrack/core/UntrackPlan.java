package com.tools.git.untrack.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * What Untrack will do in one repository. All paths are root-relative and sorted.
 *
 * @param untrack files git will untrack without force
 * @param refused tracked files git refuses to untrack without force (partial staging)
 */
public record UntrackPlan(List<String> untrack, List<String> refused) {

    public static UntrackPlan of(Collection<String> selection, IndexSnapshot index, boolean includeMissing) {
        List<String> untrack = new ArrayList<>();
        List<String> refused = new ArrayList<>();
        for (String file : new TreeSet<>(index.tracked())) {
            if (selection.stream().noneMatch(selected -> RepoPaths.covers(selected, file))) {
                continue;
            }
            boolean onDisk = index.existsOnDisk().test(file);
            if (!onDisk && !includeMissing) {
                continue;
            }
            // Mirrors `git rm --cached`: a file present on disk whose staged content differs
            // from both HEAD and the working tree is refused unless forced.
            if (onDisk && index.isStaged(file) && index.unstaged().contains(file)) {
                refused.add(file);
            } else {
                untrack.add(file);
            }
        }
        return new UntrackPlan(List.copyOf(untrack), List.copyOf(refused));
    }

    public int size() {
        return untrack.size() + refused.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }
}
