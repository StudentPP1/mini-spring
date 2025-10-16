package com.test.http;

import java.util.List;

public final class UrlPatternMatcher {
    private UrlPatternMatcher() {}
    private static final String ALL_PATH_PATTERN = "/*";
    public static boolean matches(List<String> patterns, String path) {
        for (var pattern : patterns) {
            if (match(pattern, path)) {
                return true;
            }
        }
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
