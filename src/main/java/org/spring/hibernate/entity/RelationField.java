package org.spring.hibernate.entity;

import java.lang.reflect.Field;

public record RelationField(String name,
                            Field field,
                            RelationData relation
) implements EntityField {
}
