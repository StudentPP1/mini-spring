package org.spring.hibernate.session;

import org.spring.hibernate.connection.ConnectionProvider;
import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.transaction.TransactionManager;

import java.util.Map;

public class DefaultSessionFactory implements SessionFactory {
    private final ConnectionProvider connectionProvider;
    private final TransactionManager transactionManager;
    private final Map<Class<?>, EntityMetadata> entities;
    private volatile boolean closed = false;

    public DefaultSessionFactory(ConnectionProvider connectionProvider, TransactionManager transactionManager, Map<Class<?>, EntityMetadata> entities) {
        this.connectionProvider = connectionProvider;
        this.transactionManager = transactionManager;
        this.entities = entities;
    }

    @Override
    public Session openSession() {
        if (closed) throw new IllegalStateException("SessionFactory is closed");
        return new DefaultSession(connectionProvider, transactionManager, entities);
    }

    @Override
    public Map<Class<?>, EntityMetadata> getMetadata() {
        return entities;
    }

    @Override
    public void close() {
        closed = true;
    }
}
