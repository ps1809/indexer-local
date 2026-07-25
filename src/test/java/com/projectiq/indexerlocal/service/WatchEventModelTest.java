package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.event.EventType;
import com.projectiq.indexerlocal.model.event.WatchEvent;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WatchEvent model.
 */
class WatchEventModelTest {

    @Test
    void testWatchEventCreation() {
        String repoId = "repo1";
        Path path = Paths.get("/test/file.java");
        String relativePath = "src/file.java";
        EventType type = EventType.CREATED;
        Instant timestamp = Instant.now();
        String dirPath = "/test";

        WatchEvent event = new WatchEvent(repoId, path, relativePath, type, timestamp, dirPath);

        assertEquals(repoId, event.getRepositoryId());
        assertEquals(path, event.getAbsoluteFilePath());
        assertEquals(relativePath, event.getRelativePath());
        assertEquals(type, event.getEventType());
        assertEquals(timestamp, event.getTimestamp());
        assertEquals(dirPath, event.getDirectoryPath());
    }

    @Test
    void testWatchEventIsCreated() {
        WatchEvent event = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.CREATED, Instant.now(), "/test");
        assertTrue(event.isCreated());
        assertFalse(event.isModified());
        assertFalse(event.isDeleted());
        assertFalse(event.isRenamed());
    }

    @Test
    void testWatchEventIsModified() {
        WatchEvent event = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.MODIFIED, Instant.now(), "/test");
        assertFalse(event.isCreated());
        assertTrue(event.isModified());
        assertFalse(event.isDeleted());
        assertFalse(event.isRenamed());
    }

    @Test
    void testWatchEventIsDeleted() {
        WatchEvent event = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.DELETED, Instant.now(), "/test");
        assertFalse(event.isCreated());
        assertFalse(event.isModified());
        assertTrue(event.isDeleted());
        assertFalse(event.isRenamed());
    }

    @Test
    void testWatchEventIsRenamed() {
        WatchEvent event = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.RENAMED, Instant.now(), "/test");
        assertFalse(event.isCreated());
        assertFalse(event.isModified());
        assertFalse(event.isDeleted());
        assertTrue(event.isRenamed());
    }

    @Test
    void testWatchEventNullTimestampUsesCurrentTime() {
        Instant before = Instant.now();
        WatchEvent event = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.CREATED, null, "/test");
        Instant after = Instant.now();

        assertNotNull(event.getTimestamp());
        assertTrue(!event.getTimestamp().isBefore(before));
        assertTrue(!event.getTimestamp().isAfter(after));
    }

    @Test
    void testWatchEventEquals() {
        WatchEvent event1 = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.CREATED, Instant.now(), "/test");
        WatchEvent event2 = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "different.java",
                EventType.CREATED, Instant.now(), "/test");

        assertEquals(event1, event2); // Equals based on repoId, path, eventType
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testWatchEventNotEqualsDifferentRepo() {
        WatchEvent event1 = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.CREATED, Instant.now(), "/test");
        WatchEvent event2 = new WatchEvent(
                "repo2", Paths.get("/test/file.java"), "file.java",
                EventType.CREATED, Instant.now(), "/test");

        assertNotEquals(event1, event2);
    }

    @Test
    void testWatchEventNotEqualsDifferentType() {
        WatchEvent event1 = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.CREATED, Instant.now(), "/test");
        WatchEvent event2 = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.MODIFIED, Instant.now(), "/test");

        assertNotEquals(event1, event2);
    }

    @Test
    void testWatchEventToString() {
        WatchEvent event = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.CREATED, Instant.now(), "/test");

        String str = event.toString();
        assertTrue(str.contains("repo1"));
        assertTrue(str.contains("file.java"));
        assertTrue(str.contains("CREATED"));
    }

    @Test
    void testWatchEventNullRepositoryIdThrows() {
        assertThrows(NullPointerException.class, () ->
            new WatchEvent(null, Paths.get("/test/file.java"), "file.java",
                    EventType.CREATED, Instant.now(), "/test")
        );
    }

    @Test
    void testWatchEventNullEventTypeThrows() {
        assertThrows(NullPointerException.class, () ->
            new WatchEvent("repo1", Paths.get("/test/file.java"), "file.java",
                    null, Instant.now(), "/test")
        );
    }

    @Test
    void testWatchEventEqualsSelf() {
        WatchEvent event = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.CREATED, Instant.now(), "/test");

        assertEquals(event, event);
        assertEquals(event.hashCode(), event.hashCode());
    }

    @Test
    void testWatchEventNotEqualsNull() {
        WatchEvent event = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.CREATED, Instant.now(), "/test");

        assertNotEquals(event, null);
    }

    @Test
    void testWatchEventNotEqualsDifferentClass() {
        WatchEvent event = new WatchEvent(
                "repo1", Paths.get("/test/file.java"), "file.java",
                EventType.CREATED, Instant.now(), "/test");

        assertNotEquals(event, "string");
    }
}