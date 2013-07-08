package se.sitic.megatron.util;

import java.util.Date;


/**
 * Contains static utility-methods for SQL stuff.
 */
public abstract class SqlUtil {
    // UNUSED: private static final Logger log = Logger.getLogger(SqlUtil.class);

	
    /**
     * Converts specified timestamp in seconds to a Date.
     */
    public static Date convertTimestamp(long timestampInSec) {
        return new Date(timestampInSec * 1000L);
    }

    
    /**
     * Converts specified timestamp in milliseconds to seconds.
     */
    public static long convertTimestampToSec(long timestampInMs) {
        return Math.round(timestampInMs / 1000d);  
    }

    
    /**
     * Converts specified timestamp to seconds.
     */
    public static long convertTimestamp(Date timestamp) {
        return Math.round(timestamp.getTime() / 1000d);  
    }

}
