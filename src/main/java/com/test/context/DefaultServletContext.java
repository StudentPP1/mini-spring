package com.test.context;

import com.test.config.ServletConfig;
import com.test.filter.Filter;
import com.test.filter.FilterRegistration;
import com.test.filter.RegisteredFilter;
import com.test.http.UrlPatternMatcher;
import com.test.servlet.RegisteredServlet;
import com.test.servlet.Servlet;
import com.test.servlet.ServletRegistration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultServletContext implements ServletContext, ServletConfig {
    private static final Logger log = LogManager.getLogger(DefaultServletContext.class);
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final List<RegisteredServlet> servlets = new ArrayList<>();
    private final List<RegisteredFilter> filters = new ArrayList<>();

    @Override
    public <T> void setAttribute(String key, T value) {
        this.attributes.putIfAbsent(key, value);
    }

    @Override
    public <T> T getAttribute(String key, Class<T> type) {
        return (T) this.attributes.get(key);
    }

    @Override
    public ServletRegistration registerServlet(String name, Servlet servlet) {
        RegisteredServlet registration = new RegisteredServlet(name, servlet);
        this.servlets.add(registration);
        log.debug("register servlet: {}", name);
        return registration;
    }

    @Override
    public FilterRegistration registerFilter(String name, Filter filter) {
        RegisteredFilter registration = new RegisteredFilter(name, filter);
        this.filters.add(registration);
        log.debug("register filter: {}", name);
        return registration;
    }

    @Override
    public ServletContext getServletContext() {
        return this;
    }

    @Override
    public Map<String, String> getInitParameters() {
        // don't implementing xml configuration
        return Map.of();
    }

    public void initAll() throws Exception {
        // get init parameters for each object
        // for example open connection with db in servet
        for (var f : filters) {
            log.debug("put context to filter: {}", f.getName());
            f.filter().init(this);
        }
        for (var s : servlets) {
            log.debug("put context to servlet: {}", s.getName());
            s.servlet().init(this);
        }
    }

    public void destroyAll() {
        // destroy all before close container
        // for example call destroy method in servlet that close connection with db
        log.debug("destroy each servlet & filter");
        servlets.forEach(s -> s.servlet().destroy());
        filters.forEach(f -> f.filter().destroy());
    }

    public Target findTarget(String path) {
        log.debug("find target for path: {}", path);
        var servlet = servlets.stream()
                .filter(s -> UrlPatternMatcher.matches(s.mappings(), path))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Not Found: " + path));
        log.debug("find servlet: {}", servlet.getName());
        var chainFilters = filters.stream()
                .filter(f -> UrlPatternMatcher.matches(f.mappings(), path))
                .map(RegisteredFilter::filter)
                .toList();
        log.debug("find filters: {}", filters);
        return new Target(servlet.servlet(), chainFilters);
    }
}
