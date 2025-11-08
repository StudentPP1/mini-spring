package org.spring.hibernate.session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.connection.ConnectionProvider;
import org.spring.hibernate.entity.EntityKey;
import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.query.Query;
import org.spring.hibernate.query.ResultSetParser;
import org.spring.hibernate.query.SimpleQuery;
import org.spring.hibernate.query.SqlBuilder;
import org.spring.hibernate.transaction.TransactionManager;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class DefaultSession implements Session {
    private static final Logger log = LogManager.getLogger(DefaultSession.class);
    private final ConnectionProvider connectionProvider;
    private final TransactionManager transactionManager;
    private final Map<Class<?>, EntityMetadata> entities;
    private final Map<EntityKey, Object> cache = new HashMap<>();
    private boolean closed = false;

    public DefaultSession(
            ConnectionProvider connectionProvider,
            TransactionManager transactionManager,
            Map<Class<?>, EntityMetadata> entities
    ) {
        this.connectionProvider = connectionProvider;
        this.transactionManager = transactionManager;
        this.entities = entities;
    }

    @Override
    public <T, R> T find(Class<T> entityClass, R id) {
        log.trace("call find method in session");
        EntityMetadata metadata = getEntityMetadata(entityClass);
        log.debug("load entity metadata: {}", metadata);
        EntityKey key = new EntityKey(entityClass, id);
        if (cache.containsKey(key)) {
            log.debug("get entity from cache");
            return (T) cache.get(key);
        }
        try (PreparedStatement statement = getConnection().prepareStatement(SqlBuilder.selectById(metadata))) {
            statement.setObject(1, id);
            log.trace("create find statement with id param");
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    T entity = ResultSetParser.parseEntity(resultSet, metadata, entityClass);
                    cache.put(key, entity);
                    return entity;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("find failed for " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public void persist(Object entity) {
        log.trace("call persist method in session");
        EntityMetadata metadata = getEntityMetadata(entity.getClass());
        log.debug("load entity metadata: {}", metadata);
        try (var statement = getConnection().prepareStatement(SqlBuilder.insert(metadata), RETURN_GENERATED_KEYS)) {
            log.trace("fill create statement");
            fillStatement(entity, metadata, statement);
            statement.executeUpdate();
            log.trace("statement executed update");
            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Field idField = entity.getClass().getDeclaredField(metadata.idField());
                    idField.setAccessible(true);
                    idField.set(entity, keys.getObject(1));
                    log.debug("set id field: {}", keys.getObject(1));
                }
            }
            Object valueId = getIdValue(entity, metadata);
            log.trace("save to cache");
            cache.put(new EntityKey(entity.getClass(), valueId), entity);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("persist failed", e);
        }
    }

    @Override
    public void merge(Object entity) {
        log.trace("call merge method in session");
        EntityMetadata metadata = getEntityMetadata(entity.getClass());
        log.debug("load entity metadata: {}", metadata);
        try (PreparedStatement statement = getConnection().prepareStatement(SqlBuilder.updateById(metadata))) {
            int i = fillStatement(entity, metadata, statement);
            var idField = entity.getClass().getDeclaredField(metadata.idField());
            idField.setAccessible(true);
            Object idValue = idField.get(entity);
            statement.setObject(i, idValue);
            log.trace("filled statement with field values");
            int updated = statement.executeUpdate();
            if (updated == 0) throw new RuntimeException("merge: no rows updated");
            EntityKey key = new EntityKey(entity.getClass(), idValue);
            cache.put(key, entity);
            log.trace("update cache");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("merge failed for " + entity.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void remove(Object entity) {
        log.trace("call remove method in session");
        EntityMetadata metadata = getEntityMetadata(entity.getClass());
        log.debug("load entity metadata: {}", metadata);
        try (PreparedStatement ps = getConnection().prepareStatement(SqlBuilder.deleteById(metadata))) {
            Object idValue = getIdValue(entity, metadata);
            ps.setObject(1, idValue);
            ps.executeUpdate();
            cache.remove(new EntityKey(entity.getClass(), idValue));
            log.debug("remove from cache");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("Remove failed for " + entity.getClass().getSimpleName(), e);
        }
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
            try { connectionProvider.release(true); } catch (Exception _) {}
        }
        cache.clear();
    }

    @Override
    public <T> Query<T> createQuery(String jpql, Class<T> resultType) {
        if (jpql.toUpperCase().startsWith("FROM")) {
            EntityMetadata metadata = getEntityMetadata(resultType);
            String sql = "SELECT * FROM " + metadata.tableName();
            return new SimpleQuery<>(this, sql, resultType);
        }
        throw new UnsupportedOperationException("Only simple 'FROM Entity' queries are supported");
    }

    @Override
    public Connection getConnection() {
        return this.connectionProvider.get();
    }

    private static Object getIdValue(Object entity, EntityMetadata m) throws NoSuchFieldException, IllegalAccessException {
        Field id = entity.getClass().getDeclaredField(m.idField());
        id.setAccessible(true);
        return id.get(entity);
    }

    private static int fillStatement(Object entity, EntityMetadata metadata, PreparedStatement statement) throws SQLException, IllegalAccessException {
        int i = 1;
        for (Map.Entry<String, Field> entry : metadata.columns().entrySet()) {
            String columnName = entry.getKey();
            Field field = entry.getValue();
            if (columnName.equals(metadata.idColumn())) continue;
            field.setAccessible(true);
            statement.setObject(i++, field.get(entity));
            log.trace("set columnName: {} by value: {}", columnName, field.get(entity));
        }
        return i;
    }

    public <T> EntityMetadata getEntityMetadata(Class<T> entityClass) {
        EntityMetadata metadata = entities.get(entityClass);
        if (metadata == null) throw new RuntimeException("Entity not registered: " + entityClass);
        return metadata;
    }
}
