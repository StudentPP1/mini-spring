package org.spring.hibernate.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.entity.EntityMetadata;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Map;

public final class ResultSetParser {
    private static final Logger log = LogManager.getLogger(ResultSetParser.class);

    private ResultSetParser() {}
    public static <T> T parseEntity(ResultSet resultSet, EntityMetadata metadata, Class<T> enitityClass) {
        try {
            log.trace("parse entity: {} from result set", enitityClass.getSimpleName());
            T instance = enitityClass.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Field> fieldMap : metadata.columns().entrySet()) {
                String column = fieldMap.getKey();
                Field field = fieldMap.getValue();
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
}
