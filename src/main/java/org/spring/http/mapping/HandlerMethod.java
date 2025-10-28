package org.spring.http.mapping;

import org.spring.http.HttpMethod;

import java.lang.reflect.Method;

public record HandlerMethod(Object controller,
                            Method method,
                            PathTemplate template,
                            HttpMethod httpMethod
) {
}
