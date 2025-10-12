package com.test.filter;

import com.test.handler.Handler;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class DefaultFilterChain implements FilterChain {
    private static final Logger log = LogManager.getLogger(DefaultFilterChain.class);
    private final List<Filter> filters;
    private final Handler finalHandler;
    private int currentFilterIndex = 0;

    public DefaultFilterChain(List<Filter> filters, Handler finalHandler) {
        this.filters = filters;
        this.finalHandler = finalHandler;
    }

    @Override
    public void next(HttpRequest request, HttpResponse response) throws Exception {
        log.debug("starting filter chain");
        if (currentFilterIndex < filters.size()) {
            Filter filter = filters.get(currentFilterIndex++);
            log.debug("start filter: {}", filter.getClass().getName());
            filter.doFilter(request, response, this);
        }
        else {
            finalHandler.handle(request, response);
        }
    }
}