package com.test.servlets;

public interface ServletRegistration {
    void addMapping(String... urlPatterns);
}
