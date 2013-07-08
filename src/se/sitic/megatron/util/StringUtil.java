package se.sitic.megatron.util;

import java.io.File;
import java.util.Iterator;
import java.util.List;


/**
 * Contains static utility-methods for handling strings.
 */
public abstract class StringUtil {


    /**
     * Replaces a substring in a string with another string. Replaces all
     * occurrences.
     * <p>
     * Example: replace("name=@name@", "@name@", "hubba") --> "name=hubba".
     *
     * @throws NullPointerException if the from- or to-argument is null.
     */
    public static String replace(String str, String from, String to) {
        return replaceInternal(str, from, to, false);
    }

    
    /**
     * As replace, but replace one the first occurrence.  
     */
    public static String replaceFirst(String str, String from, String to) {
        return replaceInternal(str, from, to, true);
    }


    /**
     * Removes prefix from specified string.<p>
     * Example: removePrefix("foobar.config", "foobar.") --> "config"
     */
    public static String removePrefix(String str, String prefix) {
        if ((str == null) || (prefix == null)) {
            return str;
        }

        String result = null;
        int hitIndex = str.indexOf(prefix);
        if (hitIndex == 0) {
            if (prefix.length() < str.length()) {
                result = str.substring(prefix.length());
            } else {
                result = "";
            }
        } else {
            result = str;
        }

        return result;
    }


    /**
     * Removes suffix from specified string.<p>
     * Example: removeSuffix("foobar.config", ".config") --> "foobar"
     */
    public static String removeSuffix(String str, String suffix) {
        if ((str == null) || (suffix == null)) {
            return str;
        }

        String result = str;
        int hitIndex = str.lastIndexOf(suffix);
        if ((hitIndex != -1) && ((hitIndex + suffix.length()) == str.length())) {
            result = result.substring(0, hitIndex);
        }
        return result;
    }


    /**
     * Splits specified string in two parts at delimiter position.
     *
     * @param str string to split.
     * @param delim delimiter that marks split between head and tail.
     * @param reverse is search for delimiter reverse (search starts at end of string)?
     * @return null if one argument is null, otherwise a String-array of length
     *         two in following format: { head, tail }. If delimiter is not
     *         found, then tail is empty if isReverse is false or
     *         head is empty if isReverse is true. Delimiter is not
     *         included.
     */
    public static String[] splitHeadTail(String str, String delim, boolean reverse) {
        // check param
        if ((str == null) || (delim == null)) {
            return null;
        }

        String[] result = new String[2];
        int hitPos = reverse ? str.lastIndexOf(delim) : str.indexOf(delim);
        if (hitPos != -1) {
            result[0] = str.substring(0, hitPos);
            int tailPos = hitPos + delim.length();
            if (tailPos < str.length()) {
                result[1] = str.substring(tailPos);
            } else {
                result[1] = "";
            }
        } else {
            if (reverse) {
                result[0] = "";
                result[1] = str;
            } else {
                result[0] = str;
                result[1] = "";
            }
        }

        return result;
    }


    /**
     * Returns true if specified string is null or empty.
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }


    /**
     * Returns specified string if not null, otherwise an empty string.
     */
    public static String getNotNull(String str) {
        return (str != null) ? str : "";
    }


    /**
     * Returns specified string if not null, otherwise specified default value.
     */
    public static String getNotNull(String str, String defaultStr) {
        return (str != null) ? str : defaultStr;
    }


    /**
     * Converts specifed byte-array to hex-string.
     */
    public static String encode(byte[] buf) {
        StringBuffer result = new StringBuffer(2 * buf.length);

        for (int i = 0; i < buf.length; i++) {
            int byteAsInt = buf[i] & 0xFF;
            if (byteAsInt < 16) {
                result.append("0");
            }
            result.append(Integer.toString(byteAsInt, 16).toLowerCase());
        }

        return result.toString();
    }


    /**
     * Converts specified hex-string to byte-array.
     */
    public static byte[] decodeHexString(String hexStr) throws NumberFormatException {
        if (hexStr == null) {
            return null;
        }
        // is every digit of size two?
        if ((hexStr.length() % 2) != 0) {
            throw new NumberFormatException("Bad hex-string. Length of string must be even.");
        }

        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length(); i += 2) {
            String hexDigit = hexStr.substring(i, i + 2);
            int byteValue = Integer.parseInt(hexDigit, 16);
            result[i / 2] = (byte)(byteValue & 0xFF);
        }

