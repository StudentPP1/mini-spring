package org.spring.hibernate.session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.entity.EntityHelper;
import org.spring.hibernate.entity.EntityKey;
import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.entity.RelationField;
import org.spring.hibernate.query.ResultSetParser;
import org.spring.hibernate.query.SqlBuilder;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class EntityLoader {
    private static final Logger log = LogManager.getLogger(EntityLoader.class);
    private final InternalSession session;

    public EntityLoader(InternalSession session) {
        this.session = session;
    }

    /**
     * find one entity
     */
    public <T, R> T load(Class<T> entityClass, R id) {
        log.trace("call find method in session");
        PersistenceContext cache = session.getPersistenceContext();
        EntityMetadata metadata = session.getEntityMetadata(entityClass);
        log.debug("load entity metadata: {}", metadata);
        EntityKey key = new EntityKey(entityClass, id);
        if (cache.contains(key)) {
            log.debug("get entity from cache");
            return (T) cache.get(key);
        }
        // mappedBy -> load List<Node> by left join (if exists)
        String sql = SqlBuilder.selectByIdWithJoin(entityClass, metadata, session.getEntities());
        log.trace("generate sql: {}", sql);
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            statement.setObject(1, id);
            log.trace("create find statement with id param");
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetParser parser = new ResultSetParser(resultSet, session, this);
                T rootEntity = null;
                while (resultSet.next()) {
                    if (rootEntity == null) {
                        log.trace("{}: parse simple fields in ResultSet", entityClass.getSimpleName());
                        rootEntity = parser.parseEntity(entityClass, metadata,"t0_");
                        cache.put(key, rootEntity);
                        log.trace("{}: load single EAGER fields if exists", entityClass.getSimpleName());
                        fillForeignKeys(metadata, resultSet, rootEntity, "t0_");
                    }
                    log.trace("{}: parse EAGER collection in ResultSet if exists", entityClass.getSimpleName());
                    parser.parseJoinedCollections(rootEntity, metadata);
                }
                log.trace("{}: check having not parsed EAGER collection", entityClass.getSimpleName());
                if (rootEntity != null) {
                    resolveDeepEagerCollections(rootEntity, metadata);
                }
                return rootEntity;
            }
        } catch (Exception e) {
            throw new RuntimeException("find failed for " + entityClass.getSimpleName(), e);
        }
    }

    public void resolveDeepEagerCollections(Object entity, EntityMetadata metadata) throws IllegalAccessException, NoSuchFieldException {
        Optional<RelationField> optionalRelationField = metadata.findEagerCollection();
        if (optionalRelationField.isEmpty()) {
            log.trace("{}: hasn't eager collection", entity.getClass().getSimpleName());
            return;
        }
        RelationField relationField = optionalRelationField.get();
        Field field = relationField.field();
        field.setAccessible(true);
        Collection<?> children = (Collection<?>) field.get(entity);
        if (children == null || children.isEmpty()) {
            Class<?> childClass = EntityHelper.getEntityClass(relationField.field());
            EntityMetadata childMeta = session.getEntityMetadata(childClass);
            String foreignKey = childMeta.findForeignKeyBy(entity.getClass())
                    .orElseThrow(() -> new IllegalStateException("foreignKey to " + entity.getClass().getSimpleName() + " in " + childClass.getSimpleName() + " not found"));
            log.trace("{}: has not filled eager Collection<{}>", entity.getClass().getSimpleName(), childClass.getSimpleName());
            String sql = SqlBuilder.selectByColumn(childMeta.tableName(), foreignKey);
            Object parentId = session.getIdValue(entity, metadata);
            Collection<Object> fetched = (Collection<Object>) session.createQuery(sql, childClass)
                    .setParameter(1, parentId)
                    .list();
            Collection<Object> finalCollection = Set.class.isAssignableFrom(field.getType())
                    ? new HashSet<>(fetched) : fetched;
            field.set(entity, finalCollection);
            children = finalCollection;
        }
        if (children != null) {
            for (Object child : children) {
                EntityMetadata childMeta = session.getEntityMetadata(child.getClass());
                resolveDeepEagerCollections(child, childMeta);
            }
        }
    }

    public void fillForeignKeys(EntityMetadata parentMetadata, ResultSet resultSet, Object parent, String prefix) throws SQLException, IllegalAccessException {
        for (RelationField relationField : parentMetadata.findEagerSingleFields()) {
            Class<?> childClass = EntityHelper.getEntityClass(relationField.field());
            String fkColumn = prefix + relationField.foreignKey();
            Object fkValue = resultSet.getObject(fkColumn);
            if (fkValue != null) {
                log.trace("{}: found foreign key {} = {}: find -> {}",
                        parent.getClass().getSimpleName(),
                        fkColumn,
                        fkValue,
                        childClass.getSimpleName()
                );
                Object childEntity = load(childClass, fkValue);
                Field field = relationField.field();
                field.setAccessible(true);
                field.set(parent, childEntity);
            }
        }
    }
}