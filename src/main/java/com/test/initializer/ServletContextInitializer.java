package com.test.initializer;

import com.test.context.ServletContext;

public interface ServletContextInitializer {
    void onStartup(ServletContext ctx) throws Exception;
}
