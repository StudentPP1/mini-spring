package com.test.filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RegisteredFilter implements FilterRegistration {
    private static final Logger log = LogManager.getLogger(RegisteredFilter.class);
    final String name;
    final Filter filter;
    final List<String> map = new ArrayList<>();

    public String getName() {
        return name;
    }

    public RegisteredFilter(String name, Filter filter) {
        this.name = name;
        this.filter = filter;
    }

    public Filter filter() { return filter; }
    public List<String> mappings() { return map; }

    @Override
    public void addMapping(String... urlPattern) {
        log.debug("add {} mapping for filter: {}", urlPattern, name);
        this.map.addAll(List.of(urlPattern));
    }
}
