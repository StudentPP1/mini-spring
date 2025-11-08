package org.spring.hibernate.connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.ds.PGSimpleDataSource;
import org.spring.utils.PropertiesUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PooledDataSource extends PGSimpleDataSource {
    private static final Logger log = LogManager.getLogger(PooledDataSource.class);
    private final Queue<Connection> connectionPool;

    public PooledDataSource(String url, String username, String password) throws SQLException {
        super(); // create DataSource
        setUrl(url);
        setUser(username);
        setPassword(password);
        log.info("connected to database");
        connectionPool = new ConcurrentLinkedQueue<>();
        int poolSize = Integer.parseInt(PropertiesUtils.getProperty("hibernate.connection.pool.size"));
        log.debug("connection pool size: {}", poolSize);
        for (int i = 1; i <= poolSize; i++) {
            log.debug("create {} connection", i);
            Connection physicalConnection = super.getConnection();
            ProxyConnection proxyConnection = new ProxyConnection(physicalConnection, connectionPool);
            this.connectionPool.add(proxyConnection);
            log.debug("add {} proxy connection to pool", i);
        }
        log.info("create connection pool");
    }

    @Override
    public Connection getConnection() {
        return this.connectionPool.poll();
    }
}
