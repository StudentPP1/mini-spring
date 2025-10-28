package org.spring.servlet;

public interface ServletRegistration {
    void addMapping(String... urlPatterns);
}
