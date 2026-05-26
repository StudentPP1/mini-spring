package org.spring.hibernate.entity;

import java.lang.reflect.Field;

public sealed interface EntityField permits SimpleField, RelationField {
    String name();
    Field field();
}
