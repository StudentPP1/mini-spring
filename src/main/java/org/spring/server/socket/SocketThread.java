package org.spring.server.socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketThread extends Thread {
    private static final Logger log = LogManager.getLogger(SocketThread.class);
    private final SocketThreadPool parentPool;
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

    public SocketThread(String name, SocketThreadPool parentPool) {
        super(name);
        this.parentPool = parentPool;
        setDaemon(true);
    }

    public void assign(Runnable task) throws InterruptedException {
        log.trace("{} add task: {} to list", Thread.currentThread().getName(), task);
        tasks.put(task);
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                Runnable task = tasks.take();
                try {
                    task.run();
                } finally {
                    parentPool.release(this);
                }
            }
        } catch (InterruptedException ignored) {
            log.trace("{} interrupted", Thread.currentThread().getName());
            Thread.currentThread().interrupt();
        }
    }
}
