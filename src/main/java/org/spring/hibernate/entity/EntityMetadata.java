package org.spring.hibernate.entity;

import org.spring.hibernate.annotation.FetchType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Class with all data about entity
 */
public record EntityMetadata(
        String tableName,
        String idField,
        String idColumn,
        List<RelationField> relationFields,
        List<SimpleField> simpleFields
) {
    public List<EntityField> fields() {
        List<EntityField> entityFields = new ArrayList<>();
        entityFields.addAll(relationFields);
        entityFields.addAll(simpleFields);
        return entityFields;
    }

    public Optional<String> findForeignKeyBy(Object entity) {
        return relationFields.stream().filter(entityField ->
                entityField.isRelationOwner() && entityField.field()
                        .getType()
                        .equals(entity.getClass())
        ).map(RelationField::foreignKey).findAny();
    }

    public Optional<RelationField> findEagerCollection() {
        return relationFields.stream().filter(entityField ->
                entityField.fetchType().equals(FetchType.EAGER) &&
                        !entityField.isRelationOwner() &&
                        Collection.class.isAssignableFrom(entityField.field().getType())
        ).findAny();
    }

    public List<RelationField> findEagerSingleFields() {
        return relationFields.stream().filter(entityField ->
                entityField.fetchType().equals(FetchType.EAGER) &&
                        entityField.isRelationOwner()
        ).toList();
    }

    public List<RelationField> findLazyFields() {
        return relationFields.stream().filter(e -> e.fetchType().equals(FetchType.LAZY)).toList();
    }

    public List<RelationField> getAllRelatedCollections() {
        return relationFields.stream()
                .filter(e -> !e.isRelationOwner())
                .toList();
    }
}
