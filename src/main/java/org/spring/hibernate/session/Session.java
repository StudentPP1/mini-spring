package org.spring.hibernate.session;

import org.spring.hibernate.query.Query;

import java.sql.Connection;

public interface Session {
    <T> Query<T> createQuery(String jpql, Class<T> resultType);

    <T, R> T find(Class<T> enitityClass, R id);

    void persist(Object entity);

    void merge(Object entity);

    void remove(Object entity);

    Connection getConnection();

    void flush();

    void close();
}
