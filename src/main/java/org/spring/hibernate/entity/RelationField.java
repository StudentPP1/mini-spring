package org.spring.hibernate.entity;

import org.spring.hibernate.annotation.FetchType;

import java.lang.reflect.Field;

public record RelationField(String name,
                            Field field,
                            String foreignKey,
                            FetchType fetchType,
                            String mappedBy,
                            Boolean isRelationOwner
) implements EntityField {
}
