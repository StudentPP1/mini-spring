package com.test.handler;

import com.test.http.HttpRequest;
import com.test.http.HttpResponse;

public interface Handler {
    void handle(HttpRequest request, HttpResponse response) throws Exception;
}
