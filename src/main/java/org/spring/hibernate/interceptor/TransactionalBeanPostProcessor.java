package org.spring.hibernate.interceptor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.bean.BeanPostProcessor;
import org.spring.hibernate.annotation.Transactional;
import org.spring.hibernate.session.SessionFactory;
import org.spring.hibernate.transaction.TransactionManager;

import java.lang.reflect.Proxy;

public class TransactionalBeanPostProcessor implements BeanPostProcessor {
    private static final Logger log = LogManager.getLogger(TransactionalBeanPostProcessor.class);
    private final TransactionManager transactionManager;
    private final SessionFactory sessionFactory;

    public TransactionalBeanPostProcessor(TransactionManager transactionManager, SessionFactory sessionFactory) {
        this.transactionManager = transactionManager;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> targetClass = bean.getClass();
        if (isTransactionalClass(targetClass)) {
            // example: class UserServiceImpl implements UserService
            Class<?>[] interfaces = targetClass.getInterfaces();
            if (interfaces.length == 0) {
                throw new IllegalStateException("Transactional bean must implement at least one interface: " + targetClass);
            }
            log.trace("create new transactional proxy for {}", targetClass.getSimpleName());
            return Proxy.newProxyInstance(
                    targetClass.getClassLoader(),
                    interfaces,
                    new TransactionInterceptor(bean, transactionManager, sessionFactory)
            );
        }
        return bean;
    }

    private boolean isTransactionalClass(Class<?> targetClass) {
        if (targetClass.isAnnotationPresent(Transactional.class)) return true;
        for (var method : targetClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Transactional.class)) return true;
        }
        return false;
    }
}
