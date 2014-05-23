package se.sitic.megatron.util;


/**
 * Constants in the application.
 */
public abstract class Constants {

    /** Line break in files etc. */
    public static final String LINE_BREAK = "\n";

    /** UTF-8 character-set in Java core API. */
    public static final String UTF8 = "UTF-8";

    /** ISO-8859 character-set in Java core API. */
    public static final String ISO8859 = "ISO-8859-1";

    /** MIME-type for plain text. */
    public static final String MIME_TEXT_PLAIN = "text/plain";
    
    /** Comments in config files starts with this string. */
    public static final String CONFIG_COMMENT_PREFIX = "#";

    /** Hash algoritm to use. */
    public static final String DIGEST_ALGORITHM = "md5";

    /** Job type name to use when name is missing in the job_type table. */
    public static final String DEFAULT_JOB_TYPE = "default";
    
    // Values for the property filter.countryCodeFilter.organizationToFilter
    // and filter.asnFilter.organizationToFilter
    public static final String ORGANIZATION_PRIMARY = "primary";
    public static final String ORGANIZATION_SECONDARY = "secondary";
    public static final String ORGANIZATION_BOTH = "both";

    // Additional format strings for parser.timestampFormat.
    public static final String TIME_STAMP_FORMAT_EPOCH_IN_SEC = "epochInSec";
    public static final String TIME_STAMP_FORMAT_EPOCH_IN_MS = "epochInMs";
    public static final String TIME_STAMP_FORMAT_WINDOWS_EPOCH = "windowsEpoch";
    
}
