package org.spring.http.resolver;

import org.spring.annotation.PathVariable;
import org.spring.http.HttpRequest;
import org.spring.http.HttpResponse;
import org.spring.utils.SimpleTypeConverter;
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
