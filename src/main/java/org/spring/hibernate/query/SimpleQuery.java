package org.spring.hibernate.query;

import org.spring.hibernate.entity.EntityKey;
import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.session.EntityLoader;
import org.spring.hibernate.session.InternalSession;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class SimpleQuery<T> implements Query<T> {
    private InternalSession session;
    private String sql;
    private Class<T> resultType;
    private EntityMetadata metadata;
    private final Map<Integer, Object> params = new HashMap<>();
    private final EntityLoader loader;

    public SimpleQuery(Class<T> resultType, String sql, InternalSession session, EntityMetadata metadata) {
        this.resultType = resultType;
        this.sql = sql;
        this.session = session;
        this.metadata = metadata;
        this.loader = new EntityLoader(session);
    }

    @Override
    public SimpleQuery<T> setParameter(Integer position, Object value) {
        this.params.put(position, value);
        return this;
    }
    // TODO: add logging & java doc to all EAGER/LAZY logic -> then testing
    @Override
    public List<T> list() {
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                statement.setObject(entry.getKey(), entry.getValue());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<Object, T> resultRows = new LinkedHashMap<>();
                while (resultSet.next()) {
                    String idColumnName = metadata.idColumn();
                    Object id = resultSet.getObject(idColumnName);
                    EntityKey key = new EntityKey(resultType, id);
                    T entity = resultRows.get(id);
                    if (entity == null) {
                        entity = (T) session.getPersistenceContext().get(key);
                        if (entity == null) {
                            // parse simple fields
                            entity = ResultSetParser.parseEntity(resultSet, metadata, resultType, "", session);
                            session.getPersistenceContext().put(key, entity);
                            // add single EAGER fields
                            loader.fillForeignKeys(metadata, resultSet, entity, "");
                        }
                        resultRows.put(id, entity);
                    }
                    // if was EAGER collection (join) -> parsed related entities
                    ResultSetParser.parseJoinedCollections(resultSet, metadata, entity, session.getEntities(), session);
                }
                // if inside EAGER collection (each child) was another EAGER collection
                List<T> finalResults = new ArrayList<>(resultRows.values());
                for (T entity : finalResults) {
                    loader.resolveDeepEagerCollections(entity, metadata);
                }
                return finalResults;
            }
        } catch (Exception e) {
            throw new RuntimeException("Query execution failed: " + sql, e);
        }
    }

    @Override
    public T singleResult() {
        List<T> list = list();
        if (list.isEmpty()) return null;
        if (list.size() > 1) throw new IllegalStateException("Non-unique result for query: " + sql);
        return list.getFirst();
    }
}