package org.spring.hibernate.session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.entity.*;
import org.spring.hibernate.query.SqlBuilder;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class EntityPersister {
    private static final Logger log = LogManager.getLogger(EntityPersister.class);
    private final InternalSession session;
    private final PersistenceContext cache;

    public EntityPersister(InternalSession session) {
        this.session = session;
        this.cache = this.session.getPersistenceContext();
    }

    /**
     * save entity
     */
    public void persist(Object entity) {
        log.trace("call persist method in session");
        EntityMetadata metadata = session.getEntityMetadata(entity.getClass());
        log.debug("load entity metadata: {}", metadata);
        String sql = SqlBuilder.insert(metadata);
        log.trace("generate sql: {}", sql);
        try (var statement = session.getConnection().prepareStatement(sql, RETURN_GENERATED_KEYS)) {
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
            Object valueId = session.getIdValue(entity, metadata);
            log.trace("save to cache");
            cache.put(new EntityKey(entity.getClass(), valueId), entity);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("persist failed", e);
        }
        metadata.getAllRelatedCollections()
                .forEach(relationField ->
                        saveOrUpdateChild(entity, metadata, relationField));
    }

    /**
     * update entity
     */
    public void merge(Object entity) {
        log.trace("call merge method in session");
        EntityMetadata metadata = session.getEntityMetadata(entity.getClass());
        log.debug("load entity metadata: {}", metadata);
        try (PreparedStatement statement = session.getConnection().prepareStatement(SqlBuilder.updateById(metadata))) {
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
        metadata.getAllRelatedCollections()
                .forEach(relationField ->
                        saveOrUpdateChild(entity, metadata, relationField));
    }

    /**
     * remove entity
     */
    public void remove(Object entity) {
        log.trace("call remove method in session");
        EntityMetadata metadata = session.getEntityMetadata(entity.getClass());
        log.debug("load entity metadata: {}", metadata);
        try (PreparedStatement ps = session.getConnection().prepareStatement(SqlBuilder.deleteById(metadata))) {
            Object idValue = session.getIdValue(entity, metadata);
            ps.setObject(1, idValue);
            ps.executeUpdate();
            cache.remove(new EntityKey(entity.getClass(), idValue));
            log.debug("remove from cache");
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("Remove failed for " + entity.getClass().getSimpleName(), e);
        }
    }

    private void saveOrUpdateChild(Object parent, EntityMetadata parentMetadata, RelationField relationField) {
        try {
            Field field = relationField.field();
            field.setAccessible(true);
            Object collection = field.get(parent);
            log.trace("{}: have collection: {}", parent.getClass().getSimpleName(), collection.toString());
            if (!(collection instanceof Iterable<?> iterable)) {
                log.warn("collection isn't iterable");
                return;
            }
            List<Object> currentChildIds = new ArrayList<>();
            Class<?> childClass = EntityHelper.getEntityClass(field);
            EntityMetadata childMeta = session.getEntityMetadata(childClass);
            log.trace("get metadata of child: {}", childMeta);
            for (Object child : iterable) {
                Object childId = session.getIdValue(child, childMeta);
                log.trace("{}: child id = {}", child.getClass().getSimpleName(), childId);
                if (childId == null) {
                    log.trace("{}: is new. Insert row to database", child.getClass().getSimpleName());
                    persist(child);
                    currentChildIds.add(session.getIdValue(child, childMeta));
                } else {
                    log.trace("{}: is already exists. Update row in database", child.getClass().getSimpleName());
                    merge(child);
                    currentChildIds.add(childId);
                }
            }
            deleteOtherChildren(parent, parentMetadata, currentChildIds, childMeta);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update collection " + relationField.name(), e);
        }
    }

    private void deleteOtherChildren(Object parent, EntityMetadata parentMetadata, List<Object> currentChildIds, EntityMetadata childMeta) throws SQLException, NoSuchFieldException, IllegalAccessException {
        String foreignKey = childMeta.findForeignKeyBy(parent.getClass())
                .orElseThrow(() -> new RuntimeException(parent.getClass().getSimpleName() + " hasn't mappedBy field!"));
        StringBuilder sql = new StringBuilder("DELETE FROM ")
                .append(childMeta.tableName())
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
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql.toString())) {
            int paramentIndex = 1;
            statement.setObject(paramentIndex++, session.getIdValue(parent, parentMetadata));
            for (Object childId : currentChildIds) {
                statement.setObject(paramentIndex++, childId);
            }
            int deletedCount = statement.executeUpdate();
            if (deletedCount > 0) {
                log.debug("Deleted {} orphaned entities from {}", deletedCount, childMeta.tableName());
            }
        }
    }

    private int fillStatement(Object entity, EntityMetadata metadata, PreparedStatement statement) throws SQLException, IllegalAccessException {
        int i = 1;
        for (EntityField entityField : metadata.fields().stream().filter(EntityHelper::getPhysicalEntityField).toList()) {
            try {
                Field field = entityField.field();
                String columnName = entityField.name();
                if (columnName.equals(metadata.idColumn())) continue;
                field.setAccessible(true);
                if (entityField instanceof RelationField relationField && relationField.isRelationOwner()) {
                    Object parentEntity = field.get(entity);
                    if (parentEntity == null) {
                        // TODO: if optional = false -> throw exception
                        statement.setObject(i++, null);
                        log.trace("parent entity is null, set foreign key {} to null", relationField.foreignKey());
                    } else {
                        EntityMetadata parentMetadata = session.getEntityMetadata(parentEntity.getClass());
                        Object parentId = session.getIdValue(parentEntity, parentMetadata);
                        statement.setObject(i++, parentId);
                        log.trace("set foreign key: {} by parent id: {}", relationField.foreignKey(), parentId);
                    }
                } else {
                    statement.setObject(i++, field.get(entity));
                    log.trace("set column name: {} by value: {}", columnName, field.get(entity));
                }
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        return i;
    }
}