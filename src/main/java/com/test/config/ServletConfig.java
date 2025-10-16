package com.test.config;

import com.test.context.ServletContext;

import java.util.Map;

public interface ServletConfig {
    ServletContext getServletContext();
    Map<String,String> getInitParameters();
}
