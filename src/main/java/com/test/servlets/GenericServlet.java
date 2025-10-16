package com.test.servlets;

import com.test.config.ServletConfig;
import com.test.context.ServletContext;

public abstract class GenericServlet implements Servlet {
    protected ServletConfig config;

    @Override
    public void init(ServletConfig config) {
        this.config = config;
    }

    @Override
    public void destroy() {
    }

    protected ServletContext getServletContext() {
        return config.getServletContext();
    }
}
