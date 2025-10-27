package com.test.http.resolver;

import com.test.annotation.PathVariable;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.utils.SimpleTypeConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Parameter;
import java.util.Map;

public class PathVariableResolver implements ArgumentResolver {
    private static final Logger log = LogManager.getLogger(PathVariableResolver.class);

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(PathVariable.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest request, HttpResponse response, Map<String, String> pathVariables) throws Exception {
        log.trace("Path variable resolve parameter: {}", parameter);
        String name = parameter.getAnnotation(PathVariable.class).value();
        String variable = pathVariables.get(name);
        Class<?> targetType = parameter.getType();
        return SimpleTypeConverter.convert(variable, targetType);
    }
}
