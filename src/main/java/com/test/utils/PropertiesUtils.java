package com.test.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropertiesUtils {
    private static final Properties PROPERTIES = new Properties();
    private static final Logger log = LogManager.getLogger(PropertiesUtils.class);

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream stream = PropertiesUtils.class
                .getClassLoader()
                .getResourceAsStream("application.properties")
        ) {
            PROPERTIES.load(stream);
            log.debug("Properties was loaded");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getProperty(String key) {
        return PROPERTIES.getProperty(key);
    }
}
