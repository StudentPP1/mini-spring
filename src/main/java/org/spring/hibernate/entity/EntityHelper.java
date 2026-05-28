package org.spring.hibernate.entity;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;

/**
 * Helper class with methods using in finding eager relation & parsing result sets
 */
@UtilityClass
public class EntityHelper {
    public static Class<?> getEntityClass(Field field) {
        Class<?> fieldType = field.getType();
        if (Collection.class.isAssignableFrom(fieldType)) {
            Type genericType = field.getGenericType();
            // check if type is generic -> List<Note>, not just List
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] generics = parameterizedType.getActualTypeArguments();
                if (generics.length > 0 && generics[0] instanceof Class) {
                    return generics[0].getClass();
                }
            }
            throw new IllegalArgumentException("Collection field '" + field.getName() + "' must be with generic type");
        }
        return fieldType;
    }

    public static Optional<String> getPhysicalColumnName(EntityField field) {
        if (field instanceof SimpleField simpleField) {
            return Optional.of(simpleField.name());
        } // ignore mappedBy
        else if (field instanceof RelationField relationField && relationField.isRelationOwner()) {
            return Optional.of(relationField.name());
        }
        return Optional.empty();
    }
}
