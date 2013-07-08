package se.sitic.megatron.core;


/**
 * Thrown if parsing of command line arguments fails.
 */
public class CommandLineParseException extends MegatronException {
    private static final long serialVersionUID = 1L;

    public static final int NO_ACTION = 0;
    public static final int SHOW_USAGE_ACTION = 1;
    public static final int SHOW_VERSION_ACTION = 2;

    private int action = NO_ACTION;


    /**
     * Constructs instance with an action, which means that no error
     * have occured but usage or version should be displayed.
     */
    public CommandLineParseException(int action) {
        this(null);
        this.action = action;
    }


    public CommandLineParseException(String msg) {
        super(msg);
    }


    public CommandLineParseException(String msg, Throwable cause) {
        super(msg, cause);
    }


    public int getAction() {
        return action;
    }

}
