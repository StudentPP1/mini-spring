package org.spring.hibernate.entity;

import java.lang.reflect.Field;

public record SimpleField(String name, Field field) implements EntityField {
}
