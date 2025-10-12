package com.test.exception;

import com.test.http.HttpRequest;
import com.test.http.HttpResponse;

public interface ExceptionResolver {
    boolean resolve(Throwable throwable, HttpRequest request, HttpResponse response);
}
