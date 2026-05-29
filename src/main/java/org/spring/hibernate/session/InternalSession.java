package org.spring.hibernate.session;

import org.spring.hibernate.entity.EntityMetadata;
import java.sql.Connection;
import java.util.Map;

public interface InternalSession extends Session {
    PersistenceContext getPersistenceContext();
    Map<Class<?>, EntityMetadata> getEntities();
    Connection getConnection();
    <T> EntityMetadata getEntityMetadata(Class<T> entityClass);
    Object getIdValue(Object entity, EntityMetadata m) throws NoSuchFieldException, IllegalAccessException;
}
