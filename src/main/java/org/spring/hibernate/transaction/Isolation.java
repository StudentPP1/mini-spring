package org.spring.hibernate.transaction;

import lombok.Getter;

import java.sql.Connection;

@Getter
public enum Isolation {
    DEFAULT(Connection.TRANSACTION_NONE),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int level;

    Isolation(int level) {
        this.level = level;
    }
}
