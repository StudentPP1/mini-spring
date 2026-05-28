package org.spring.hibernate.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.entity.*;
import org.spring.hibernate.interceptor.LazyCollection;
import org.spring.hibernate.interceptor.LazyList;
import org.spring.hibernate.interceptor.LazySet;
import org.spring.hibernate.session.EntityLoader;
import org.spring.hibernate.session.InternalSession;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class ResultSetParser {
    private static final Logger log = LogManager.getLogger(ResultSetParser.class);
    private final ResultSet resultSet;
    private final InternalSession session;
    private final EntityLoader loader;

    public ResultSetParser(ResultSet resultSet, InternalSession session, EntityLoader loader) {
        this.resultSet = resultSet;
        this.session = session;
        this.loader = loader;
    }

    public <T> T parseEntity(Class<T> entityClass, EntityMetadata targetMetadata, String sqlFieldsPrefix) {
        try {
            log.trace("parse entity: {} from result set", entityClass.getSimpleName());
            T instance = entityClass.getDeclaredConstructor().newInstance();
            for (SimpleField simpleField : targetMetadata.simpleFields()) {
                String column = sqlFieldsPrefix + simpleField.name();
                Field field = simpleField.field();
                Object value = resultSet.getObject(column);
                field.setAccessible(true);
                field.set(instance, value);
                log.debug("set value: {} to field: {}", value, field.getName());
            }
            for (RelationField relationField : targetMetadata.findLazyFields()) {
                Field field = relationField.field();
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                Class<?> childClass = EntityHelper.getEntityClass(field);
                if (Set.class.isAssignableFrom(fieldType)) {
                    field.set(instance, new LazySet<>(session, childClass, instance, targetMetadata));
                } else if (List.class.isAssignableFrom(fieldType)) {
                    field.set(instance, new LazyList<>(session, childClass, instance, targetMetadata));
                } else if (Collection.class.isAssignableFrom(fieldType)) {
                    field.set(instance, new LazyCollection<>(session, new ArrayList<>(), childClass, instance, targetMetadata));
                } else {
                    throw new RuntimeException("Unsupported type for LAZY relation: " + fieldType.getName());
                }
                log.debug("set lazy collection to field: {}", field.getName());
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> void parseJoinedCollections(T rootEntity, EntityMetadata rootMetadata) throws IllegalAccessException, SQLException {
        Optional<RelationField> collectionEntityField = rootMetadata.findEagerCollection();
        if (collectionEntityField.isEmpty()) {
            log.trace("{}: hasn't eager collection", rootEntity.getClass().getSimpleName());
            return;
        }
        RelationField relationField = collectionEntityField.get();
        Field collectionField = relationField.field();
        collectionField.setAccessible(true);
        Class<?> childType = EntityHelper.getEntityClass(collectionField);
        EntityMetadata childMetadata = session.getEntities().get(childType);
        String joinIdColumn = "t1_" + childMetadata.idColumn();
        if (!hasColumn(resultSet, joinIdColumn) || resultSet.getObject(joinIdColumn) == null) {
            log.trace("query hasn't join to parse: {}", relationField.name());
            return;
        }
        Object childId = resultSet.getObject(joinIdColumn);
        EntityKey childKey = new EntityKey(childType, childId);
        Object parsedChild = session.getPersistenceContext().get(childKey);
        if (parsedChild == null) {
            log.trace("parse {} for eager Collection<{}> inside root entity: {}",
                    childType.getSimpleName(),
                    childType.getSimpleName(),
                    rootEntity.getClass().getSimpleName()
            );
            parsedChild = parseEntity(childType, childMetadata, "t1_");
            session.getPersistenceContext().put(childKey, parsedChild);
            loader.fillForeignKeys(childMetadata, resultSet, parsedChild, "t1_");
        } else {
            log.trace("found {} in cache for eager Collection<{}> inside root entity: {}",
                    childType.getSimpleName(),
                    childType.getSimpleName(),
                    rootEntity.getClass().getSimpleName()
            );
        }
        linkChildToRoot(rootEntity, relationField, childMetadata, childType, parsedChild);
        Collection<Object> collection = (Collection<Object>) collectionField.get(rootEntity);
        if (collection == null) {
            Class<?> fieldType = collectionField.getType();
            if (Set.class.isAssignableFrom(fieldType)) {
                collection = new HashSet<>();
            } else if (List.class.isAssignableFrom(fieldType) || Collection.class.isAssignableFrom(fieldType)) {
                collection = new ArrayList<>();
            } else {
                throw new RuntimeException("Unsupported collection type for mappedBy relation: " + fieldType.getName());
            }
            collectionField.set(rootEntity, collection);
        }
        log.trace("parsed eager collection: {}", collection);
        if (!collection.contains(parsedChild)) {
            collection.add(parsedChild);
        }
    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private <T> void linkChildToRoot(T rootEntity, RelationField relationField, EntityMetadata childMetadata, Class<?> childType, Object parsedChild) throws IllegalAccessException {
        String mappedBy = relationField.mappedBy();
        if (mappedBy != null && !mappedBy.isEmpty()) {
            RelationField entityField = childMetadata.relationFields().stream()
                    .filter(f -> f.name().equals(mappedBy))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("MappedBy field '" + mappedBy + "' not found in " + childType.getSimpleName()));
            Field mappedByField = entityField.field();
            mappedByField.setAccessible(true);
            mappedByField.set(parsedChild, rootEntity);
            log.trace("link root entity {} to parsed child {} by mappedBy field: {}",
                    rootEntity.getClass().getSimpleName(),
                    parsedChild.getClass().getSimpleName(),
                    mappedBy
            );
        }
    }
}
