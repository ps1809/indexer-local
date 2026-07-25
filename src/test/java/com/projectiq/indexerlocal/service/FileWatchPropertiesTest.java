package com.projectiq.indexerlocal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileWatchProperties filtering functionality.
 */
class FileWatchPropertiesTest {

    private FileWatchProperties properties;

    @BeforeEach
    void setUp() {
        List<String> ignoredDirs = Arrays.asList(
                ".git", ".idea", ".vscode", "target", "build", "node_modules"
        );
        List<String> ignoredFiles = Arrays.asList("*.tmp", "*.swp", "*~");
        properties = new FileWatchProperties(
                ignoredDirs,
                ignoredFiles,
                1000,
                500,
                true
        );
    }

    @Test
    void testIsIgnoredDirectory_git() {
        assertTrue(properties.isIgnoredDirectory(".git"));
    }

    @Test
    void testIsIgnoredDirectory_idea() {
        assertTrue(properties.isIgnoredDirectory(".idea"));
    }

    @Test
    void testIsIgnoredDirectory_vscode() {
        assertTrue(properties.isIgnoredDirectory(".vscode"));
    }

    @Test
    void testIsIgnoredDirectory_target() {
        assertTrue(properties.isIgnoredDirectory("target"));
    }

    @Test
    void testIsIgnoredDirectory_build() {
        assertTrue(properties.isIgnoredDirectory("build"));
    }

    @Test
    void testIsIgnoredDirectory_nodeModules() {
        assertTrue(properties.isIgnoredDirectory("node_modules"));
    }

    @Test
    void testIsNotIgnoredDirectory_src() {
        assertFalse(properties.isIgnoredDirectory("src"));
    }

    @Test
    void testIsNotIgnoredDirectory_main() {
        assertFalse(properties.isIgnoredDirectory("main"));
    }

    @Test
    void testIsNotIgnoredDirectory_java() {
        assertFalse(properties.isIgnoredDirectory("java"));
    }

    @Test
    void testShouldIgnorePath_gitSubdirectory() {
        Path path = Paths.get("project/.git/config");
        assertTrue(properties.shouldIgnorePath(path));
    }

    @Test
    void testShouldIgnorePath_ideaSubdirectory() {
        Path path = Paths.get("project/.idea/workspace.xml");
        assertTrue(properties.shouldIgnorePath(path));
    }

    @Test
    void testShouldIgnorePath_tmpFile() {
        Path path = Paths.get("project/file.tmp");
        assertTrue(properties.shouldIgnorePath(path));
    }

    @Test
    void testShouldIgnorePath_swpFile() {
        Path path = Paths.get("project/file.swp");
        assertTrue(properties.shouldIgnorePath(path));
    }

    @Test
    void testShouldNotIgnorePath_javaFile() {
        Path path = Paths.get("project/src/Main.java");
        assertFalse(properties.shouldIgnorePath(path));
    }

    @Test
    void testShouldNotIgnorePath_xmlResource() {
        Path path = Paths.get("project/src/main/resources/application.xml");
        assertFalse(properties.shouldIgnorePath(path));
    }

    @Test
    void testShouldNotIgnorePath_pythonFile() {
        Path path = Paths.get("project/src/main.py");
        assertFalse(properties.shouldIgnorePath(path));
    }

    @Test
    void testShouldIgnoreNullPath() {
        assertTrue(properties.shouldIgnorePath(null));
    }

    @Test
    void testIsNullDirectory() {
        assertTrue(properties.isInIgnoredDirectory(null));
    }

    @Test
    void testIsNullDirectory_nestedIgnored() {
        Path path = Paths.get("project/target/classes");
        assertTrue(properties.isInIgnoredDirectory(path));
    }

    @Test
    void testIsNotNullDirectory_srcMain() {
        Path path = Paths.get("project/src/main/java");
        assertFalse(properties.isInIgnoredDirectory(path));
    }

    @Test
    void testEnabledFlag() {
        assertTrue(properties.isEnabled());
    }

    @Test
    void testDebounceDelay() {
        assertEquals(500, properties.getDebounceDelay());
    }

    @Test
    void testMaxQueueSize() {
        assertEquals(1000, properties.getMaxQueueSize());
    }

    @Test
    void testDefaultIgnoredDirectories() {
        FileWatchProperties defaultProps = new FileWatchProperties(
                null,
                null,
                1000,
                500,
                true
        );
        assertTrue(defaultProps.isIgnoredDirectory(".git"));
        assertTrue(defaultProps.isIgnoredDirectory("target"));
        assertFalse(defaultProps.isIgnoredDirectory("src"));
    }

    @Test
    void testGlobPatternMatching() {
        // Test various glob patterns
        assertTrue(properties.shouldIgnorePath(Paths.get("project/file.tmp")));
        assertTrue(properties.shouldIgnorePath(Paths.get("project/file.swp")));
        assertFalse(properties.shouldIgnorePath(Paths.get("project/file.txt")));
        assertFalse(properties.shouldIgnorePath(Paths.get("project/file.java")));
    }

    @Test
    void testMultipleIgnoredDirectoriesInPath() {
        Path path = Paths.get("project/.git/objects/pack");
        assertTrue(properties.isInIgnoredDirectory(path));
    }

    @Test
    void testNestedStructure() {
        Path path = Paths.get("src/main/java/com/example/controller");
        assertFalse(properties.shouldIgnorePath(path));
    }

    @Test
    void testComplexPathFiltering() {
        // Should ignore git tracked files
        assertTrue(properties.shouldIgnorePath(Paths.get(".git/HEAD")));
        assertTrue(properties.shouldIgnorePath(Paths.get("repo/.git/config")));
        
        // Should not ignore regular source files
        assertFalse(properties.shouldIgnorePath(Paths.get("src/App.java")));
        assertFalse(properties.shouldIgnorePath(Paths.get("main/resources/application.yml")));
    }
}