package com.test.filter;

import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggingFilter implements Filter {

    private static final Logger log = LogManager.getLogger(LoggingFilter.class);

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain filterChain) throws Exception {
        log.info(">> {} {}", request.getMethod(), request.getPath());
        filterChain.next(request, response);
        log.info("<< done");
    }
}
