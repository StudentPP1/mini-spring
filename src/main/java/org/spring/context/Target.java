package org.spring.context;

import org.spring.filter.Filter;
import org.spring.servlet.Servlet;

import java.util.List;

public record Target(Servlet servlet, List<Filter> filters) {
}
