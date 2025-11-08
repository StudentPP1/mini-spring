package org.spring.hibernate.transaction;

import org.spring.hibernate.connection.ConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;

public class TransactionManager {
    private final ConnectionProvider provider;
    private final ThreadLocal<TransactionContext> current = new ThreadLocal<>();
    private final ThreadLocal<Deque<TransactionContext>> paused = ThreadLocal.withInitial(ArrayDeque::new);

    public TransactionManager(ConnectionProvider provider) {
        this.provider = provider;
    }

    public void begin(TransactionDefinition def) throws SQLException {
        switch (def.propagation()) {
            case REQUIRED -> beginRequired(def);
            case REQUIRES_NEW -> beginRequiresNew(def);
            case SUPPORTS -> {
                if (isActive()) current().setDepth(current().getDepth() + 1);
            }
            case MANDATORY -> {
                if (!isActive()) throw new IllegalStateException("Transaction required (MANDATORY)");
                current().setDepth(current().getDepth() + 1);
            }
            case NEVER -> {
                if (isActive()) throw new IllegalStateException("Existing transaction not allowed (NEVER)");
            }
        }
    }

    public void commit() throws SQLException {
        TransactionContext ctx = current.get();
        if (ctx == null || ctx.getDepth() == 0) return;
        if (ctx.getDepth() > 1) {
            ctx.setDepth(ctx.getDepth() - 1);
            return;
        }
        try {
            if (ctx.isRollbackOnly()) {
                transactionRollback(ctx);
                throw new RuntimeException("Transaction marked as rollback-only");
            }
            ctx.getConnection().commit();
        } catch (SQLException e) {
            transactionRollback(ctx);
            throw new RuntimeException("Commit failed; rolled back", e);
        } finally {
            restoreAndRelease(ctx);
            resumeIfNeeded();
        }
    }

    public void rollback() throws SQLException {
        TransactionContext ctx = current.get();
        if (ctx == null || ctx.getDepth() == 0) return;
        transactionRollback(ctx);
        restoreAndRelease(ctx);
        resumeIfNeeded();
    }

    public boolean isActive() {
        TransactionContext ctx = current.get();
        return ctx != null && ctx.getDepth() > 0;
    }

    public void setRollbackOnly() {
        TransactionContext ctx = current.get();
        if (ctx != null) ctx.setRollbackOnly(true);
    }

    private void beginRequired(TransactionDefinition def) throws SQLException {
        if (!isActive()) {
            createNewTransaction(def);
        } else {
            current().setDepth(current().getDepth() + 1);
        }
    }

    private void createNewTransaction(TransactionDefinition def) throws SQLException {
        TransactionContext ctx = new TransactionContext(null, 0, false, null, null);
        Connection connection = provider.get();
        Integer prevIsolation = connection.getTransactionIsolation();
        Boolean prevReadOnly = connection.isReadOnly();
        if (def.isolation().getLevel() != prevIsolation) {
            connection.setTransactionIsolation(def.isolation().getLevel());
        }
        if (def.readOnly() != prevReadOnly) {
            connection.setReadOnly(def.readOnly());
        }
        connection.setAutoCommit(false);
        ctx.setConnection(connection);
        ctx.setDepth(1);
        ctx.setPrevIsolation(prevIsolation);
        ctx.setPrevReadOnly(prevReadOnly);
        current.set(ctx);
        provider.setTransactionActive(true);
    }

    private void beginRequiresNew(TransactionDefinition def) throws SQLException {
        if (isActive()) {
            paused.get().push(current.get());
            provider.setTransactionActive(false);
            provider.unbind();
            current.remove();
        }
        createNewTransaction(def);
    }

    private void restoreAndRelease(TransactionContext ctx) {
        try {
            if (ctx.getPrevReadOnly() != null) {
                ctx.getConnection().setReadOnly(ctx.getPrevReadOnly());
            }
            if (ctx.getPrevIsolation() != null) {
                ctx.getConnection().setTransactionIsolation(ctx.getPrevIsolation());
            }
            ctx.getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            provider.setTransactionActive(false);
            provider.release(true);
            current.remove();
        }
    }

    private void resumeIfNeeded() {
        Deque<TransactionContext> stack = paused.get();
        if (!stack.isEmpty()) {
            TransactionContext prev = stack.pop();
            current.set(prev);
            provider.bind(prev.getConnection());
            provider.setTransactionActive(true);
        } else {
            paused.remove();
        }
    }

    private void transactionRollback(TransactionContext ctx) throws SQLException {
        ctx.getConnection().rollback();
    }

    private TransactionContext current() {
        TransactionContext ctx = current.get();
        if (ctx == null) throw new IllegalStateException("No current transaction context");
        return ctx;
    }
}
