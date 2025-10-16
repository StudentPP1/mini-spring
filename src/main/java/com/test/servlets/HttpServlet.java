package com.test.servlets;

import com.test.http.HttpMethod;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.http.HttpStatus;

public abstract class HttpServlet extends GenericServlet {
    @Override
    public final void service(HttpRequest request, HttpResponse response) throws Exception {
        HttpMethod method = request.getMethod();
        switch (method) {
            case GET -> doGet(request, response);
            case POST -> doPost(request, response);
            case PUT -> doPut(request, response);
            case DELETE -> doDelete(request, response);
            case OPTIONS -> doOptions(request, response);
            default -> {
                response.setHttpStatus(HttpStatus.NOT_FOUND);
                response.setHeader("Allow", "GET,POST,PUT,DELETE,OPTIONS");
            }
        }
    }

    protected void doGet(HttpRequest request, HttpResponse response) throws Exception {
    }

    protected void doPost(HttpRequest request, HttpResponse response) throws Exception {
    }

    protected void doPut(HttpRequest request, HttpResponse response) throws Exception {
    }

    protected void doDelete(HttpRequest request, HttpResponse response) throws Exception {
    }

    protected void doOptions(HttpRequest request, HttpResponse response) throws Exception {
        response.setHeader("Allow", "GET,POST,PUT,DELETE,OPTIONS");
        response.setHttpStatus(HttpStatus.NO_CONTENT);
    }
}
