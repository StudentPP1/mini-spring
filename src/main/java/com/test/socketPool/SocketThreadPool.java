package com.test.socketPool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketThreadPool {
    private static final Logger LOGGER = LogManager.getLogger(SocketThreadPool.class);
    private final BlockingQueue<SocketThread> pool;
    private final AtomicBoolean isPoolWorking = new AtomicBoolean(true);

    public SocketThreadPool(int poolSize) {
        this.pool = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            SocketThread thread = new SocketThread("socket-thread-%s".formatted(i + 1), this);
            thread.start();
            this.pool.add(thread);
        }
        LOGGER.trace("Thread pool created with {} threads", poolSize);
    }

    public void execute(Runnable task) {
        if (!isPoolWorking.get()) {
            throw new RejectedExecutionException("Pool shut down");
        }
        try {
            SocketThread worker = pool.take();
            worker.assign(task);
            LOGGER.trace("Task assign to {}. Pool size = {}", worker.getName(), pool.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Interrupted while enqueuing task", e);
        }
    }

    public void shutdown() {
        isPoolWorking.set(false);
        pool.forEach(Thread::interrupt);
    }

    void release(SocketThread worker) {
        if (isPoolWorking.get()) {
            pool.offer(worker);
            LOGGER.trace("Thread {} released to pool. Pool size: {}", worker.getName(), pool.size());
        }
    }
}