package org.spring.hibernate.transaction;

public record TransactionDefinition(
        Propagation propagation,
        Isolation isolation,
        boolean readOnly,
        int timeout
) {
}
