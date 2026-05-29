package org.spring.hibernate.session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.connection.ConnectionProvider;
import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.query.Query;
import org.spring.hibernate.query.SimpleQuery;
import org.spring.hibernate.transaction.TransactionManager;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Map;

public class DefaultSession implements InternalSession {
    private static final Logger log = LogManager.getLogger(DefaultSession.class);
    private final ConnectionProvider connectionProvider;
    private final TransactionManager transactionManager;
    private final Map<Class<?>, EntityMetadata> entities;
    private final PersistenceContext persistenceContext = new PersistenceContext();
    private final EntityLoader loader = new EntityLoader(this);
    private final EntityPersister persister = new EntityPersister(this);
    private boolean closed = false;

    public DefaultSession(ConnectionProvider connectionProvider, TransactionManager transactionManager, Map<Class<?>, EntityMetadata> entities) {
        this.connectionProvider = connectionProvider;
        this.transactionManager = transactionManager;
        this.entities = entities;
    }

    @Override
    public <T, R> T find(Class<T> entityClass, R id) {
        return loader.load(entityClass, id);
    }

    @Override
    public void persist(Object entity) {
        persister.persist(entity);
    }

    @Override
    public void merge(Object entity) {
        persister.merge(entity);
    }

    @Override
    public void remove(Object entity) {
        persister.remove(entity);
    }

    @Override
    public <T> Query<T> createQuery(String sql, Class<T> resultType) {
        if (sql.toUpperCase().startsWith("SELECT")) {
            EntityMetadata metadata = getEntityMetadata(resultType);
            String className = resultType.getSimpleName();
            sql = sql.replaceAll("\\b%s\\b".formatted(className), metadata.tableName());
            return new SimpleQuery<>(resultType, sql, this, metadata);
        }
        throw new UnsupportedOperationException("Only SELECT queries are supported");
    }

    @Override
    public void flush() {
        // TODO: implement dirty checking
    }

    @Override
    public void close() {
        log.trace("close session");
        if (closed) return;
        closed = true;
        if (!transactionManager.isActive()) {
            try {
                connectionProvider.release(true);
            } catch (Exception ignored) {}
        }
        persistenceContext.clear();
    }

    @Override
    public PersistenceContext getPersistenceContext() {
        return persistenceContext;
    }

    @Override
    public Map<Class<?>, EntityMetadata> getEntities() {
        return entities;
    }

    @Override
    public Connection getConnection() {
        return this.connectionProvider.get();
    }

    @Override
    public <T> EntityMetadata getEntityMetadata(Class<T> entityClass) {
        EntityMetadata metadata = entities.get(entityClass);
        if (metadata == null) throw new RuntimeException("Entity not registered: " + entityClass);
        return metadata;
    }

    @Override
    public Object getIdValue(Object entity, EntityMetadata m) throws NoSuchFieldException, IllegalAccessException {
        Field id = entity.getClass().getDeclaredField(m.idField());
        id.setAccessible(true);
        return id.get(entity);
    }
}