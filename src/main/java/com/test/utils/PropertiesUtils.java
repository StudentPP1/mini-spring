package com.test.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropertiesUtils {
    private PropertiesUtils() {}
    private static final Logger log = LogManager.getLogger(PropertiesUtils.class);

    private static final class Holder {
        static final Properties PROPERTIES = loadProperties();

        private static Properties loadProperties() {
            Properties properties = new Properties();
            try (InputStream stream = PropertiesUtils.class
                    .getClassLoader()
                    .getResourceAsStream("application.properties")
            ) {
                properties.load(stream);
                log.debug("Properties was loaded");
                return properties;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    // initialize Holder once with first method call & save properties in variable
    // thread-save initializing of class guaranteed by JVM specification
    public static String getProperty(String key) {
        String property = Holder.PROPERTIES.getProperty(key);
        log.trace("find property: {} by key: {}", property, key);
        return property;
    }
}
