package com.test.multithreading;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketThread extends Thread {
    private final SocketThreadPool parentPool;
    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();

    public SocketThread(String name, SocketThreadPool parentPool) {
        super(name);
        this.parentPool = parentPool;
        setDaemon(true);
    }

    public void assign(Runnable task) throws InterruptedException {
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
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
