package com.test.filter;

import com.test.http.HttpRequest;
import com.test.http.HttpResponse;

public interface FilterChain {
    void doFilter(HttpRequest request, HttpResponse response) throws Exception;
}
