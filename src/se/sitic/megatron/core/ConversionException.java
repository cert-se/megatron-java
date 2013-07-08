package se.sitic.megatron.core;


/**
 * Thrown when a conversion fails, e.g. IP as a string to a long.
 */
public class ConversionException extends MegatronException {
    private static final long serialVersionUID = 1L;


    public ConversionException(String msg) {
        super(msg);
    }

    
    public ConversionException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
}
