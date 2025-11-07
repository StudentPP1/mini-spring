package org.spring.hibernate.session;

import org.spring.hibernate.entity.EntityMetadata;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Map;

public final class ResultSetParser {
    private ResultSetParser() {}
    public static <T> T parseEntity(ResultSet resultSet, EntityMetadata metadata, Class<T> enitityClass) {
        try {
            T instance = enitityClass.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Field> fieldMap : metadata.columns().entrySet()) {
                String column = fieldMap.getKey();
                Field field = fieldMap.getValue();
                Object value = resultSet.getObject(column);
                field.setAccessible(true);
                field.set(instance, value);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
