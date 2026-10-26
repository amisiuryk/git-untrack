package com.tools.git.untrack.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Path arithmetic on system-independent paths ({@code /}-separated, no trailing slash).
 * A relative path of {@code ""} stands for the repository root itself.
 */
public final class RepoPaths {
    private RepoPaths() {
    }

    /** Returns {@code path} relative to {@code root}, or {@code null} if it is not inside {@code root}. */
    public static String relativize(String root, String path) {
        if (path.equals(root)) {
            return "";
        }
        String prefix = root.endsWith("/") ? root : root + "/";
        return path.startsWith(prefix) ? path.substring(prefix.length()) : null;
    }

    /** Whether the selected relative path {@code selected} is {@code file} itself or a folder containing it. */
    public static boolean covers(String selected, String file) {
        return selected.isEmpty() || file.equals(selected) || file.startsWith(selected + "/");
    }

    /**
     * Groups absolute selected paths by the repository root owning them, as root-relative paths.
     * Paths outside any repository ({@code rootOf} returns {@code null}) are dropped.
     */
    public static Map<String, List<String>> groupByRoot(Collection<String> selected, Function<String, String> rootOf) {
        Map<String, Set<String>> grouped = new LinkedHashMap<>();
        for (String path : selected) {
            String root = rootOf.apply(path);
            if (root == null) {
                continue;
            }
            String relative = relativize(root, path);
            if (relative != null) {
                grouped.computeIfAbsent(root, r -> new LinkedHashSet<>()).add(relative);
            }
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        grouped.forEach((root, paths) -> result.put(root, List.copyOf(paths)));
        return result;
    }

    /** Converts a root-relative path to a git pathspec ({@code "."} for the root). */
    public static String toPathspec(String relative) {
        return relative.isEmpty() ? "." : relative;
    }
}
