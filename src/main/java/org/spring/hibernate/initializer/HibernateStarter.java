package org.spring.hibernate.initializer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spring.bean.BeanFactory;
import org.spring.hibernate.connection.PooledDataSource;
import org.spring.hibernate.connection.ThreadLocalConnectionProvider;
import org.spring.hibernate.entity.EntityScanner;
import org.spring.hibernate.interceptor.TransactionalBeanPostProcessor;
import org.spring.hibernate.session.DefaultSessionFactory;
import org.spring.hibernate.session.SessionFactory;
import org.spring.hibernate.transaction.TransactionManager;
import org.spring.utils.PropertiesUtils;

import java.io.IOException;
import java.sql.SQLException;

public final class HibernateStarter {
    private static final Logger log = LogManager.getLogger(HibernateStarter.class);

    private HibernateStarter() {}

    public static void init(BeanFactory beanFactory, String basePackage) {
        try {
            log.trace("start initializing of hibernate");
            PooledDataSource dataSource = new PooledDataSource(
                    PropertiesUtils.getProperty("db.url"),
                    PropertiesUtils.getProperty("db.user"),
                    PropertiesUtils.getProperty("db.pass")
            );
            log.trace("finish creating of dataSource");
            ThreadLocalConnectionProvider connectionProvider = new ThreadLocalConnectionProvider(dataSource);
            log.trace("finish creating of connectionProvider");
            TransactionManager transactionManager = new TransactionManager(connectionProvider);
            log.trace("finish creating of transactionManager");
            SessionFactory sessionFactory = new DefaultSessionFactory(
                    connectionProvider,
                    transactionManager,
                    EntityScanner.scan(basePackage)
            );
            log.trace("finish creating of sessionFactory");
            beanFactory.registerSingleton("dataSource", dataSource);
            beanFactory.registerSingleton("connectionProvider", connectionProvider);
            beanFactory.registerSingleton("transactionManager", transactionManager);
            beanFactory.registerSingleton("sessionFactory", sessionFactory);
            log.trace("register instances to context");
            beanFactory.addPostProcessor(
                    new TransactionalBeanPostProcessor(transactionManager, sessionFactory)
            );
            log.trace("add post processor for transactions");
        } catch (IOException | ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
