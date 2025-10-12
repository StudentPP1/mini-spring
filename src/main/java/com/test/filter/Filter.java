package com.test.filter;

import com.test.http.HttpRequest;
import com.test.http.HttpResponse;

public interface Filter {
    void doFilter(HttpRequest request, HttpResponse response, FilterChain filterChain) throws Exception;
}
