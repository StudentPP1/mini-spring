package com.test.handler;

import com.test.exception.NotFoundException;
import com.test.http.HttpMethod;
import com.test.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ControllerMapping implements HandlerMapping {
    private static final Logger log = LogManager.getLogger(ControllerMapping.class);
    private final Map<String, Handler> getHandlers = new LinkedHashMap<>();
    private final Map<String, Handler> postHandlers = new LinkedHashMap<>();
    private final Map<String, Handler> putHandlers = new LinkedHashMap<>();
    private final Map<String, Handler> deleteHandlers = new LinkedHashMap<>();

    public ControllerMapping addGetHandler(String pattern, Handler handler) {
        getHandlers.put(pattern, handler);
        return this;
    }

    public ControllerMapping addPostHandler(String pattern, Handler handler) {
        postHandlers.put(pattern, handler);
        return this;
    }

    public ControllerMapping addPutHandler(String pattern, Handler handler) {
        putHandlers.put(pattern, handler);
        return this;
    }

    public ControllerMapping addDeleteHandler(String pattern, Handler handler) {
        deleteHandlers.put(pattern, handler);
        return this;
    }

    @Override
    public Handler getHandler(HttpRequest request) {
        String pathPattern = request.getPath();
        HttpMethod method = request.getMethod();
        Handler handler = switch (method) {
            case GET -> getHandlers.get(pathPattern);
            case POST -> postHandlers.get(pathPattern);
            case PUT -> putHandlers.get(pathPattern);
            case DELETE -> deleteHandlers.get(pathPattern);
            default -> null;
        };
        if (handler == null) throw new NotFoundException(pathPattern);
        log.debug("mapping pattern: {} to handler: {}", pathPattern, handler.getClass().getName());
        return handler;
    }
}
