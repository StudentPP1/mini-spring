package com.test.servlet;

import com.test.config.ServletConfig;
import com.test.context.ServletContext;
import com.test.http.mapping.HandlerHttpMapping;
import com.test.http.HttpRequest;
import com.test.http.HttpResponse;
import com.test.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DispatcherServlet implements Servlet {
    private static final Logger log = LogManager.getLogger(DispatcherServlet.class);
    private ServletContext context;
    private HandlerHttpMapping mapping;

    @Override
    public void init(ServletConfig config) {
        log.debug("init dispatcher servlet");
        this.context = config.getServletContext();
        this.mapping = this.context.getAttribute("handlerHttpMapping", HandlerHttpMapping.class);
        log.debug("load mapping from context");
        if (mapping == null) throw new IllegalStateException("handlerHttpMapping not set");
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        try {
            log.debug("DispatcherServlet handle: {}", request.getPath());
            boolean handled = mapping.handle(request, response);
            if (!handled) {
                errorResponse(request, response);
            }
        } catch (Exception _) {
            errorResponse(request, response);
        }
    }

    private void errorResponse(HttpRequest request, HttpResponse response) {
        response.setHttpStatus(HttpStatus.NOT_FOUND);
        response.setBody("No handler for " + request.getMethod() + " " + request.getPath());
    }

    @Override public void destroy() {}
}
