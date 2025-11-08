package org.spring.hibernate.entity;

import java.lang.reflect.Field;
import java.util.Map;

// TODO: scan all entities -> create tables & metadata
public record EntityMetadata(
        String tableName,
        String idField,
        String idColumn,
        Map<String, Field> columns
) { }
