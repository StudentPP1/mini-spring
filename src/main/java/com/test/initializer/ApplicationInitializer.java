package com.test.initializer;

import com.test.annotation.RestController;
import com.test.annotation.WebFilter;
import com.test.context.AppContext;
import com.test.context.ServletContext;
import com.test.filter.Filter;
import com.test.filter.FilterEntry;
import com.test.http.mapping.HandlerHttpMapping;
import com.test.servlet.DispatcherServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ApplicationInitializer implements ServletContextInitializer {
    private static final Logger log = LogManager.getLogger(ApplicationInitializer.class);

    @Override
    public void onStartup(ServletContext ctx) {
        log.trace("start up app");
        AppContext context = new AppContext("com.test");
        log.trace("context created");
        HandlerHttpMapping mapping = new HandlerHttpMapping();
        for (Object bean : context.getFactory().getAllBeans()) {
            if (bean.getClass().isAnnotationPresent(RestController.class)) {
                log.trace("register controller: {}", bean.getClass().getName());
                mapping.registerController(bean);
            }
        }
        List<FilterEntry> filters = new ArrayList<>();
        context.getFactory().getBeanByType(Filter.class).forEach((name, filter) -> {
            if (filter.getClass().isAnnotationPresent(WebFilter.class)) {
                log.trace("find filter: {}", name);
                WebFilter annotation = filter.getClass().getAnnotation(WebFilter.class);
                int order = annotation.order();
                String path = annotation.path();
                filters.add(new FilterEntry(name, filter, path, order));
            }
        });
        filters.sort(Comparator.comparingInt(FilterEntry::order));
        for (FilterEntry filter : filters) {
            log.trace("register filter: {}", filter.name());
            ctx.registerFilter(filter.name(), filter.filter())
                    .addMapping(filter.path());
        }
        ctx.setAttribute("handlerHttpMapping", mapping);
        ctx.registerServlet("dispatcher", new DispatcherServlet()).addMapping("/*");
    }
}
