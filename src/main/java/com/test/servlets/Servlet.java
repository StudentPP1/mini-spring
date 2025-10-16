package com.test.servlets;

import com.test.config.ServletConfig;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;

public interface Servlet {
    void init(ServletConfig config) throws Exception;
    void service(HttpRequest request, HttpResponse response) throws Exception;
    void destroy();
}
