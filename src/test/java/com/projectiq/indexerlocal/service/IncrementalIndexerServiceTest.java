package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.event.EventType;
import com.projectiq.indexerlocal.model.event.WatchEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IncrementalIndexerService.
 */
@ExtendWith(MockitoExtension.class)
class IncrementalIndexerServiceTest {

    @Mock
    private FileWatchService fileWatchService;

    @Mock
    private IncrementalIndexerEventProcessor eventProcessor;

    private IncrementalIndexerService incrementalIndexerService;

    @BeforeEach
    void setUp() {
        incrementalIndexerService = new IncrementalIndexerService(fileWatchService, eventProcessor);
    }

    @Test
    void testInitialState() {
        assertFalse(incrementalIndexerService.isRunning());
        assertEquals(0, incrementalIndexerService.getTotalEventsProcessed());
        assertEquals(0, incrementalIndexerService.getTotalEventsFailed());
        assertEquals(0, incrementalIndexerService.getTotalEventsSkipped());
    }

    @Test
    void testStartAndStop() {
        incrementalIndexerService.start();
        assertTrue(incrementalIndexerService.isRunning());

        incrementalIndexerService.stop();
        assertFalse(incrementalIndexerService.isRunning());
    }

    @Test
    void testProcessEventsSuccessfully() throws InterruptedException {
        WatchEvent event1 = new WatchEvent("repo1", Paths.get("/test/File1.java"), "File1.java",
                EventType.CREATED, Instant.now(), "/test");
        WatchEvent event2 = new WatchEvent("repo1", Paths.get("/test/File2.java"), "File2.java",
                EventType.MODIFIED, Instant.now(), "/test");

        // First two calls return events, third returns null (queue empty)
        when(fileWatchService.captureEvent(anyLong(), any(TimeUnit.class)))
                .thenReturn(event1)
                .thenReturn(event2)
                .thenReturn(null);

        when(eventProcessor.processEvent(event1)).thenReturn(true);
        when(eventProcessor.processEvent(event2)).thenReturn(true);

        incrementalIndexerService.start();
        Thread.sleep(200); // Allow polling thread to process
        incrementalIndexerService.stop();

        verify(eventProcessor, times(1)).processEvent(event1);
        verify(eventProcessor, times(1)).processEvent(event2);
    }

    @Test
    void testProcessEventsWithFailure() throws InterruptedException {
        WatchEvent event1 = new WatchEvent("repo1", Paths.get("/test/File1.java"), "File1.java",
                EventType.CREATED, Instant.now(), "/test");
        WatchEvent event2 = new WatchEvent("repo1", Paths.get("/test/File2.java"), "File2.java",
                EventType.CREATED, Instant.now(), "/test");

        when(fileWatchService.captureEvent(anyLong(), any(TimeUnit.class)))
                .thenReturn(event1)
                .thenReturn(event2)
                .thenReturn(null);

        when(eventProcessor.processEvent(event1)).thenReturn(true);
        when(eventProcessor.processEvent(event2)).thenReturn(false);

        incrementalIndexerService.start();
        Thread.sleep(200);
        incrementalIndexerService.stop();

        verify(eventProcessor, times(1)).processEvent(event1);
        verify(eventProcessor, times(1)).processEvent(event2);
    }

    @Test
    void testProcessEventsHandlesNullEvent() throws InterruptedException {
        // Queue returns null immediately (empty queue)
        when(fileWatchService.captureEvent(anyLong(), any(TimeUnit.class)))
                .thenReturn(null);

        incrementalIndexerService.start();
        Thread.sleep(200);
        incrementalIndexerService.stop();

        verifyNoInteractions(eventProcessor);
    }

    @Test
    void testStopWithoutStart() {
        // Should not throw
        incrementalIndexerService.stop();
        assertFalse(incrementalIndexerService.isRunning());
    }

    @Test
    void testMultipleStartCalls() {
        incrementalIndexerService.start();
        incrementalIndexerService.start(); // Second start should be no-op
        assertTrue(incrementalIndexerService.isRunning());
        incrementalIndexerService.stop();
    }
}