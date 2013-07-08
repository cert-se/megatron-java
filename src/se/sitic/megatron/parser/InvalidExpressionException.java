package se.sitic.megatron.parser;

import se.sitic.megatron.core.MegatronException;


/**
 * Thrown when a line expression is invalid.
 */
public class InvalidExpressionException extends MegatronException {
    private static final long serialVersionUID = 1L;


    public InvalidExpressionException(String msg) {
        super(msg);
    }

    
    public InvalidExpressionException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
}
