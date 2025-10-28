package org.spring.http.mapping;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathTemplate {
    private static final Logger log = LogManager.getLogger(PathTemplate.class);
    private final Pattern pattern;
    private final List<String> variableNames;
    private final String STRING_START = "^";
    private final String END_START = "$";

    public PathTemplate(String template) {
        String path = template.startsWith("/") ? template : "/" + template;
        this.variableNames = new ArrayList<>();
        StringBuilder regex = new StringBuilder(STRING_START);
        for (String part : path.split("/")) {
            if (part.isEmpty()) {
                continue;
            }
            regex.append("/");
            if (part.startsWith("{") && part.endsWith("}")) {
                String name = part.substring(1, part.length() - 1);
                variableNames.add(name);
                regex.append("(?<").append(name).append(">[^/]+)");
            } else {
                regex.append(Pattern.quote(part));
            }
        }
        if (path.endsWith("/")) {
            regex.append("/");
        }
        regex.append(END_START);
        this.pattern = Pattern.compile(regex.toString());
        log.trace("compile pattern: {}", this.pattern);
    }

    public Map<String,String> match(String path) {
        String p = path.startsWith("/") ? path : "/" + path;
        Matcher matcher = pattern.matcher(p);
        if (!matcher.matches()) {
            return null;
        }
        Map<String,String> variables = new HashMap<>();
        for (String name : variableNames) {
            variables.put(name, matcher.group(name));
        }
        return variables;
    }
}
