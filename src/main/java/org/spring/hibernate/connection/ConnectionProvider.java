package org.spring.hibernate.connection;

import java.sql.Connection;

public interface ConnectionProvider {
    Connection get();
    Connection getOrNull();
    void release(boolean force);
    void bind(Connection c);
    void unbind();
}
