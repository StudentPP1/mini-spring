package org.spring.hibernate.session;

import org.spring.hibernate.entity.EntityKey;
import java.util.HashMap;
import java.util.Map;

public class PersistenceContext {
    private final Map<EntityKey, Object> entities = new HashMap<>();

    public void put(EntityKey key, Object entity) {
        entities.put(key, entity);
    }

    public Object get(EntityKey key) {
        return entities.get(key);
    }

    public boolean contains(EntityKey key) {
        return entities.containsKey(key);
    }

    public void remove(EntityKey key) {
        entities.remove(key);
    }

    public void clear() {
        entities.clear();
    }
}