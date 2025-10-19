package com.test.servlet;

import com.test.context.DefaultServletContext;
import com.test.context.Target;
import com.test.filter.DefaultFilterChain;
import com.test.filter.FilterChain;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.initializer.ServletContextInitializer;

import java.util.List;

public class DispatcherServlet {
    private final DefaultServletContext context = new DefaultServletContext();
    private final List<ServletContextInitializer> initializers;

    public DispatcherServlet(List<ServletContextInitializer> initializers) {
        this.initializers = initializers;
    }

    public void init() throws Exception {
        for (var initializer : initializers) initializer.onStartup(context);
        context.initAll();
    }

    public void handle(HttpRequest request, HttpResponse response) throws Exception {
        Target target = context.findTarget(request.getPath());
        FilterChain filterChain = new DefaultFilterChain(target.filters(), target.servlet());
        filterChain.doFilter(request, response);
    }

    public void destroy() { context.destroyAll(); }
}
