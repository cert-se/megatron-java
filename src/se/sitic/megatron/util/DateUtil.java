package se.sitic.megatron.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Contains static utility-methods for handling dates.
 */
public abstract class DateUtil {

    /**
     * Date and time format with 'T' as separator and time-zone,
     * e.g. "2007-12-10T14:59:49+0200", according to ISO 8601.
     */
    public static final String DATE_TIME_FORMAT_WITH_T_CHAR_AND_TZ = "yyyy-MM-dd'T'HH:mm:ssZ";

    /**
     * Date and time format with 'T' as separator, e.g. "2007-12-10T14:59:49",
     */
    public static final String DATE_TIME_FORMAT_WITH_T_CHAR = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Date and time format with milli-seconds, e.g. "2007-12-10 14:59:49.993".
     */
    public static final String DATE_TIME_FORMAT_WITH_MS = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * Date and time format with seconds, e.g. "2007-12-10 14:59:49".
     */
    public static final String DATE_TIME_FORMAT_WITH_SECONDS = "yyyy-MM-dd HH:mm:ss";

    /**
     * Date format, e.g. "2007-12-10".
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * Time format with seconds, e.g. "14:59:49".
     */
    public static final String TIME_FORMAT_WITH_SECONDS = "HH:mm:ss";


    /**
     * Returns date formatted as specified in format.
     *
     * @param format format pattern, e.g. DATE_TIME_FORMAT_WITH_SECONDS.
     * @param date date to format.
     * @return date formatted by format with SimpleDateFormat
     * @see java.text.SimpleDateFormat
     */
    public static String formatDateTime(String format, Date date) {
        if ((format == null) || (date == null)) {
            return "";
        }

        return new SimpleDateFormat(format).format(date);
    }


    /**
     * Parses specified date string according to format.
     *
     * @param format format pattern, e.g. DATE_TIME_FORMAT_WITH_SECONDS.
     * @param dateStr string to parse.
     */
    public static Date parseDateTime(String format, String dateStr) throws ParseException {
        if ((format == null) || (dateStr == null)) {
            return null;
        }

        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setLenient(false);
        return formatter.parse(dateStr);
    }


    /**
     * Returns specified duration as a string in the following format:
     * "hours:minutes:seconds.milliseconds".
     */
    public static String formatDuration(long millis) {
        final int millisInSecond = 1000;
        final int millisInMinute = 60*1000;
        final int millisInHour = 60*60*1000;

        int hours = (int) (millis / millisInHour);
        millis = millis - (hours * millisInHour);
        int minutes = (int) (millis / millisInMinute);
        millis = millis - (minutes * millisInMinute);
        int seconds = (int) (millis / millisInSecond);
        millis = millis - (seconds * millisInSecond);
        int milliseconds = (int) millis;

        StringBuilder result = new StringBuilder(16);
        result.append(hours);
        result.append(':');
        result.append((char) (minutes / 10 + '0'));
        result.append((char) (minutes % 10 + '0'));
        result.append(':');
        result.append((char) (seconds / 10 + '0'));
        result.append((char) (seconds % 10 + '0'));
        result.append('.');
        if (milliseconds < 10) {
            result.append('0').append('0');
        } else if (milliseconds < 100) {
            result.append('0');
        }
        result.append(milliseconds);

        return result.toString();
    }

}
