package com.tools.git.untrack.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepoPathsTest {

    @Test
    void relativizesInsideRoot() {
        assertEquals("src/A.java", RepoPaths.relativize("/repo", "/repo/src/A.java"));
        assertEquals("", RepoPaths.relativize("/repo", "/repo"));
    }

    @Test
    void doesNotRelativizeSiblingWithSamePrefix() {
        assertNull(RepoPaths.relativize("/repo", "/repository/A.java"));
    }

    @Test
    void folderCoversFilesBeneathIt() {
        assertTrue(RepoPaths.covers("src", "src/a/B.java"));
        assertTrue(RepoPaths.covers("src/B.java", "src/B.java"));
        assertTrue(RepoPaths.covers("", "anything/at/all"));
        assertFalse(RepoPaths.covers("src", "srcgen/B.java"));
    }

    @Test
    void groupsSelectionByRepositoryAndDropsPathsOutsideRepositories() {
        Map<String, List<String>> grouped = RepoPaths.groupByRoot(
                List.of("/a/x.txt", "/b/dir", "/a", "/elsewhere/y.txt", "/a/x.txt"),
                path -> path.startsWith("/a") ? "/a" : path.startsWith("/b") ? "/b" : null);

        assertEquals(Map.of("/a", List.of("x.txt", ""), "/b", List.of("dir")), grouped);
    }

    @Test
    void rootPathspecIsDot() {
        assertEquals(".", RepoPaths.toPathspec(""));
        assertEquals("src", RepoPaths.toPathspec("src"));
    }

    @Test
    void parsesNulSeparatedOutput() {
        assertEquals(Set.of("a b.txt", "dir/c\nd"), NulSeparated.parse("a b.txt\0dir/c\nd\0"));
        assertEquals(Set.of(), NulSeparated.parse(""));
    }
}
