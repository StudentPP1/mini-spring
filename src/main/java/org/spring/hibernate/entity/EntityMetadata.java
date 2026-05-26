package org.spring.hibernate.entity;

import java.util.List;

public record EntityMetadata(
        String tableName,
        String idField,
        String idColumn,
        List<EntityField> fields
) {
}
