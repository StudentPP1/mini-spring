package com.test.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public final class UrlPatternMatcher {
    private static final Logger log = LogManager.getLogger(UrlPatternMatcher.class);

    private UrlPatternMatcher() {}
    private static final String ALL_PATH_PATTERN = "/*";
    public static boolean matches(List<String> patterns, String path) {
        for (var pattern : patterns) {
            if (match(pattern, path)) {
                log.trace("find matches for path: {} in patterns: {}", path, patterns);
                return true;
            }
        }
        log.trace("not found matches for path: {} in patterns: {}", path, patterns);
        return false;
    }

    private static boolean match(String pattern, String path) {
        if (ALL_PATH_PATTERN.equals(pattern)) return true;
        if (pattern.endsWith(ALL_PATH_PATTERN)) {
            String basePath = pattern.substring(0, pattern.length() - ALL_PATH_PATTERN.length());
            return basePath.equals(path) || path.startsWith(basePath + "/");
        }
        return pattern.equals(path);
    }
}
