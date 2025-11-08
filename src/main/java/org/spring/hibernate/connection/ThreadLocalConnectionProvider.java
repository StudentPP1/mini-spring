package org.spring.hibernate.connection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ThreadLocalConnectionProvider implements ConnectionProvider {
    private final DataSource dataSource;
    private final ThreadLocal<Connection> connection = new ThreadLocal<>();
    private final ThreadLocal<Boolean> transactionActive = ThreadLocal.withInitial(() -> false);

    public ThreadLocalConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    @Override
    public void bind(Connection c) {
        connection.set(c);
    }

    @Override
    public void unbind() {
        connection.remove();
        transactionActive.remove();
    }

    @Override
    public Connection get() {
        if (connection.get() == null) {
            try {
                connection.set(dataSource.getConnection());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return connection.get();
    }

    @Override
    public Connection getOrNull() {
        return connection.get();
    }

    @Override
    public void setTransactionActive(boolean active) {
        transactionActive.set(active);
    }

    @Override
    public boolean isTransactionActive() {
        return Boolean.TRUE.equals(transactionActive.get());
    }

    @Override
    public boolean isExistConnection() {
        return connection.get() != null;
    }

    @Override
    public void release(boolean force) {
        Connection c = connection.get();
        if (c == null) return;
        if (force || Boolean.FALSE.equals(transactionActive.get())) {
            try {
                c.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            connection.remove();
            transactionActive.remove();
        }
    }
}
