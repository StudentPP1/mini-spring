package org.spring.hibernate.entity;

import org.spring.hibernate.annotation.FetchType;

import java.lang.reflect.Field;
import java.util.List;

public record RelationField(String name,
                            Field field,
                            String foreignKey,
                            FetchType fetchType,
                            String mappedBy,
                            Boolean isRelationOwner
) implements EntityField {

    public boolean isEagerCollection() {
        return !isRelationOwner && fetchType.equals(FetchType.EAGER) && List.class.isAssignableFrom(field.getType());
    }
}
