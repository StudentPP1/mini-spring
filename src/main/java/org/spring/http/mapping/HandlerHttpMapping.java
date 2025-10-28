package org.spring.http.mapping;

import org.spring.annotation.*;
import org.spring.http.HttpMethod;
import org.spring.http.HttpRequest;
import org.spring.http.HttpResponse;
import org.spring.http.HttpStatus;
import org.spring.http.resolver.ArgumentResolver;
import org.spring.http.resolver.PathVariableResolver;
import org.spring.http.resolver.RequestBodyResolver;
import org.spring.mapper.convertor.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HandlerHttpMapping {
    private static final Logger log = LogManager.getLogger(HandlerHttpMapping.class);
    private final List<HandlerMethod> handlers = new ArrayList<>();
    private final List<ArgumentResolver> resolvers = List.of(
            new PathVariableResolver(),
            new RequestBodyResolver()
    );

    public void registerController(Object bean) {
        Class<?> beanClass = bean.getClass();
        String basePrefix = "";
        RestController annotation = beanClass.getAnnotation(RestController.class);
        if (annotation.path() != null && !annotation.path().isEmpty()) {
            basePrefix = annotation.path();
        }
        for (Method method : beanClass.getDeclaredMethods()) {
            method.setAccessible(true);
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            PutMapping putMapping = method.getAnnotation(PutMapping.class);
            DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
            String subPath = null;
            HttpMethod httpMethod = null;
            if (getMapping != null) {
                subPath = getMapping.path();
                httpMethod = HttpMethod.GET;
            }
            if (postMapping != null) {
                subPath = postMapping.path();
                httpMethod = HttpMethod.POST;
            }
            if (putMapping != null) {
                subPath = putMapping.path();
                httpMethod = HttpMethod.PUT;
            }
            if (deleteMapping != null) {
                subPath = deleteMapping.path();
                httpMethod = HttpMethod.DELETE;
            }
            String fullPath = joinPaths(basePrefix, subPath);
            log.trace("add handler method: {} at path: {}", httpMethod + " " + method.getName(), fullPath);
            handlers.add(new HandlerMethod(bean, method, new PathTemplate(fullPath), httpMethod));
        }
    }

    private static String normalizePath(String path) {
        String string = path.startsWith("/") ? path : "/" + path;
        if (string.length() > 1 && string.endsWith("/")) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    private static String joinPaths(String prefix, String subPath) {
        String normalizedPrefix = normalizePath(prefix);
        String normalizedSubPath = normalizePath(subPath);
        if (normalizedPrefix.equals("/")) return normalizedPrefix;
        if (normalizedSubPath.equals("/")) return normalizedSubPath;
        return normalizedPrefix + (normalizedSubPath.startsWith("/") ? "" : "/") + normalizedSubPath;
    }

    public boolean handle(HttpRequest request, HttpResponse response) throws Exception {
        String path = request.getPath();
        final HttpMethod httpMethod = request.getMethod();
        for (HandlerMethod handlerMethod : handlers) {
            if (handlerMethod.httpMethod() != httpMethod) {
                continue;
            }
            Map<String, String> pathVariables = handlerMethod.template().match(path);
            log.trace("path variables: {}", pathVariables);
            if (pathVariables == null) {
                continue;
            }
            Object[] args = resolveArgs(handlerMethod.method(), request, response, pathVariables);
            log.trace("handle path: {} with args: {}", path, args);
            Object result = handlerMethod.method().invoke(handlerMethod.controller(), args);
            log.trace("result: {}", result);
            writeReturnValue(result, response);
            return true;
        }
        return false;
    }

    private void writeReturnValue(Object result, HttpResponse response) {
        if (result == null) return;
        if (result instanceof String content) {
            response.setHeader("Content-Type", "text/plain; charset=UTF-8");
            response.setBody(content);
        } else {
            response.setHeader("Content-Type", "application/json; charset=UTF-8");
            response.setBody(ObjectMapper.write(result));
        }
        if (response.getHttpStatus() == null) {
            response.setHttpStatus(HttpStatus.OK);
        }
    }

    private Object[] resolveArgs(Method method, HttpRequest request, HttpResponse response, Map<String, String> pathVariables) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            boolean done = false;
            for (ArgumentResolver resolver : resolvers) {
                if (resolver.supports(parameter)) {
                    args[i] = resolver.resolve(parameter, request, response, pathVariables);
                    done = true;
                    break;
                }
            }
            if (!done) {
                throw new IllegalStateException("No resolver for parameter: " + parameter);
            }
        }
        return args;
    }
}
