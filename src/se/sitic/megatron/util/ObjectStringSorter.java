package se.sitic.megatron.util;

import java.text.Collator;
import java.util.Comparator;

import org.apache.log4j.Logger;


/**
 * Abstract class for sorting objects by string comparision.
 * <p>
 * This sorter does take locale into account, and supports sorting of trailing
 * numbers.
 */
public abstract class ObjectStringSorter implements Comparator<Object> {
    private static final Logger log = Logger.getLogger(ObjectStringSorter.class);

    /** String-comparer that is locale-sensitive */
    private Collator collator = null;
    /** Compare trailing numbers? */
    private boolean numberSensitive = true;
    /** Is a null value greater than a non-null value? */
    private boolean nullGreaterThanNotNull = true;


    /**
     * Returns default implementation of this class that uses toString() as
     * string representation.
     */
    public static synchronized ObjectStringSorter createDefaultSorter() {
        ObjectStringSorter result = new ObjectStringSorter() {
            @Override
            protected String getObjectString(Object obj) {
                return (obj != null) ? obj.toString() : null;
            }
        };

        return result;
    }

    /**
     * Same as createDefaultSorter(), but ignoring trailing numbers (better
     * performance).
     */
    public static synchronized ObjectStringSorter createBasicSorter() {
        ObjectStringSorter result = new ObjectStringSorter(false, true) {
            @Override
            protected String getObjectString(Object obj) {
                return (obj != null) ? obj.toString() : null;
            }
        };

        return result;
    }

    /**
     * Constructs a sorter that is number sensitive, i.e. trailing numbers
     * matters.
     */
    public ObjectStringSorter() {
        this.collator = Collator.getInstance();
    }

    /**
     * Constructor.
     *
     * @param numberSensitive
     *            Compare trailing numbers? If true, and the two strings to
     *            compare have numbers as suffixes and the same prefix, the
     *            numbers will be compared. Only integers are supported. Example
     *            when false: { "str1", "str10", "str2", "str20", "str3" }
     *            Example when true: { "str1", "str2", "str3", "str10", "str20" }
     * @param nullGreaterThanNotNull
     *            Is a null object string greater than a non-null value?
     */
    public ObjectStringSorter(boolean numberSensitive, boolean nullGreaterThanNotNull) {
        this();
        this.numberSensitive = numberSensitive;
        this.nullGreaterThanNotNull = nullGreaterThanNotNull;
    }

    /**
     * Compares string representation for specified objects, which may be null.
     * Implements Comparator.compare(Object, Object).
     */
    @Override
    public int compare(Object obj1, Object obj2) {
        String str1 = getObjectString(obj1);
        String str2 = getObjectString(obj2);

        if (str1 == str2)
            return 0;

        if (str1 == null)
            return this.nullGreaterThanNotNull ? 1 : -1;

        if (str2 == null)
            return this.nullGreaterThanNotNull ? -1 : 1;

        int result = collator.compare(str1, str2);

        if (this.numberSensitive && (result != 0) && isNumericSuffixAvailable(str1) && isNumericSuffixAvailable(str2)) {
            String[] headTailArray1 = splitNumericSuffixString(str1);
            String[] headTailArray2 = splitNumericSuffixString(str2);
            String prefix1 = headTailArray1[0];
            String prefix2 = headTailArray2[0];
            int prefixResult = collator.compare(prefix1, prefix2);
            // same prefix?
            if (prefixResult == 0) {
                try {
                    // compare as numbers
                    long long1 = Long.parseLong(headTailArray1[1]);
                    long long2 = Long.parseLong(headTailArray2[1]);
                    result = (long1 < long2) ? -1 : (long1 == long2) ? 0 : 1;
                } catch (NumberFormatException e) {
                    log.error("Internal Error: String should be a long.", e);
                }
            }
        }

        return result;
    }

    /** Gets the comparision string for the specified object */
    protected abstract String getObjectString(Object obj);

    /** Returns true if specifed string ends with a number. */
    private boolean isNumericSuffixAvailable(String str) {
        // assert: null-check already done
        if (str.length() == 0)
            return false;

        char lastChar = str.charAt(str.length() - 1);
        return (('0' <= lastChar) && (lastChar <= '9'));
    }

    /**
     * Splits specified string in two parts: head (string before numeric
     * suffix), and tail (numeric suffix or an empty string).
     * <p>
     * Example: "foo42" returns {"foo", "42"}, "foo" returns {"foo", ""}
     */
    private String[] splitNumericSuffixString(String str) {
        String[] result = new String[2];

        for (int i = (str.length() - 1); i >= 0; i--) {
            char ch = str.charAt(i);
            // is not a number?
            if (!(('0' <= ch) && (ch <= '9'))) {
                result[0] = str.substring(0, i + 1);
                result[1] = ((i + 1) < str.length()) ? str.substring(i + 1, str.length()) : "";
                return result;
            }
        }

        result[0] = "";
        result[1] = str;
        return result;
    }

}
