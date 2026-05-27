package org.spring.hibernate.interceptor;

import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.session.InternalSession;

import java.util.List;
import java.util.Collection;
import java.util.ListIterator;

public class LazyList<T> extends LazyCollection<T> implements List<T> {

    public LazyList(InternalSession session, Class<?> childClass, Object parent, EntityMetadata parentMetadata) {
        super(session, new java.util.ArrayList<>(), childClass, parent, parentMetadata);
    }

    private List<T> getList() {
        return (List<T>) collection;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        initialize();
        return getList().addAll(index, c);
    }

    @Override
    public T get(int index) {
        initialize();
        return getList().get(index);
    }

    @Override
    public T set(int index, T element) {
        initialize();
        return getList().set(index, element);
    }

    @Override
    public void add(int index, T element) {
        initialize();
        getList().add(index, element);
    }

    @Override
    public T remove(int index) {
        initialize();
        return getList().remove(index);
    }

    @Override
    public int indexOf(Object o) {
        initialize();
        return getList().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        initialize();
        return getList().lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        initialize();
        return getList().listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        initialize();
        return getList().listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        initialize();
        return getList().subList(fromIndex, toIndex);
    }
}
