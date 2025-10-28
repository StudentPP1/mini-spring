package org.spring.filter;

import org.spring.config.ServletConfig;
import org.spring.http.HttpRequest;
import org.spring.http.HttpResponse;

public interface Filter {
    void init(ServletConfig config) throws Exception;
    void doFilter(HttpRequest request, HttpResponse response, FilterChain filterChain) throws Exception;
    void destroy();
}
