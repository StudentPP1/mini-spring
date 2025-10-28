package com.test;

import com.test.context.DefaultServletContext;
import com.test.initializer.ApplicationInitializer;
import com.test.server.HttpServer;

public class App {
    public static void main(String[] args) throws Exception {
        ApplicationInitializer initializer = new ApplicationInitializer();
        DefaultServletContext context = new DefaultServletContext();
        initializer.onStartup(context);
        context.init();
        try (var server = new HttpServer(context)) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            server.start();
        } catch (Exception e) {
            context.destroy();
            throw new RuntimeException(e);
        }
    }
}