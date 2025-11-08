package org.spring.hibernate.entity;

import java.util.Objects;

public final class EntityKey {
    private final Class<?> type;
    private final Object id;

    public EntityKey(Class<?> type, Object id) {
        this.type = type;
        this.id = id;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityKey other = (EntityKey) o;
        return type.equals(other.type) && Objects.equals(id, other.id);
    }

    public int hashCode() {
        return 31 * type.hashCode() + (id != null ? id.hashCode() : 0);
    }

    @Override
    public String toString() {
        return "EntityKey{" +
                "type=" + type.getSimpleName() +
                ", id=" + id +
                '}';
    }
}
