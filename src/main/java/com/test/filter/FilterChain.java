package com.test.filter;

import com.test.http.HttpRequest;
import com.test.http.HttpResponse;

public interface FilterChain {
    void next(HttpRequest request, HttpResponse response) throws Exception;
}
