package org.spring.hibernate.transaction;

public enum TransactionState {
    ACTIVE, COMMITTING, ROLLED_BACK, MARKED_ROLLBACK
}
