package org.spring.hibernate.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.entity.EntityField;
import org.spring.hibernate.entity.EntityHelper;
import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.entity.RelationField;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class ResultSetParser {
    private static final Logger log = LogManager.getLogger(ResultSetParser.class);

    private ResultSetParser() {
    }

    public static <T> T parseEntity(ResultSet resultSet, EntityMetadata metadata, Class<T> entityClass, String prefix) {
        try {
            log.trace("parse entity: {} from result set", entityClass.getSimpleName());
            T instance = entityClass.getDeclaredConstructor().newInstance();
            List<EntityField> entityFields = metadata.fields().stream()
                    .filter(entityField -> EntityHelper.getPhysicalColumnName(entityField).isPresent())
                    .toList();
            for (EntityField entityField : entityFields) {
                String column = prefix + entityField.name();
                Field field = entityField.field();
                Object value = resultSet.getObject(column);
                log.debug("set value: {} to field: {}", value, field.getName());
                field.setAccessible(true);
                field.set(instance, value);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> void parseJoinedCollections(ResultSet resultSet, EntityMetadata metadata, T rootEntity, Map<Class<?>, EntityMetadata> entities) throws IllegalAccessException, SQLException {
        Optional<EntityField> collectionEntityField = metadata.fields().stream()
                .filter(entityField ->
                        entityField instanceof RelationField relationField &&
                                !relationField.relation().isRelationOwner())
                .findFirst();
        if (collectionEntityField.isEmpty()) {
            return;
        }
        RelationField relationField = (RelationField) collectionEntityField.get();
        Field collectionField = relationField.field();
        collectionField.setAccessible(true);
        Class<?> childType = EntityHelper.getEntityClass(collectionField);
        EntityMetadata childMetadata = entities.get(childType);
        if (resultSet.getObject("t1_" + childMetadata.idColumn()) == null) {
            return;
        }
        Object parsedChild = parseEntity(resultSet, childMetadata, childType, "t1_");
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
        String mappedBy = relationField.relation().mappedBy();
        if (mappedBy != null && !mappedBy.isEmpty()) {
            EntityField entityField = childMetadata.fields().stream()
                    .filter(f -> f.name().equals(mappedBy))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("MappedBy field '" + mappedBy + "' not found in " + childType.getSimpleName()));
            Field mappedByField = entityField.field();
            mappedByField.setAccessible(true);
            mappedByField.set(rootEntity, parsedChild);
        }
    }
}
