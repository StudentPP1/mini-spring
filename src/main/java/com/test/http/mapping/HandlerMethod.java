package com.test.http.mapping;

import com.test.http.HttpMethod;

import java.lang.reflect.Method;

public record HandlerMethod(Object controller,
                            Method method,
                            PathTemplate template,
                            HttpMethod httpMethod
) {
}
