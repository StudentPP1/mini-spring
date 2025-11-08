package org.spring.hibernate.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface Repository<E extends Serializable, K extends Serializable> {
    E save(E entity);

    void delete(K id);

    void update(E entity);

    Optional<E> findById(K id);

    List<E> findAll();
}
