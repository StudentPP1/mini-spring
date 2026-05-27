package org.spring.hibernate.interceptor;

import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.query.SqlBuilder;
import org.spring.hibernate.session.InternalSession;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class LazyCollection<T> implements Collection<T> {
    private final InternalSession session;
    private boolean isInitialized = false;
    protected final Collection<T> collection;
    private final Class<?> childClass;
    private final Object parent;
    private final EntityMetadata parentMetadata;

    public LazyCollection(InternalSession session, Collection<T> collection, Class<?> childClass, Object parent, EntityMetadata parentMetadata) {
        this.session = session;
        this.collection = collection;
        this.childClass = childClass;
        this.parent = parent;
        this.parentMetadata = parentMetadata;
    }


    @Override
    public int size() {
        initialize();
        return collection.size();
    }

    @Override
    public boolean isEmpty() {
        initialize();
        return collection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        initialize();
        return collection.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        initialize();
        return collection.iterator();
    }

    @Override
    public Object[] toArray() {
        initialize();
        return collection.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        initialize();
        return collection.toArray(a);
    }

    @Override
    public boolean add(T t) {
        initialize();
        return collection.add(t);
    }

    @Override
    public boolean remove(Object o) {
        initialize();
        return collection.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        initialize();
        return collection.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        initialize();
        return collection.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        initialize();
        return collection.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        initialize();
        return collection.retainAll(c);
    }

    @Override
    public void clear() {
        initialize();
        collection.clear();
    }

    protected void initialize() {
        if (!isInitialized) {
            try {
                EntityMetadata childMeta = session.getEntityMetadata(childClass);
                String foreignKey = childMeta.findForeignKeyByEntity(parent.getClass())
                        .orElseThrow(() -> new IllegalStateException("foreignKey to " + parent.getClass().getSimpleName() + " in " + childClass.getSimpleName() + " not found"));
                String sql = SqlBuilder.selectByColumn(childMeta.tableName(), foreignKey);
                Object parentId = session.getIdValue(parent, parentMetadata);
                List<T> children = session.createQuery(sql, (Class<T>) childClass)
                        .setParameter(1, parentId)
                        .list();
                this.collection.addAll(children);
                isInitialized = true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize lazy collection", e);
            }
        }
    }

}
