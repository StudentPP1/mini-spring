package com.test.filter;

import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.servlets.Servlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class DefaultFilterChain implements FilterChain {
    private static final Logger log = LogManager.getLogger(DefaultFilterChain.class);
    private final List<Filter> filters;
    private final Servlet servlet;
    private int currentFilterIndex = 0;

    public DefaultFilterChain(List<Filter> filters, Servlet servlet) {
        this.servlet = servlet;
        this.filters = filters;
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response) throws Exception {
        log.debug("starting filter chain");
        if (currentFilterIndex < filters.size()) {
            Filter filter = filters.get(currentFilterIndex++);
            log.debug("start filter: {}", filter.getClass().getName());
            filter.doFilter(request, response, this);
        } else {
            log.debug("start servlet: {}", servlet.getClass().getName());
            servlet.service(request, response);
        }
    }
}