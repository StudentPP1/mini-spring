package com.test.context;

import com.test.filter.Filter;
import com.test.filter.FilterRegistration;
import com.test.servlet.Servlet;
import com.test.servlet.ServletRegistration;

public interface ServletContext {
    <T> void setAttribute(String key, T value);
    <T> T getAttribute(String key, Class<T> type);
    ServletRegistration registerServlet(String name, Servlet servlet);
    FilterRegistration registerFilter(String name, Filter filter);
    void init() throws Exception;
    void destroy() throws Exception;
}
