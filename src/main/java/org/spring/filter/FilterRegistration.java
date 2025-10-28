package org.spring.filter;

public interface FilterRegistration {
    void addMapping(String... urlPattern);
}
