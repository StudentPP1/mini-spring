package org.spring.hibernate.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.entity.*;
import org.spring.hibernate.interceptor.LazyCollection;
import org.spring.hibernate.interceptor.LazyList;
import org.spring.hibernate.interceptor.LazySet;
import org.spring.hibernate.session.InternalSession;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class ResultSetParser {
    private static final Logger log = LogManager.getLogger(ResultSetParser.class);

    private ResultSetParser() {
    }

    public static <T> T parseEntity(ResultSet resultSet, EntityMetadata metadata, Class<T> entityClass, String prefix, InternalSession session) {
        try {
            log.trace("parse entity: {} from result set", entityClass.getSimpleName());
            T instance = entityClass.getDeclaredConstructor().newInstance();
            for (SimpleField simpleField : metadata.simpleFields()) {
                String column = prefix + simpleField.name();
                Field field = simpleField.field();
                Object value = resultSet.getObject(column);
                log.debug("set value: {} to field: {}", value, field.getName());
                field.setAccessible(true);
                field.set(instance, value);
            }
            for (RelationField relationField : metadata.findLazyFields()) {
                Field field = relationField.field();
                field.setAccessible(true);
                Class<?> fieldType = field.getType();
                Class<?> childClass = EntityHelper.getEntityClass(field);
                if (Set.class.isAssignableFrom(fieldType)) {
                    field.set(instance, new LazySet<>(session, childClass, instance, metadata));
                } else if (List.class.isAssignableFrom(fieldType)) {
                    field.set(instance, new LazyList<>(session, childClass, instance, metadata));
                } else if (Collection.class.isAssignableFrom(fieldType)) {
                    field.set(instance, new LazyCollection<>(session, new ArrayList<>(), childClass, instance, metadata));
                } else {
                    throw new RuntimeException("Unsupported collection type for LAZY relation: " + fieldType.getName());
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> void parseJoinedCollections(ResultSet resultSet, EntityMetadata metadata, T rootEntity, Map<Class<?>, EntityMetadata> entities, InternalSession session) throws IllegalAccessException, SQLException {
        Optional<RelationField> collectionEntityField = metadata.findEagerCollection();
        if (collectionEntityField.isEmpty()) return;
        RelationField relationField = collectionEntityField.get();
        Field collectionField = relationField.field();
        collectionField.setAccessible(true);
        Class<?> childType = EntityHelper.getEntityClass(collectionField);
        EntityMetadata childMetadata = entities.get(childType);
        if (resultSet.getObject("t1_" + childMetadata.idColumn()) == null) return;
        Object parsedChild = parseEntity(resultSet, childMetadata, childType, "t1_", session);
        linkChildToRoot(rootEntity, relationField, childMetadata, childType, parsedChild);
        Collection<Object> collection = (Collection<Object>) collectionField.get(rootEntity);
        if (collection == null) {
            Class<?> fieldType = collectionField.getType();
            if (Set.class.isAssignableFrom(fieldType)) {
                collection = new HashSet<>();
            } else if (List.class.isAssignableFrom(fieldType) || Collection.class.isAssignableFrom(fieldType)) {
                collection = new ArrayList<>();
            } else {
                // TODO: Map/Queue (complicated)
                throw new RuntimeException("Unsupported collection type for mappedBy relation: " + fieldType.getName());
            }
            collectionField.set(rootEntity, collection);
        }
        if (!collection.contains(parsedChild)) {
            collection.add(parsedChild);
        }
    }

    private static <T> void linkChildToRoot(T rootEntity, RelationField relationField, EntityMetadata childMetadata, Class<?> childType, Object parsedChild) throws IllegalAccessException {
        String mappedBy = relationField.mappedBy();
        if (mappedBy != null && !mappedBy.isEmpty()) {
            RelationField entityField = childMetadata.relationFields().stream()
                    .filter(f -> f.name().equals(mappedBy))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("MappedBy field '" + mappedBy + "' not found in " + childType.getSimpleName()));
            Field mappedByField = entityField.field();
            mappedByField.setAccessible(true);
            mappedByField.set(parsedChild, rootEntity);
        }
    }
}
