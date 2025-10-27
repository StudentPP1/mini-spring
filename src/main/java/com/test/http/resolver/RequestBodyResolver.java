package com.test.http.resolver;

import com.test.annotation.PathVariable;
import com.test.annotation.RequestBody;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.mapper.convertor.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Parameter;
import java.util.Map;

public class RequestBodyResolver implements ArgumentResolver {
    private static final Logger log = LogManager.getLogger(RequestBodyResolver.class);

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest request, HttpResponse response, Map<String, String> pathVariables) throws Exception {
        log.trace("Request body resolve parameter: {}", parameter);
        Class<?> targetType = parameter.getType();
        return ObjectMapper.parse(request.getBody(), targetType);
    }
}
