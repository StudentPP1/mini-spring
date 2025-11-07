package org.spring.hibernate.session;

import org.spring.hibernate.connection.ConnectionProvider;
import org.spring.hibernate.entity.EntityKey;
import org.spring.hibernate.entity.EntityMetadata;
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
        EntityMetadata metadata = getEntityMetadata(entityClass);
        EntityKey key = new EntityKey(entityClass, id);
        if (cache.containsKey(key)) return (T) cache.get(key);
        try (PreparedStatement statement = getConnection().prepareStatement(SqlBuilder.selectById(metadata))) {
            statement.setObject(1, id);
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
        EntityMetadata metadata = getEntityMetadata(entity.getClass());
        try (var statement = getConnection().prepareStatement(SqlBuilder.insert(metadata), RETURN_GENERATED_KEYS)) {
            fillStatement(entity, metadata, statement);
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    Field idField = entity.getClass().getDeclaredField(metadata.idField());
                    idField.setAccessible(true);
                    idField.set(entity, keys.getObject(1));
                }
            }
            Object valueId = getIdValue(entity, metadata);
            cache.put(new EntityKey(entity.getClass(), valueId), entity);
        } catch (Exception e) {
            throw new RuntimeException("persist failed", e);
        }
    }

    @Override
    public void merge(Object entity) {
        EntityMetadata metadata = getEntityMetadata(entity.getClass());
        try (PreparedStatement statement = getConnection().prepareStatement(SqlBuilder.updateById(metadata))) {
            int i = fillStatement(entity, metadata, statement);
            var idField = entity.getClass().getDeclaredField(metadata.idField());
            idField.setAccessible(true);
            Object idValue = idField.get(entity);
            statement.setObject(i, idValue);
            int updated = statement.executeUpdate();
            if (updated == 0) throw new RuntimeException("merge: no rows updated");
            EntityKey key = new EntityKey(entity.getClass(), idValue);
            cache.put(key, entity);
        } catch (Exception e) {
            throw new RuntimeException("merge failed for " + entity.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void remove(Object entity) {
        EntityMetadata metadata = getEntityMetadata(entity.getClass());
        try (PreparedStatement ps = getConnection().prepareStatement(SqlBuilder.deleteById(metadata))) {
            Object idValue = getIdValue(entity, metadata);
            ps.setObject(1, idValue);
            ps.executeUpdate();
            cache.remove(new EntityKey(entity.getClass(), idValue));
        } catch (Exception e) {
            throw new RuntimeException("Remove failed for " + entity.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void flush() {
        // TODO: implement dirty checking
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (!transactionManager.isActive()) {
            try { connectionProvider.release(true); } catch (Exception _) {}
        }
        cache.clear();
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
        }
        return i;
    }

    private <T> EntityMetadata getEntityMetadata(Class<T> entityClass) {
        EntityMetadata metadata = entities.get(entityClass);
        if (metadata == null) throw new RuntimeException("Entity not registered: " + entityClass);
        return metadata;
    }
}
