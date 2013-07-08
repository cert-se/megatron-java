package se.sitic.megatron.rss;


/**
 * Thrown if parsing of RSS fails.
 */
public class RssParseException extends RssException {
    private static final long serialVersionUID = 1L;


    public RssParseException(String msg) {
        super(msg);
    }


    public RssParseException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
