package com.test.initializer;

import com.test.context.ServletContext;
import com.test.demo.LoggingFilter;
import com.test.demo.NoteServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ApplicationInitializer implements ServletContextInitializer {
    private static final Logger log = LogManager.getLogger(ApplicationInitializer.class);

    @Override
    public void onStartup(ServletContext ctx) {
        log.debug("register servlets & filters to context");
        ctx.registerServlet("noteServlet", new NoteServlet())
                .addMapping("/note/*");
        ctx.registerFilter("loggingFilter", new LoggingFilter())
                .addMapping("/*");
    }
}
