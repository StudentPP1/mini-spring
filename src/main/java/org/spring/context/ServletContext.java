package org.spring.context;

import org.spring.filter.Filter;
import org.spring.filter.FilterRegistration;
import org.spring.servlet.Servlet;
import org.spring.servlet.ServletRegistration;

public interface ServletContext {
    <T> void setAttribute(String key, T value);
    <T> T getAttribute(String key, Class<T> type);
    ServletRegistration registerServlet(String name, Servlet servlet);
    FilterRegistration registerFilter(String name, Filter filter);
    void init() throws Exception;
    void destroy() throws Exception;
}
