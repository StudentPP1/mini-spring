package org.spring.hibernate.transaction;

/**
 * Defines how transactional methods behave when invoked inside or outside
 * of an existing transaction context.
 *
 * In JDBC terms:
 *  - "create transaction" means setAutoCommit(false) and commit/rollback manually;
 *  - "without transaction" means leave autoCommit=true (each statement is its own transaction).
 */
public enum Propagation {

    /**
     * <b>REQUIRED</b> –
     * Join the current transaction if one exists; otherwise, start a new one.
     * <p>
     * This is the default behavior in most ORMs (e.g. Spring, Hibernate).
     * <ul>
     *   <li>If a transaction is active → join it (share the same Connection).</li>
     *   <li>If no transaction → open a new Connection, setAutoCommit(false).</li>
     *   <li>Commit / rollback only once at the outermost level.</li>
     * </ul>
     */
    REQUIRED,

    /**
     * <b>REQUIRES_NEW</b> –
     * Always start a new, independent transaction.
     * <p>
     * If a transaction already exists, it is suspended until this one completes.
     * The new transaction uses its own Connection.
     * <ul>
     *   <li>Existing transaction → temporarily paused (Connection unbound).</li>
     *   <li>New transaction → new Connection, setAutoCommit(false).</li>
     *   <li>After commit/rollback → resume the previous one.</li>
     * </ul>
     */
    REQUIRES_NEW,

    /**
     * <b>SUPPORTS</b> –
     * Execute within a transaction if one exists; otherwise, execute non-transactionally.
     * <p>
     * This mode never starts a new transaction by itself.
     * Typically used for read-only queries.
     * <ul>
     *   <li>If a transaction exists → join it.</li>
     *   <li>If none → leave autoCommit=true, every statement commits immediately.</li>
     * </ul>
     */
    SUPPORTS,

    /**
     * <b>MANDATORY</b> –
     * Must run inside an existing transaction.
     * <p>
     * If no transaction is active, an exception is thrown.
     * <ul>
     *   <li>If a transaction exists → join it.</li>
     *   <li>If none → throw IllegalStateException.</li>
     * </ul>
     */
    MANDATORY,

    /**
     * <b>NEVER</b> –
     * Must execute without a transaction.
     * <p>
     * If a transaction exists, an exception is thrown.
     * <ul>
     *   <li>If a transaction exists → throw IllegalStateException.</li>
     *   <li>If none → run with autoCommit=true (plain JDBC mode).</li>
     * </ul>
     */
    NEVER
}
