package org.spring.hibernate.session;

import org.spring.hibernate.entity.EntityMetadata;

import java.util.Map;

public interface SessionFactory extends AutoCloseable {
    Session openSession();
    Map<Class<?>, EntityMetadata> getMetadata();
    @Override void close();
}
