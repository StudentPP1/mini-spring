package org.spring.hibernate.interceptor;

import org.spring.hibernate.annotation.Transactional;
import org.spring.hibernate.session.Session;
import org.spring.hibernate.session.SessionFactory;
import org.spring.hibernate.transaction.Propagation;
import org.spring.hibernate.transaction.TransactionDefinition;
import org.spring.hibernate.transaction.TransactionManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

public final class TransactionInterceptor implements InvocationHandler {
    private final Object target;
    private final TransactionManager transactionManager;
    private final SessionFactory sessionFactory;

    public TransactionInterceptor(Object target, TransactionManager transactionManager, SessionFactory sessionFactory) {
        this.target = target;
        this.transactionManager = transactionManager;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Transactional transactional = findTransactionalAnnotation(method);
        if (transactional == null) {
            return method.invoke(target, args);
        }
        TransactionDefinition definition = new TransactionDefinition(
                transactional.propagation(),
                transactional.isolation(),
                transactional.readOnly(),
                transactional.timeoutSeconds()
        );
        Session session = null;
        // first @Transaction -> false, second -> true
        boolean alreadyExists = transactionManager.isActive();
        // start transaction accordingly to definition
        transactionManager.begin(definition);
        // if after definition transaction manager has new active transaction / REQUIRES_NEW -> create new physical transaction
        boolean startedNewTransaction = (!alreadyExists && transactionManager.isActive()) || definition.propagation() == Propagation.REQUIRES_NEW;
        try {
            session = sessionFactory.openSession();
            Object result;
            result = processMethodInvocation(method, args, startedNewTransaction, definition, session);
            flushIfStartedNewTransaction(startedNewTransaction, definition, session);
            return result;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private Object processMethodInvocation(Method method, Object[] args, boolean startedNew, TransactionDefinition definition, Session session) throws Throwable {
        Object result;
        try {
            result = method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable ex = e.getTargetException();
            if (startedNew) {
                // if non-checked exception -> rollback
                if (ex instanceof RuntimeException || ex instanceof Error) {
                    transactionManager.rollback();
                } else {
                    flushIfStartedNewTransaction(true, definition, session);
                }
            }
            throw ex;
        }
        return result;
    }

    private void flushIfStartedNewTransaction(boolean startedNewTransaction, TransactionDefinition definition, Session session) throws SQLException {
        if (startedNewTransaction) {
            // if transaction not read-only & it is active -> sync to db
            if (!definition.readOnly() && transactionManager.isActive()) {
                session.flush();
            }
            transactionManager.commit();
        }
    }

    private Transactional findTransactionalAnnotation(Method method) {
        if (method.isAnnotationPresent(Transactional.class)) {
            return method.getAnnotation(Transactional.class);
        }
        Class<?> targetClass = method.getDeclaringClass();
        while (targetClass != null && targetClass != Object.class) {
            if (targetClass.isAnnotationPresent(Transactional.class)) {
                return targetClass.getAnnotation(Transactional.class);
            }
            targetClass = targetClass.getSuperclass();
        }
        return null;
    }
}
