package org.spring.hibernate.repository;

import lombok.Getter;
import org.spring.hibernate.session.Session;

import java.io.Serializable;
import java.util.Optional;

public abstract class BaseRepository<K extends Serializable, E extends Serializable>
        implements Repository<K, E> {

    private final Class<E> entityClass;

    @Getter
    private final Session session;

    protected BaseRepository(Class<E> entityClass, Session session) {
        this.entityClass = entityClass;
        this.session = session;
    }

    @Override
    public E save(E entity) {
        session.persist(entity);
        return entity;
    }

    @Override
    public void update(E entity) {
        session.merge(entity);
    }

    @Override
    public void delete(K id) {
        E entity = session.find(entityClass, id);
        if (entity != null) session.remove(entity);
    }

    @Override
    public Optional<E> findById(K id) {
        return Optional.ofNullable(session.find(entityClass, id));
    }
}
