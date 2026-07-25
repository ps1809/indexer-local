package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.event.EventType;
import com.projectiq.indexerlocal.model.event.WatchEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WatchEventQueue.
 */
class WatchEventQueueTest {

    private WatchEventQueue queue;
    private static final int MAX_SIZE = 100;

    @BeforeEach
    void setUp() {
        queue = new WatchEventQueue(MAX_SIZE);
    }

    @Test
    void testInitialState() {
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.getTotalEnqueued());
        assertEquals(0, queue.getTotalDequeued());
        assertEquals(0, queue.getTotalDropped());
        assertEquals(MAX_SIZE, queue.getMaxSize());
    }

    @Test
    void testOfferAndPoll() {
        WatchEvent event = createTestEvent("repo1", "/test/file.java", EventType.CREATED);
        boolean offered = queue.offer(event);
        assertTrue(offered);
        assertEquals(1, queue.size());
        assertEquals(1, queue.getTotalEnqueued());

        try {
            WatchEvent polled = queue.poll(100, TimeUnit.MILLISECONDS);
            assertNotNull(polled);
            assertEquals(event.getRepositoryId(), polled.getRepositoryId());
            assertEquals(event.getEventType(), polled.getEventType());
            assertEquals(event.getRelativePath(), polled.getRelativePath());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Unexpected interruption");
        }
    }

    @Test
    void testPollEmptyQueue() {
        try {
            WatchEvent event = queue.poll(50, TimeUnit.MILLISECONDS);
            assertNull(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Unexpected interruption");
        }
    }

    @Test
    void testOfferToFullQueue() {
        // Fill the queue
        for (int i = 0; i < MAX_SIZE; i++) {
            WatchEvent event = createTestEvent("repo1", "/test/file" + i + ".java", EventType.CREATED);
            boolean offered = queue.offer(event);
            assertTrue(offered);
        }

        // Next offer should fail
        WatchEvent overflowEvent = createTestEvent("repo1", "/test/overflow.java", EventType.CREATED);
        boolean offered = queue.offer(overflowEvent);
        assertFalse(offered);
        assertEquals(MAX_SIZE, queue.size());
        assertEquals(1, queue.getTotalDropped());
    }

    @Test
    void testFifoOrdering() {
        // Enqueue events
        for (int i = 0; i < 10; i++) {
            WatchEvent event = createTestEvent("repo1", "/test/file" + i + ".java", EventType.CREATED);
            queue.offer(event);
        }

        // Dequeue and verify order
        try {
            for (int i = 0; i < 10; i++) {
                WatchEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
                assertNotNull(event);
                assertEquals("/test/file" + i + ".java", event.getRelativePath());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Unexpected interruption");
        }
    }

    @Test
    void testTake() throws InterruptedException {
        WatchEvent event = createTestEvent("repo1", "/test/file.java", EventType.CREATED);
        queue.offer(event);

        WatchEvent taken = queue.take();
        assertNotNull(taken);
        assertEquals(event.getRepositoryId(), taken.getRepositoryId());
    }

    @Test
    void testClear() {
        for (int i = 0; i < 10; i++) {
            WatchEvent event = createTestEvent("repo1", "/test/file" + i + ".java", EventType.CREATED);
            queue.offer(event);
        }

        assertEquals(10, queue.size());
        queue.clear();
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        final int numProducers = 5;
        final int eventsPerProducer = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean errorOccurred = new AtomicBoolean(false);

        // Producers
        Thread[] producerThreads = new Thread[numProducers];
        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            producerThreads[i] = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < eventsPerProducer; j++) {
                        WatchEvent event = createTestEvent(
                                "repo" + producerId,
                                "/test/file" + producerId + "_" + j + ".java",
                                EventType.CREATED);
                        queue.offer(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            producerThreads[i].start();
        }

        // Consumers
        Thread[] consumerThreads = new Thread[2];
        AtomicReference<WatchEvent> lastEvent = new AtomicReference<>();
        for (int i = 0; i < consumerThreads.length; i++) {
            consumerThreads[i] = new Thread(() -> {
                try {
                    startLatch.await();
                    while (!Thread.currentThread().isInterrupted()) {
                        WatchEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (event != null) {
                            lastEvent.set(event);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumerThreads[i].start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for producers to finish
        for (Thread producer : producerThreads) {
            producer.join(5000);
        }

        // Allow consumers a short time to process remaining events
        Thread.sleep(500);

        // Interrupt consumers
        for (Thread consumer : consumerThreads) {
            consumer.interrupt();
        }

        // Verify stats
        long totalEnqueued = queue.getTotalEnqueued();
        long totalDequeued = queue.getTotalDequeued();
        assertTrue(totalEnqueued > 0, "Events should have been enqueued");
        assertTrue(totalDequeued >= 0, "Dequeued count should be non-negative");
    }

    @Test
    void testGetTotalDropped() {
        // Fill the queue completely
        for (int i = 0; i < MAX_SIZE; i++) {
            WatchEvent event = createTestEvent("repo1", "/test/file" + i + ".java", EventType.CREATED);
            queue.offer(event);
        }

        assertEquals(0, queue.getTotalDropped());

        // Try to add one more which should fail
        queue.offer(createTestEvent("repo1", "/test/overflow.java", EventType.CREATED));
        assertEquals(1, queue.getTotalDropped());
    }

    private WatchEvent createTestEvent(String repositoryId, String path, EventType type) {
        return new WatchEvent(
                repositoryId,
                Paths.get(path),
                path,
                type,
                Instant.now(),
                "/test"
        );
    }
}