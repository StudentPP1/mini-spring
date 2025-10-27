package com.test.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PropertyResolver {
    private static final Logger log = LogManager.getLogger(PropertyResolver.class);

    public static String resolve(String expression) {
        log.trace("resolve {}", expression);
        if (expression == null) return null;
        if (expression.startsWith("${") && expression.endsWith("}")) {
            String body = expression.substring(2, expression.length() - 1);
            int colonIndex = body.indexOf(':');
            String key = (colonIndex >= 0) ? body.substring(0, colonIndex) : body;
            String defaultValue = (colonIndex >= 0) ? body.substring(colonIndex + 1) : null;
            try {
                return PropertiesUtils.getProperty(key);
            } catch (Exception _) {
                log.trace("return property by default {}", defaultValue);
                return defaultValue;
            }
        }
        log.trace("return expression {}", expression);
        return expression;
    }
}
