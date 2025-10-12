package com.test.handler;

import com.test.http.HttpRequest;

public interface HandlerMapping {
    Handler getHandler(HttpRequest request);
}
