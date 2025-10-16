package com.test.context;

import com.test.filter.Filter;
import com.test.servlets.Servlet;

import java.util.List;

public record Target(Servlet servlet, List<Filter> filters) {
}
