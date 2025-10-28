package org.spring.filter;

import org.spring.http.HttpRequest;
import org.spring.http.HttpResponse;

public interface FilterChain {
    void doFilter(HttpRequest request, HttpResponse response) throws Exception;
}
