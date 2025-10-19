package com.test;

import com.test.initializer.ApplicationInitializer;
import com.test.server.HttpServer;
import com.test.servlet.DispatcherServlet;

import java.util.List;

public class App {
    private static final int SERVER_PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        DispatcherServlet dispatcherServlet = new DispatcherServlet(List.of(new ApplicationInitializer()));
        try (var server = new HttpServer(SERVER_PORT, THREAD_POOL_SIZE, dispatcherServlet)) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}