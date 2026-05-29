package org.spring.hibernate.entity;

import org.spring.hibernate.annotation.FetchType;


public record RelationData(
        String foreignKey,
        FetchType fetchType,
        String mappedBy,
        Boolean isRelationOwner
) {}
