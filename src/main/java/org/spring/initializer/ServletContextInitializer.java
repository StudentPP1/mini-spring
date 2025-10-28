package org.spring.initializer;

import org.spring.context.ServletContext;

public interface ServletContextInitializer {
    void onStartup(ServletContext ctx) throws Exception;
}
