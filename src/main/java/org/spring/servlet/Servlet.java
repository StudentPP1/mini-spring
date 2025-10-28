package org.spring.servlet;

import org.spring.config.ServletConfig;
import org.spring.http.HttpRequest;
import org.spring.http.HttpResponse;

public interface Servlet {
    void init(ServletConfig config) throws Exception;
    void service(HttpRequest request, HttpResponse response) throws Exception;
    void destroy();
}
