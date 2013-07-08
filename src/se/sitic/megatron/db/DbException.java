package se.sitic.megatron.db;

import se.sitic.megatron.core.MegatronException;

/**
 * Thrown if DB handling fails.
 */
public class DbException extends MegatronException {
    private static final long serialVersionUID = 1L;


    public DbException(String msg) {
        super(msg);
    }


    public DbException(String msg, Throwable cause) {
        super(msg, cause);
    }

}