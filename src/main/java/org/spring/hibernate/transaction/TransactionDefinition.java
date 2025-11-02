package org.spring.hibernate.transaction;

import java.time.Duration;

public record TransactionDefinition(
        Propagation propagation,
        Isolation isolation,
        boolean readOnly,
        Duration timeout
) {
}
