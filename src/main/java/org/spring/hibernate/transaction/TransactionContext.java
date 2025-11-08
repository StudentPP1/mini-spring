package org.spring.hibernate.transaction;

import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;

@Getter
@Setter
public final class TransactionContext {
    /**
     * Current physical connection with db
     */
    private Connection connection;

    /**
     * Inner transaction counter for Propagation.REQUIRES_NEW
     */
    private int depth;

    /**
     * Mark that current transaction must be rollback
     */
    private boolean rollbackOnly;

    /**
     * return default value of isolation to connection after transaction
     */
    private Integer prevIsolation;

    /**
     * return default value of readOnly to connection after transaction
     */
    private Boolean prevReadOnly;

    public TransactionContext(Connection connection, int depth, boolean rollbackOnly, Integer prevIsolation, Boolean prevReadOnly) {
        this.connection = connection;
        this.depth = depth;
        this.rollbackOnly = rollbackOnly;
        this.prevIsolation = prevIsolation;
        this.prevReadOnly = prevReadOnly;
    }

    // use commit()/rollback() when we in main transaction (depth = 1)
    // when depth != 1 -> we inside inner transaction
    public boolean isOuter() {
        return this.depth == 1;
    }
}
