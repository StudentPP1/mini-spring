package com.test.servlets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RegisteredServlet implements ServletRegistration {
    private static final Logger log = LogManager.getLogger(RegisteredServlet.class);
    final String name;
    final Servlet servlet;
    final List<String> map = new ArrayList<>();

    public RegisteredServlet(String name, Servlet servlet) {
        this.name = name;
        this.servlet = servlet;
    }

    public String getName() {
        return name;
    }

    public Servlet servlet() { return servlet; }
    public List<String> mappings() { return map; }

    @Override
    public void addMapping(String... urlPatterns) {
        log.debug("add {} mapping for servlet: {}", urlPatterns, name);
        this.map.addAll(List.of(urlPatterns));
    }
}
