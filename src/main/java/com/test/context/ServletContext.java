package com.test.context;

import com.test.filter.Filter;
import com.test.filter.FilterRegistration;
import com.test.servlets.Servlet;
import com.test.servlets.ServletRegistration;

public interface ServletContext {
    <T> void setAttribute(String key, T value);
    <T> T getAttribute(String key, Class<T> type);
    ServletRegistration registerServlet(String name, Servlet servlet);
    FilterRegistration registerFilter(String name, Filter filter);
}
