package se.sitic.megatron.core;


/**
 * General exception in the Megatron application.
 *
 * Use this class as a super-class for more specific exception in Megatron.
 */
public class MegatronException extends Exception {
    private static final long serialVersionUID = 1L;


    public MegatronException(String msg) {
        super(msg);
    }

    public MegatronException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
