package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.event.EventType;
import com.projectiq.indexerlocal.model.event.WatchEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IncrementalIndexerEventProcessor.
 */
@ExtendWith(MockitoExtension.class)
class IncrementalIndexerEventProcessorTest {

    @Mock
    private IncrementalIndexingService incrementalIndexingService;

    private IncrementalIndexerEventProcessor processor;

    private static final String REPO_ID = "test-repo";
    private static final Path TEST_PATH = Paths.get("src/test/java/TestFile.java");
    private static final Path DOC_PATH = Paths.get("src/test/docs/readme.md");

    @BeforeEach
    void setUp() {
        processor = new IncrementalIndexerEventProcessor(incrementalIndexingService);
    }

    @Test
    void testProcessEventNull() {
        assertFalse(processor.processEvent(null));
    }

    @Test
    void testProcessEventEmptyFields() {
        WatchEvent event = new WatchEvent("", Paths.get("/test"), "test.java",
                EventType.CREATED, Instant.now(), "/test");
        assertFalse(processor.processEvent(event));
    }

    @Test
    void testProcessEventNullPath() {
        WatchEvent event = new WatchEvent(REPO_ID, null, null, EventType.CREATED, Instant.now(), null);
        assertFalse(processor.processEvent(event));
    }

    @Test
    void testProcessEventNonExistentFileCreated() {
        // For a created event on a non-existent file, should return false
        Path nonExistent = Paths.get("nonexistent/File.java");
        WatchEvent event = new WatchEvent(REPO_ID, nonExistent, "File.java",
                EventType.CREATED, Instant.now(), "nonexistent");
        assertFalse(processor.processEvent(event));
        verifyNoInteractions(incrementalIndexingService);
    }

    @Test
    void testProcessEventNonExistentFileModified() {
        Path nonExistent = Paths.get("nonexistent/File.java");
        WatchEvent event = new WatchEvent(REPO_ID, nonExistent, "File.java",
                EventType.MODIFIED, Instant.now(), "nonexistent");
        assertFalse(processor.processEvent(event));
        verifyNoInteractions(incrementalIndexingService);
    }

    @Test
    void testProcessEventCreated() {
        // Create a real temp file for created event test
        Path tempFile = createTempJavaFile();
        WatchEvent event = new WatchEvent(REPO_ID, tempFile, tempFile.getFileName().toString(),
                EventType.CREATED, Instant.now(), tempFile.getParent().toString());

        doNothing().when(incrementalIndexingService).indexSingleFile(anyString(), anyString());
        boolean result = processor.processEvent(event);
        assertTrue(result);

        verify(incrementalIndexingService, times(1)).indexSingleFile(eq(REPO_ID), eq(tempFile.toAbsolutePath().normalize().toString()));
        tempFile.toFile().delete();
    }

    @Test
    void testProcessEventModified() {
        Path tempFile = createTempJavaFile();
        WatchEvent event = new WatchEvent(REPO_ID, tempFile, tempFile.getFileName().toString(),
                EventType.MODIFIED, Instant.now(), tempFile.getParent().toString());

        doNothing().when(incrementalIndexingService).removeFileFromIndex(anyString(), anyString());
        doNothing().when(incrementalIndexingService).indexSingleFile(anyString(), anyString());

        boolean result = processor.processEvent(event);
        assertTrue(result);

        verify(incrementalIndexingService, times(1)).removeFileFromIndex(eq(REPO_ID), eq(tempFile.toAbsolutePath().normalize().toString()));
        verify(incrementalIndexingService, times(1)).indexSingleFile(eq(REPO_ID), eq(tempFile.toAbsolutePath().normalize().toString()));
        tempFile.toFile().delete();
    }

    @Test
    void testProcessEventDeleted() {
        Path nonExistent = Paths.get("deleted/File.java");
        WatchEvent event = new WatchEvent(REPO_ID, nonExistent, "File.java",
                EventType.DELETED, Instant.now(), "deleted");

        doNothing().when(incrementalIndexingService).removeFileFromIndex(anyString(), anyString());

        boolean result = processor.processEvent(event);
        assertTrue(result);

        verify(incrementalIndexingService, times(1)).removeFileFromIndex(eq(REPO_ID), eq(nonExistent.toAbsolutePath().normalize().toString()));
    }

    @Test
    void testProcessEventUnhandledType() {
        // RENAMED with existing file -> should handle
        Path tempFile = createTempJavaFile();
        WatchEvent event = new WatchEvent(REPO_ID, tempFile, tempFile.getFileName().toString(),
                EventType.RENAMED, Instant.now(), tempFile.getParent().toString());

        doNothing().when(incrementalIndexingService).indexSingleFile(anyString(), anyString());

        boolean result = processor.processEvent(event);
        assertTrue(result);
        verify(incrementalIndexingService, times(1)).indexSingleFile(eq(REPO_ID), eq(tempFile.toAbsolutePath().normalize().toString()));
        tempFile.toFile().delete();
    }

    @Test
    void testProcessFileCreatedWithException() {
        Path tempFile = createTempJavaFile();
        WatchEvent event = new WatchEvent(REPO_ID, tempFile, tempFile.getFileName().toString(),
                EventType.CREATED, Instant.now(), tempFile.getParent().toString());

        doThrow(new RuntimeException("Test error")).when(incrementalIndexingService).indexSingleFile(anyString(), anyString());
        boolean result = processor.processEvent(event);
        assertFalse(result);
        tempFile.toFile().delete();
    }

    @Test
    void testDetermineFileTypeJava() {
        assertEquals("JAVA", processor.determineFileType("MyClass.java"));
        assertEquals("JAVA", processor.determineFileType("/path/to/MyClass.java"));
    }

    @Test
    void testDetermineFileTypeBuild() {
        assertEquals("BUILD", processor.determineFileType("pom.xml"));
        assertEquals("BUILD", processor.determineFileType("build.gradle"));
    }

    @Test
    void testDetermineFileTypeConfig() {
        // .properties and .xml files match BUILD first, so use other config extensions
        assertEquals("CONFIG", processor.determineFileType("config.json"));
        assertEquals("CONFIG", processor.determineFileType("application.conf"));
        assertEquals("CONFIG", processor.determineFileType("config.ini"));
        assertEquals("CONFIG", processor.determineFileType("custom.cfg"));
    }

    @Test
    void testDetermineFileTypeDocumentation() {
        assertEquals("DOCUMENTATION", processor.determineFileType("README.md"));
        assertEquals("DOCUMENTATION", processor.determineFileType("index.html"));
        assertEquals("DOCUMENTATION", processor.determineFileType("docs/index.adoc"));
    }

    @Test
    void testDetermineFileTypeOther() {
        assertEquals("OTHER", processor.determineFileType("image.png"));
        assertEquals("OTHER", processor.determineFileType("data.csv"));
        assertEquals("OTHER", processor.determineFileType("script.sh"));
    }

    @Test
    void testDetermineFileTypeNull() {
        assertEquals("UNKNOWN", processor.determineFileType(null));
    }

    /**
     * Create a temporary Java file for testing.
     */
    private Path createTempJavaFile() {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            Path tempFile = tempDir.resolve("TestIncremental_" + System.nanoTime() + ".java");
            java.nio.file.Files.writeString(tempFile, "public class TestIncremental { }");
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temp file", e);
        }
    }
}