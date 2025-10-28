package org.spring.config;

import org.spring.context.ServletContext;

import java.util.Map;

public interface ServletConfig {
    ServletContext getServletContext();
    Map<String,String> getInitParameters();
}
