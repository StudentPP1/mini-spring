package org.spring.hibernate.entity;

import java.lang.reflect.Field;
import java.util.Map;

public record EntityMetadata(
        String tableName,
        String idField,
        String idColumn,
        Map<String, Field> columns
) { }
