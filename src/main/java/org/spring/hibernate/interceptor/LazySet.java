package org.spring.hibernate.interceptor;

import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.session.InternalSession;

import java.util.HashSet;
import java.util.Set;

public class LazySet<T> extends LazyCollection<T> implements Set<T> {
    public LazySet(InternalSession session, Class<?> childClass, Object parent, EntityMetadata parentMetadata) {
        super(session, new HashSet<>(), childClass, parent, parentMetadata);
    }
}