        return result;
    }


    /**
     * Encodes specifed text using named entities, e.g "<" converts
     * to "&amp;lt;".
     * <p>
     * The result is safe to include in XML or HTML.
     */
    public static String encodeCharacterEntities(String str) {
        if (str == null) {
            return null;
        }

        StringBuilder result = new StringBuilder(str.length() + 50);
        for (int i = 0; i < str.length(); i++) {
            char chr = str.charAt(i);
            switch (chr) {
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                case '"':
                    result.append("&quot;");
                    break;
                default:
                    result.append(chr);
            }
        }
        return result.toString();
    }

    
    /**
     * Removes line beaks from specified string.
     */
    public static String removeLineBreaks(String str, String replaceWith) {
        String result = str;
        result = replace(result, "\n", replaceWith);
        result = replace(result, "\r", replaceWith);
        return result;
    }


    /**
     * Removes trailing white spaces (including line breaks) from 
     * specified string.
     */
    public static String removeTrailingSpaces(String str) {
        if ((str == null) || (str.length() == 0)) {
            return str;
        }

        int endIndex = 0;
        for (int i = (str.length()-1); i >= 0; i--) {
            if (!Character.isWhitespace(str.charAt(i))) {
                endIndex = i + 1;
                break;
            }
        }
        
        return str.substring(0, endIndex);
    }
    
    
    /**
     * Removes enclosing characters from string.
     * Example: str="'foobar'", enclosingChars="'" will return "foobar". 
     */
    public static String removeEnclosingChars(String str, String enclosingChars) {
        if ((str == null) || !(str.startsWith(enclosingChars) && str.endsWith(enclosingChars))) {
            return str;
        }

        return removeSuffix(removePrefix(str, enclosingChars), enclosingChars); 
    }

    
    /**
     * Truncates specified string if longer than max length. "..." is added
     * as suffix to mark that the string have been truncated.
     */
    public static String truncateString(String str, int maxLength) {
        if ((str == null) || (str.length() <= maxLength)) {
            return str;
        }
        final String suffix = "...";
        if (maxLength < suffix.length()) {
            return suffix;
        }
        
        StringBuilder result = new StringBuilder(maxLength);
        int endIndex = maxLength - suffix.length();
        result.append(str.substring(0, endIndex)).append(suffix);

        return result.toString();
    }

    
    /**
     * Returns a string containing strToCopy duplicated
     * noOfCopies times.
     */
    public static String nCopyString(String strToCopy, int noOfCopies) {
        // check param
        if (strToCopy == null) {
            return null;
        }

        StringBuilder result = new StringBuilder(noOfCopies * strToCopy.length());
        for (int i = 0; i < noOfCopies; i++) {
            result.append(strToCopy);
        }

        return result.toString();
    }

    
    /**
     * Pads specified string in head with spaces.
     */
    public static String leftPad(String str, int len) {
        return pad(str, len, " ", true);
    }


    /**
     * Pads specified string in tail with spaces.
     */
    public static String rightPad(String str, int len) {
        return pad(str, len, " ", false);
    }


    /**
     * Pads specified string in head with pad-string.
     */
    public static String leftPad(String str, int len, String padString) {
        return pad(str, len, padString, true);
    }


    /**
     * Pads specified string in tail with pad-string.
     */
    public static String rightPad(String str, int len, String padString) {
        return pad(str, len, padString, false);
    }

    
    /**
     * Returns specified files as a string list. Returns never null.
     */
    public static String toString(List<File> files, boolean shortName) {
        if (files == null) {
            return "[null]";
        }

        StringBuilder result = new StringBuilder(256);
        for (Iterator<File> iterator = files.iterator(); iterator.hasNext(); ) {
            File file = iterator.next();
            if (result.length() > 0) {
                result.append(", ");
            }

            String filename = shortName ? file.getName() : file.getAbsolutePath();
            result.append(filename);
        }

        return result.toString();
    }

    
    /**
     * Returns specified list as a string where every element is quoted and
     * comma separated.
     */
    public static String toQuotedString(List<?> list) {
        if ((list == null) || (list.size() == 0)) {
            return "";
        }
        
        StringBuffer result = new StringBuffer(256);
        for (Iterator<?> iterator = list.iterator(); iterator.hasNext(); ) {
            Object obj = iterator.next();
            String str = (obj != null) ? obj.toString() : "";
            if (result.length() > 0) {
                result.append(",");
            }
            result.append("\"").append(str).append("\"");
        }

        return result.toString();
    }

    
    /**
     * Pads specified string with pad-string.
     */
    private static String pad(String str, int len, String padString, boolean leftPad) {
        if ((str == null) || (str.length() >= len) || (padString == null)) {
            return str;
        }

        int n = (len - str.length()) / padString.length();
        String paddedString = nCopyString(padString, n);
        StringBuilder result = new StringBuilder(len);
        if (leftPad) {
            result.append(paddedString).append(str);
        } else {
            result.append(str).append(paddedString);
        }

        return result.toString();
    }
    
    
    @SuppressWarnings("null")
    private static String replaceInternal(String str, String from, String to, boolean replaceFirst) {
        if (str == null) {
            return null;
        }
        if ((from == null) || (to == null)) {
            throw new NullPointerException("from- or to-argument is null.");
        }

        StringBuilder result = null;
        int fromlength = 0;
        int index = 0;
        boolean quit = false;
        while (!quit) {
            int hitIndex = str.indexOf(from, index);
            if (hitIndex < 0) {
                break;
            }
            if (result == null) {
                result = new StringBuilder(str.length() + 40);
                fromlength = from.length();
            }
            result.append(str.substring(index, hitIndex));
            result.append(to);
            index = hitIndex + fromlength;
            quit = replaceFirst;
        }
        if (index == 0) {
            return str;
        }
        result.append(str.substring(index));
        return result.toString();
    }


}
