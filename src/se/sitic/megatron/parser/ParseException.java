package se.sitic.megatron.parser;

import se.sitic.megatron.core.MegatronException;


/**
 * Thrown when parsing of a log record fails.
 */
public class ParseException extends MegatronException {
    private static final long serialVersionUID = 1L;


    public ParseException(String msg) {
        super(msg);
    }

    
    public ParseException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
}
