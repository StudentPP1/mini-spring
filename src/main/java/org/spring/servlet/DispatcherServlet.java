package org.spring.servlet;

import org.spring.config.ServletConfig;
import org.spring.context.ServletContext;
import org.spring.http.mapping.HandlerHttpMapping;
import org.spring.http.HttpRequest;
import org.spring.http.HttpResponse;
import org.spring.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DispatcherServlet implements Servlet {
    private static final Logger log = LogManager.getLogger(DispatcherServlet.class);

    private HandlerHttpMapping mapping;

    @Override
    public void init(ServletConfig config) {
        log.debug("init dispatcher servlet");
        ServletContext context = config.getServletContext();
        this.mapping = context.getAttribute("handlerHttpMapping", HandlerHttpMapping.class);
        log.debug("load mapping from context");
        if (mapping == null) throw new IllegalStateException("handlerHttpMapping not set");
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        try {
            log.debug("handle: {}", request.getPath());
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
        response.setBody("no handler for " + request.getMethod() + " " + request.getPath());
    }

    @Override public void destroy() {}
}
