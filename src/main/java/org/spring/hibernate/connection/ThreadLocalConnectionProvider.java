package org.spring.hibernate.connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class ThreadLocalConnectionProvider implements ConnectionProvider {
    private static final Logger log = LogManager.getLogger(ThreadLocalConnectionProvider.class);
    private final DataSource dataSource;
    private final ThreadLocal<Connection> connection = new ThreadLocal<>();
    private final ThreadLocal<Boolean> transactionActive = ThreadLocal.withInitial(() -> false);

    public ThreadLocalConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    @Override
    public void bind(Connection c) {
        log.trace("set new connection to thread local");
        connection.set(c);
    }

    @Override
    public void unbind() {
        log.trace("remove connection from thread local");
        connection.remove();
        transactionActive.remove();
    }

    @Override
    public Connection get() {
        if (connection.get() == null) {
            try {
                log.trace("create new connection and save to thread local");
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
        log.trace("released connection and return to pool");
    }
}
