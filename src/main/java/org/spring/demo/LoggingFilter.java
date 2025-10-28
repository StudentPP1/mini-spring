package org.spring.demo;

import org.spring.annotation.WebFilter;
import org.spring.config.ServletConfig;
import org.spring.filter.Filter;
import org.spring.filter.FilterChain;
import org.spring.http.HttpRequest;
import org.spring.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@WebFilter(order = -1)
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
