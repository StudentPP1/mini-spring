package org.spring.hibernate.entity;

import java.lang.reflect.Field;

/**
 * Base interface that's represent field of entity
*/
public sealed interface EntityField permits SimpleField, RelationField {
    String name();
    Field field();
}
