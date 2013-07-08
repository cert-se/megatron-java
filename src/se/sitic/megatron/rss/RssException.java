package se.sitic.megatron.rss;


/**
 * Thrown if RSS processing fails.
 */
public class RssException extends Exception {
    private static final long serialVersionUID = 1L;


    public RssException(String msg) {
        super(msg);
    }


    public RssException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
