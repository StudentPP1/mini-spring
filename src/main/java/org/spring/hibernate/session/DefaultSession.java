package org.spring.hibernate.session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.connection.ConnectionProvider;
import org.spring.hibernate.entity.*;
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
import java.util.*;

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
        // mappedBy -> load List<Node> by left join (if exists)
        String sql = SqlBuilder.selectByIdWithJoin(metadata, this.entities);
        log.trace("generate sql: {}", sql);
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setObject(1, id);
            log.trace("create find statement with id param");
            try (ResultSet resultSet = statement.executeQuery()) {
                T rootEntity = null;
                while (resultSet.next()) {
                    if (rootEntity == null) {
                        rootEntity = ResultSetParser.parseEntity(resultSet, metadata, entityClass, "t0_");
                        cache.put(key, rootEntity);
                        // load Person by @JoinColumn (if exists)
                        fillForeignKey(metadata, resultSet, rootEntity);
                    }
                    ResultSetParser.parseJoinedCollections(resultSet, metadata, rootEntity, entities);
                }
                return rootEntity;
            }
        } catch (Exception e) {
            throw new RuntimeException("find failed for " + entityClass.getSimpleName(), e);
        }
    }

    private void fillForeignKey(EntityMetadata parentMetadata, ResultSet resultSet, Object parent) throws SQLException, IllegalAccessException {
        for (EntityField entityField : parentMetadata.fields()) {
            if (entityField instanceof RelationField relationField && relationField.relation().isRelationOwner()) {
                Class<?> childClass = EntityHelper.getEntityClass(relationField.field());
                String fkColumn = "t0_" + relationField.relation().foreignKey();
                Object fkValue = resultSet.getObject(fkColumn);
                if (fkValue != null) {
                    log.trace("Found foreign key {} = {}. Triggering recursive find.", fkColumn, fkValue);
                    Object childEntity = find(childClass, fkValue);
                    Field field = relationField.field();
                    field.setAccessible(true);
                    field.set(parent, childEntity);
                }
            }
        }
    }

    /**
     * save entity
    */
    @Override
    public void persist(Object entity) {
        log.trace("call persist method in session");
        EntityMetadata metadata = getEntityMetadata(entity.getClass());
        log.debug("load entity metadata: {}", metadata);
        String sql = SqlBuilder.insert(metadata);
        log.trace("generate sql: {}", sql);
        try (var statement = getConnection().prepareStatement(sql, RETURN_GENERATED_KEYS)) {
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
        for (EntityField entityField : metadata.fields()) {
            if (entityField instanceof RelationField relationField && !relationField.relation().isRelationOwner()) {
                updateChildren(entity, metadata, entityField);
            }
        }
    }

    private void updateChildren(Object parent, EntityMetadata parentMetadata, EntityField entityField) {
        if (entityField instanceof RelationField relationField && !relationField.relation().isRelationOwner()) {
            try {
                Field field = relationField.field();
                field.setAccessible(true);
                Object collection = field.get(parent);
                if (!(collection instanceof Iterable<?> iterable)) return;

                List<Object> currentChildIds = new ArrayList<>();
                Class<?> childClass = EntityHelper.getEntityClass(field);
                EntityMetadata childMeta = getEntityMetadata(childClass);

                for (Object child : iterable) {
                    Object childId = getIdValue(child, childMeta);
                    if (childId == null) {
                        log.trace("Child is new. Cascading INSERT to: {}", child.getClass().getSimpleName());
                        persist(child);
                        currentChildIds.add(getIdValue(child, childMeta));
                    } else {
                        log.trace("Child already exists (ID: {}). Cascading UPDATE to: {}", childId, child.getClass().getSimpleName());
                        merge(child);
                        currentChildIds.add(childId);
                    }
                }
                Object parentId = getIdValue(parent, parentMetadata);
                deleteOrphans(parentId, currentChildIds, childMeta, relationField);
            } catch (Exception e) {
                throw new RuntimeException("Failed to update collection " + entityField.name(), e);
            }
        }
    }

    private void deleteOrphans(Object parentId, List<Object> currentChildIds, EntityMetadata childMeta, RelationField relationField) throws SQLException {
        // DELETE FROM note WHERE person_id = 5 AND id NOT IN (101, 102);
        String foreignKey = getForeignKeyField(childMeta, relationField).relation().foreignKey();
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(childMeta.tableName())
                .append(" WHERE ").append(foreignKey).append(" = ?");
        if (!currentChildIds.isEmpty()) {
            sql.append(" AND ").append(childMeta.idColumn()).append(" NOT IN (");
            for (int i = 0; i < currentChildIds.size(); i++) {
                sql.append("?");
                if (i < currentChildIds.size() - 1) sql.append(", ");
            }
            sql.append(")");
        }
        log.trace("Executing orphan removal: {}", sql);
        try (PreparedStatement statement = getConnection().prepareStatement(sql.toString())) {
            int paramentIndex = 1;
            statement.setObject(paramentIndex++, parentId);
            for (Object childId : currentChildIds) {
                statement.setObject(paramentIndex++, childId);
            }
            int deletedCount = statement.executeUpdate();
            if (deletedCount > 0) {
                log.debug("Deleted {} orphaned entities from {}", deletedCount, childMeta.tableName());
            }
        }
    }

    private static RelationField getForeignKeyField(EntityMetadata childMeta, RelationField relationField) {
        String mappedBy = relationField.relation().mappedBy();
        EntityField entityField = childMeta.fields().stream()
                .filter(field -> field.field().getName().equals(mappedBy))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("MappedBy field '" + mappedBy + "' not found!"));
        return ((RelationField) entityField);
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
        for (EntityField entityField : metadata.fields()) {
            if (entityField instanceof RelationField relationField && !relationField.relation().isRelationOwner()) {
                updateChildren(entity, metadata, entityField);
            }
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
            try {
                connectionProvider.release(true);
            } catch (Exception ignored) {
            }
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

    private Object getIdValue(Object entity, EntityMetadata m) throws NoSuchFieldException, IllegalAccessException {
        Field id = entity.getClass().getDeclaredField(m.idField());
        id.setAccessible(true);
        return id.get(entity);
    }

    private int fillStatement(Object entity, EntityMetadata metadata, PreparedStatement statement) throws SQLException, IllegalAccessException {
        int i = 1;
        for (EntityField entityField : metadata.fields()) {
            try {
                Field field = entityField.field();
                String columnName = entityField.name();
                if (columnName.equals(metadata.idColumn())) continue;
                field.setAccessible(true);
                if (entityField instanceof RelationField relationField && !relationField.relation().foreignKey().isEmpty()) {
                    Object parentEntity = field.get(entity);
                    if (parentEntity == null) {
                        // TODO: if optional=false -> throw exception
                        statement.setObject(i++, field.get(entity));
                        log.trace("Parent entity is null, set foreign key {} to null", relationField.relation().foreignKey());
                    } else {
                        EntityMetadata parentMetadata = this.entities.get(parentEntity.getClass());
                        Object parentId = getIdValue(parentEntity, parentMetadata);
                        statement.setObject(i++, parentId);
                        log.trace("Set foreignKey: {} by parent ID: {}", relationField.relation().foreignKey(), parentId);
                    }
                } else {
                    statement.setObject(i++, field.get(entity));
                    log.trace("set columnName: {} by value: {}", columnName, field.get(entity));
                }
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        return i;
    }

    public <T> EntityMetadata getEntityMetadata(Class<T> entityClass) {
        EntityMetadata metadata = entities.get(entityClass);
        if (metadata == null) throw new RuntimeException("Entity not registered: " + entityClass);
        return metadata;
    }
}
