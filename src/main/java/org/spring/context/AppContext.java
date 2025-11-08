package org.spring.context;

import lombok.Getter;
import org.spring.bean.BeanFactory;
import org.spring.bean.BeanPostProcessor;
import org.spring.bean.DefaultBeanPostProcessor;
import org.spring.hibernate.initializer.HibernateStarter;
import org.spring.scan.ComponentScanner;

@Getter
public class AppContext {
    private final BeanFactory factory;

    public AppContext(String basePackage) {
        try {
            var definitions = ComponentScanner.scan(basePackage);
            this.factory = new BeanFactory(definitions);
            HibernateStarter.init(this.factory, basePackage);
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
}