package com.tools.git.untrack.core;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Parses {@code -z} output of git commands such as {@code ls-files} and {@code diff --name-only}. */
public final class NulSeparated {
    private NulSeparated() {
    }

    public static Set<String> parse(String output) {
        Set<String> result = new LinkedHashSet<>();
        Arrays.stream(output.split("\0"))
                .filter(path -> !path.isEmpty())
                .forEach(result::add);
        return result;
    }
}
