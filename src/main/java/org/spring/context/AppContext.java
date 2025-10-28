package org.spring.context;

import org.spring.bean.BeanFactory;
import org.spring.bean.BeanPostProcessor;
import org.spring.bean.DefaultBeanPostProcessor;
import org.spring.scan.ComponentScanner;

public class AppContext {
    private final BeanFactory factory;

    public AppContext(String basePackage) {
        try {
            var definitions = ComponentScanner.scan(basePackage);
            this.factory = new BeanFactory(definitions);
            this.factory.addPostProcessor(new DefaultBeanPostProcessor());
            // we can scan BeanPostProcessor because it's Component -> find them
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
