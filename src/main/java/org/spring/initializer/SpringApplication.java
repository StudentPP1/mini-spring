package org.spring.initializer;

import org.spring.annotation.SpringBootApplication;
import org.spring.context.DefaultServletContext;
import org.spring.server.HttpServer;

public final class SpringApplication {
    private SpringApplication() {
    }

    public static void run(Class<?> bootClass) {
        SpringBootApplication annotation = bootClass.getAnnotation(SpringBootApplication.class);
        String basePackage = annotation != null && !annotation.scanBasePackage().isEmpty()
                ? annotation.scanBasePackage()
                : bootClass.getPackageName();
        DefaultServletContext context = new DefaultServletContext();
        try {
            ApplicationInitializer initializer = new ApplicationInitializer(basePackage);
            initializer.onStartup(context);
            context.init();
            try (var server = new HttpServer(context)) {
                server.start();
            }
        } catch (Exception _) {
            context.destroy();
        }
    }
}
