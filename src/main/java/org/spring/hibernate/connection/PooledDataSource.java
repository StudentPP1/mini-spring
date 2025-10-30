package org.spring.hibernate.connection;

import lombok.SneakyThrows;
import org.postgresql.ds.PGConnectionPoolDataSource;
import org.spring.utils.PropertiesUtils;

import java.sql.Connection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PooledDataSource extends PGConnectionPoolDataSource {
    private final Queue<Connection> connectionPool;

    @SneakyThrows
    public PooledDataSource(String url, String username, String password) {
        super(); // create DataSource
        setUrl(url);
        setUser(username);
        setPassword(password);
        connectionPool = new ConcurrentLinkedQueue<>();
        for (int i = 1; i <= Integer.getInteger(PropertiesUtils.getProperty("hibernate.connection.pool.size")); i++) {
            Connection physicalConnection = super.getConnection();
            this.connectionPool.add(new ProxyConnection(physicalConnection, connectionPool));
        }
    }

    @Override
    public Connection getConnection() {
        return this.connectionPool.peek();
    }
}
