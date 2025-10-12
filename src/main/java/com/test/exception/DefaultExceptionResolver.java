package com.test.exception;

import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DefaultExceptionResolver implements ExceptionResolver {

    private static final Logger log = LogManager.getLogger(DefaultExceptionResolver.class);

    @Override
    public boolean resolve(Throwable throwable, HttpRequest request, HttpResponse response) {
        log.error("resolve error: {}", throwable.getMessage());
        if (throwable instanceof NotFoundException exception) {
            response.setHttpStatus(HttpStatus.NOT_FOUND);
            response.setBody(exception.getMessage());
            return true;
        }
        response.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setBody(throwable.getMessage());
        return true;
    }
}
