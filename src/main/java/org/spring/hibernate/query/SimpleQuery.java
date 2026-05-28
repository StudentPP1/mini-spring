package org.spring.hibernate.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.hibernate.entity.EntityKey;
import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.session.EntityLoader;
import org.spring.hibernate.session.InternalSession;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class SimpleQuery<T> implements Query<T> {
    private static final Logger log = LogManager.getLogger(SimpleQuery.class);
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

    @Override
    public List<T> list() {
        log.trace("sql: {}", sql);
        log.trace("parse List<{}>", resultType.getSimpleName());
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
            for (Map.Entry<Integer, Object> entry : params.entrySet()) {
                statement.setObject(entry.getKey(), entry.getValue());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetParser resultSetParser = new ResultSetParser(resultSet, session, loader);
                Map<Object, T> resultRows = new LinkedHashMap<>();
                while (resultSet.next()) {
                    String idColumnName = metadata.idColumn();
                    Object id = resultSet.getObject(idColumnName);
                    EntityKey key = new EntityKey(resultType, id);
                    T entity = resultRows.get(id);
                    if (entity == null) {
                        entity = (T) session.getPersistenceContext().get(key);
                        if (entity == null) {
                            log.trace("{}: parse simple fields in ResultSet", resultType.getSimpleName());
                            entity = resultSetParser.parseEntity(resultType, metadata, "");
                            session.getPersistenceContext().put(key, entity);
                            log.trace("{}: load single EAGER fields if exists", resultType.getSimpleName());
                            loader.fillForeignKeys(metadata, resultSet, entity, "");
                        }
                        resultRows.put(id, entity);
                    }
                    log.trace("{}: parse EAGER collection in ResultSet if exists", resultType.getSimpleName());
                    resultSetParser.parseJoinedCollections(entity, metadata);
                }
                List<T> finalResults = new ArrayList<>(resultRows.values());
                log.trace("{}: check having not parsed EAGER collection", resultType.getSimpleName());
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