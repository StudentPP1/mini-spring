package com.test.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String path) {
        super("Not found servlet for pattern: %s".formatted(path));
    }
}
