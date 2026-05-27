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
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
        String sql = SqlBuilder.selectByIdWithJoin(metadata, session.getEntities());
        log.trace("generate sql: {}", sql);
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            statement.setObject(1, id);
            log.trace("create find statement with id param");
            try (ResultSet resultSet = statement.executeQuery()) {
                T rootEntity = null;
                while (resultSet.next()) {
                    if (rootEntity == null) {
                        rootEntity = ResultSetParser.parseEntity(resultSet, metadata, entityClass, "t0_", session);
                        cache.put(key, rootEntity);
                        // load Person by @JoinColumn (if exists)
                        fillForeignKeys(metadata, resultSet, rootEntity, "t0_");
                    }
                    ResultSetParser.parseJoinedCollections(resultSet, metadata, rootEntity, session.getEntities(), session);
                }
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
        if (optionalRelationField.isEmpty()) return;
        RelationField relationField = optionalRelationField.get();
        Field field = relationField.field();
        field.setAccessible(true);
        Collection<?> children = (Collection<?>) field.get(entity);
        // load EAGER collection
        // in each children collection element (Note)
        // inside parent entity (Person -> List<Note>)
        if (children == null || children.isEmpty()) {
            Class<?> childClass = EntityHelper.getEntityClass(relationField.field());
            EntityMetadata childMeta = session.getEntityMetadata(childClass);
            String foreignKey = relationField.mappedBy();
            // SELECT * FROM subnotes WHERE note_id = ?;
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
        // if subnote have eager collection
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
                log.trace("Found foreign key {} = {}. Triggering recursive find.", fkColumn, fkValue);
                Object childEntity = load(childClass, fkValue);
                Field field = relationField.field();
                field.setAccessible(true);
                field.set(parent, childEntity);
            }
        }
    }
}