package com.test.context;

import com.test.bean.BeanFactory;
import com.test.bean.BeanPostProcessor;
import com.test.bean.DefaultBeanPostProcessor;
import com.test.scan.ComponentScanner;

public class AppContext {
    private final BeanFactory factory;

    public AppContext(String basePackage) {
        try {
            var definitions = ComponentScanner.scan(basePackage);
            this.factory = new BeanFactory(definitions);
            this.factory.addPostProcessor(new DefaultBeanPostProcessor());
            definitions.stream()
                    .filter(definition -> BeanPostProcessor.class.isAssignableFrom(definition.beanClass()))
                    .forEach(definition -> {
                        var postProcessor = (BeanPostProcessor) factory.getBean(definition.name());
                        factory.addPostProcessor(postProcessor);
                    });
            this.factory.preInstanceSingletons();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public BeanFactory getFactory() {
        return factory;
    }
}
