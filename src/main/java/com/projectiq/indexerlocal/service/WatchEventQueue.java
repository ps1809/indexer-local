package com.projectiq.indexerlocal.service;

import com.projectiq.indexerlocal.model.event.WatchEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe, FIFO event queue for watch events.
 * Uses a BlockingQueue for efficient producer-consumer pattern support.
 */
public class WatchEventQueue {

    private final BlockingQueue<WatchEvent> eventQueue;
    private final int maxSize;
    private long totalEnqueued;
    private long totalDequeued;
    private long totalDropped;

    public WatchEventQueue(int maxSize) {
        this.maxSize = maxSize;
        this.eventQueue = new LinkedBlockingQueue<>(maxSize);
        this.totalEnqueued = 0;
        this.totalDequeued = 0;
        this.totalDropped = 0;
    }

    /**
     * Non-blocking enqueue operation.
     * Returns false if the queue is full (dropped).
     */
    public boolean offer(WatchEvent event) {
        boolean offered = eventQueue.offer(event);
        if (offered) {
            totalEnqueued++;
        } else {
            totalDropped++;
        }
        return offered;
    }

    /**
     * Blocking dequeue operation with timeout.
     * Returns null if timeout expires.
     */
    public WatchEvent poll(long timeout, TimeUnit unit) throws InterruptedException {
        WatchEvent event = eventQueue.poll(timeout, unit);
        if (event != null) {
            totalDequeued++;
        }
        return event;
    }

    /**
     * Blocking dequeue operation without timeout.
     */
    public WatchEvent take() throws InterruptedException {
        WatchEvent event = eventQueue.take();
        if (event != null) {
            totalDequeued++;
        }
        return event;
    }

    /**
     * Returns the current size of the queue.
     */
    public int size() {
        return eventQueue.size();
    }

    /**
     * Returns true if the queue is empty.
     */
    public boolean isEmpty() {
        return eventQueue.isEmpty();
    }

    /**
     * Clears all events from the queue.
     */
    public void clear() {
        eventQueue.clear();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public long getTotalEnqueued() {
        return totalEnqueued;
    }

    public long getTotalDequeued() {
        return totalDequeued;
    }

    public long getTotalDropped() {
        return totalDropped;
    }
}