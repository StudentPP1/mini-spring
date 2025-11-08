package org.spring.hibernate.query;


import org.spring.hibernate.entity.EntityMetadata;
import org.spring.hibernate.session.DefaultSession;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public record SimpleQuery<T>(DefaultSession session, String sql, Class<T> resultType) implements Query<T> {

    @Override
    public List<T> list() {
        EntityMetadata metadata = session.getEntityMetadata(resultType);
        try (PreparedStatement statement = session.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<T> results = new ArrayList<>();
            while (resultSet.next()) {
                results.add(ResultSetParser.parseEntity(resultSet, metadata, resultType));
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
    }

    @Override
    public T singleResult() {
        List<T> list = list();
        if (list.isEmpty()) return null;
        if (list.size() > 1) throw new IllegalStateException("Non-unique result for query: " + sql);
        return list.get(0);
    }
}

