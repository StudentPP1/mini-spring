package com.test.dispatcher;

import com.test.exception.ExceptionResolver;
import com.test.filter.DefaultFilterChain;
import com.test.filter.Filter;
import com.test.filter.FilterChain;
import com.test.handler.Handler;
import com.test.handler.HandlerMapping;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.http.HttpStatus;

import java.util.List;

public class Dispatcher {
    private final HandlerMapping handlerMapping;
    private final List<Filter> filters;
    private final ExceptionResolver exceptionResolver;

    public Dispatcher(HandlerMapping handlerMapping, List<Filter> filters, ExceptionResolver exceptionResolver) {
        this.handlerMapping = handlerMapping;
        this.filters = filters;
        this.exceptionResolver = exceptionResolver;
    }

    public HttpResponse dispatch(HttpRequest request) {
        HttpResponse response = HttpResponse.build(HttpStatus.OK);
        try {
            Handler handler = handlerMapping.getHandler(request);
            FilterChain chain = new DefaultFilterChain(filters, handler);
            chain.next(request, response);
            return response;
        } catch (Throwable throwable) {
            HttpResponse errorResponse = HttpResponse.build(HttpStatus.INTERNAL_SERVER_ERROR);
            boolean handled = exceptionResolver.resolve(throwable, request, errorResponse);
            return handled ? errorResponse : HttpResponse.build(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
