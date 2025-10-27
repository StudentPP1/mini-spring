package com.test;

import com.test.context.DefaultServletContext;
import com.test.initializer.ApplicationInitializer;
import com.test.server.HttpServer;

public class App {
    private static final int SERVER_PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) throws Exception {
        ApplicationInitializer initializer = new ApplicationInitializer();
        DefaultServletContext context = new DefaultServletContext();
        initializer.onStartup(context);
        context.initAll();
        try (var server = new HttpServer(SERVER_PORT, THREAD_POOL_SIZE, context)) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}