package org.spring.hibernate.query;

import java.util.List;

/**
 * Simple abstraction for executing SQL/JPQL-like queries.
 * Minimal version of Hibernate's Query API.
 */
public interface Query<T> {

    /**
     * Executes the query and returns the full result list.
     */
    List<T> list();

    /**
     * Executes the query and returns a single result (or throws if not unique).
     */
    T singleResult();
}