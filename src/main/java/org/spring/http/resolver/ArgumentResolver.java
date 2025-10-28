package org.spring.http.resolver;

import org.spring.http.HttpRequest;
import org.spring.http.HttpResponse;

import java.lang.reflect.Parameter;
import java.util.Map;

public interface ArgumentResolver {
    boolean supports(Parameter parameter);
    Object resolve(Parameter parameter,
                   HttpRequest request,
                   HttpResponse response,
                   Map<String,String> pathVariables
    ) throws Exception;
}
