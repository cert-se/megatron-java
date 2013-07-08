package se.sitic.megatron.mail;

import se.sitic.megatron.core.MegatronException;


/**
 * Thrown if mail sending fails.
 */
public class MailException extends MegatronException {
    private static final long serialVersionUID = 1L;


    public MailException(String msg) {
        super(msg);
    }


    public MailException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
