package org.spring.bean;

import org.spring.annotation.PostConstruct;

import java.lang.reflect.Method;

public final class DefaultBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                method.setAccessible(true);
                try {
                    method.invoke(bean);
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to execute @PostConstruct on bean " + beanName, e);
                }
            }
        }
        return bean;
    }
}
