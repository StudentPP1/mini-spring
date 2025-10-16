package com.test.demo;

import com.test.config.ServletConfig;
import com.test.filter.Filter;
import com.test.filter.FilterChain;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggingFilter implements Filter {

    private static final Logger log = LogManager.getLogger(LoggingFilter.class);

    @Override
    public void init(ServletConfig config) throws Exception {

    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain filterChain) throws Exception {
        log.info(">> {} {}", request.getMethod(), request.getPath());
        filterChain.doFilter(request, response);
        log.info("<< done");
    }

    @Override
    public void destroy() {

    }
}
