package se.sitic.megatron.util;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.ConversionException;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.parser.LogEntryMapper;


/**
 * Contains miscellaneous static utility-methods that are application specific.
 */
public abstract class AppUtil {
    private static final Logger log = Logger.getLogger(AppUtil.class);

    private static Random rand = new Random();
    

    /**
     * Compiles regular expression from specified property. Expands the
     * regular expression before compiling it.
     *
     * @return compiled regular expression, or null if not found.
     * @throws MegatronException if invalid regular expression.
     */
    public static Matcher compileRegExp(TypedProperties props, String regExpPropertyKey) throws MegatronException {
        Matcher result = null;
        String regExp = null;
        try {
            regExp = props.getString(regExpPropertyKey, null);
            regExp = AppUtil.expandRegExp(props, regExp);
            if (!StringUtil.isNullOrEmpty(regExp)) {
                log.debug("Compiling " + regExpPropertyKey + ": " + regExp);
                result = Pattern.compile(regExp).matcher("");
            }
        } catch (PatternSyntaxException e) {
            String msg = "Cannot compile regular expression: " + regExp;
            throw new MegatronException(msg, e);
        }

        return result;
    }


    /**
     * Expands specified regular expresson with templates defined in properties.
     * <p>
     * Example: "titleMustNotMatch=@secuniaLinuxDistro@" will be expanded to
     * "titleMustNotMatch=(?i)(debian|gentoo|suse|ubuntu)\supdate".
     *
     * @param regExp regular expression to expand, e.g. "@secuniaLinuxDistro@".
     */
    public static String expandRegExp(TypedProperties props, String regExp) {
        if ((props == null) || (regExp == null)) {
            return regExp;
        }

        // Return if no "@" found
        if (regExp.indexOf('@') == -1) {
            return regExp;
        }

        // -- Find template tokens
        List<String> tokens = new ArrayList<String>();
        // "@" may be escaped by "\"
        String tokenStr = StringUtil.replace(regExp, "\\@", "#");
        Pattern pattern = Pattern.compile("(@.+?@)");
        Matcher matcher = pattern.matcher(tokenStr);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));
            }
        }

        // -- Replace tokens
        String result = regExp;
        final String regExpTemplatePrefix = "@regExp.";
        for (Iterator<String> iterator = tokens.iterator(); iterator.hasNext(); ) {
            String templateName = iterator.next();
            String key = templateName;
            // key may not be prefixed with "@regExp."
            if (!key.startsWith(regExpTemplatePrefix)) {
                key = regExpTemplatePrefix + key.substring(1);
            }
            // remove enclosing "@".
            key = key.substring(1, key.length()-1);
            String templateValue = props.getString(key, null);

            if (templateValue != null) {
                result = StringUtil.replace(result, templateName, templateValue);
            }
        }

        return result;
    }


    /**
     * Returns specified string hashed, e.g. using the MD5 algorithm.<p>
     *
     * @return hex-encoded hash value.
     */
    public static String hashString(String str) {
        str = (str != null) ? str : "";
        String result = null;
        try {
            MessageDigest digest = MessageDigest.getInstance(Constants.DIGEST_ALGORITHM);
            byte[] hashValue = digest.digest(str.getBytes(Constants.UTF8));
            result = StringUtil.encode(hashValue);
        } catch (Exception e) {
            // NoSuchAlgorithmException, UnsupportedEncodingException
            log.error("Cannot hash string (this should never happen).", e);
            result = str;
        }
        return result;
    }
    
    
    /**
     * Expands variables in specified template. Side effect: items may be added to attrMap. 
     * 
     * @param template template with variables to expand.
     * @param attrMap map replace values (key: variable name, value: variable value).
     * 
     * @throws ConversionException if not all variables have been expanded.
     */
    public static String replaceVariables(String str, Map<String, String> attrMap, boolean isXml, String templateName) throws ConversionException {
        
        // TODO: Convert this method to a Class, e.g. TemplateReplacer. Can avoid creating so many objects. May also use TypedProperties.   
        
        // -- Add padding if present
        // Format: variableName#padRight<num>|padLeft<num>
        // Example: $ipAddress#padRight10
        if ((str.indexOf("#padLeft") != -1) || (str.indexOf("#padRight") != -1)) {
            String regExp = "\\" + LogEntryMapper.VARIABLE_PREFIX + "(\\w+?)#(padLeft|padRight)(\\d+)";
            Matcher matcher = Pattern.compile(regExp).matcher(str);
            while (matcher.find() && (matcher.groupCount() == 3)) {
                String variable = matcher.group(1);
                String padType = matcher.group(2);
                int len = Integer.parseInt(matcher.group(3));
                String value = attrMap.get(variable);
                if (value != null) {
                    if (padType.equals("padLeft")) {
                        value = StringUtil.leftPad(value, len);
                    } else if (padType.equals("padRight")) {
                        value = StringUtil.rightPad(value, len);
                    }
                    String key = variable + "#" + padType + len;
                    attrMap.put(key, value);
                }
            }
        }
        
        // -- Add $freeTextList and $additionalItemList if present 
        if (str.indexOf(LogEntryMapper.VARIABLE_PREFIX + "freeTextList") != -1) {
            StringBuilder freeTextListStr = new StringBuilder(128);
            int freeTextIndex = 0;
            boolean freeTextFound = true; 
            while (freeTextFound) {
                String key = LogEntryMapper.FREE_TEXT_PREFIX + (freeTextIndex++);
                if (attrMap.get(key) != null) {
                    if (freeTextListStr.length() > 0) {
                        freeTextListStr.append(", ");
                    }
                    freeTextListStr.append('"').append(attrMap.get(key)).append('"');
                } else {
                    freeTextFound = false;
                }
            }
            attrMap.put("freeTextList", freeTextListStr.toString());
        }
        if (str.indexOf(LogEntryMapper.VARIABLE_PREFIX + "additionalItemList") != -1) {
            StringBuilder additonalItemListStr = new StringBuilder(128);
            List<String> keys = new ArrayList<String>(attrMap.keySet());
            Collections.sort(keys, ObjectStringSorter.createDefaultSorter());
            for (Iterator<String> iterator = keys.iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                if (key.startsWith(LogEntryMapper.ADDITIONAL_ITEM_PREFIX)) {
                    if (additonalItemListStr.length() > 0) {
                        additonalItemListStr.append(", ");
                    }
                    String[] headTail = StringUtil.splitHeadTail(key, "_", false);
                    additonalItemListStr.append(headTail[1]).append("=").append('"').append(attrMap.get(key)).append('"');
                }
            }
            attrMap.put("additionalItemList", additonalItemListStr.toString());
        }
        
        // -- Sort variables by length, e.g. $ipAddress2 must come before $ipAddress (otherwise "2" will not be replaced)
        List<String> keys = new ArrayList<String>(attrMap.keySet());
        Collections.sort(keys, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    // reverse sort order
                    return (o1.length() > o2.length()) ? -1 : (o1.length() == o2.length() ? 0 : 1);
                }
            });
        
        // -- Replace variables
        String result = str;
        int indexOfVariable = result.indexOf(LogEntryMapper.VARIABLE_PREFIX);
        for (Iterator<String> iterator = keys.iterator(); (indexOfVariable != -1) && iterator.hasNext(); ) {
            String key = iterator.next();
            String value = isXml ? StringUtil.encodeCharacterEntities(attrMap.get(key)) : attrMap.get(key);
            value = (value != null) ? value : "";
            result = StringUtil.replace(result, LogEntryMapper.VARIABLE_PREFIX + key, value);
            indexOfVariable = result.indexOf(LogEntryMapper.VARIABLE_PREFIX);
        }
        
        // Note: '$' not allowed in template.
        if (indexOfVariable != -1) {
            boolean dollarSignInvalues = false;
            for (Iterator<String> iterator = keys.iterator(); !dollarSignInvalues && iterator.hasNext(); ) {
                if (attrMap.get(iterator.next()).indexOf(LogEntryMapper.VARIABLE_PREFIX) != -1) {
                    dollarSignInvalues = true;
                }
            }
            if (!dollarSignInvalues) {
                String msg = "The template '" + templateName + "' may contain unused variables because '$' is present. Replaced template: " + result;               
               // Issue Warning message instead of exception until a better solution is implemented
               //TODO Fix issue with two stage variable replacement
               log.warn(msg);
            }
        }
        
        return result;
    }

    
    /**
     * Returns IP-addresses from ipAddress and ipRange to make lookups for.
     * 
     * @return list of IP-addresses, or null if not found.
     */
    public static List<Long> getIpAddressesToDecorate(LogEntry logEntry) {
        List<Long> result = null;
        
        // order: ipAddress, rangeStart+1, rangeStart, rangeEnd
        if (logEntry.getIpAddress() != null) {
            result = new ArrayList<Long>();
            result.add(logEntry.getIpAddress());
        }
        
        if ((logEntry.getIpRangeStart() != null) && (logEntry.getIpRangeStart().longValue() != 0L) && 
                (logEntry.getIpRangeEnd() != null) && (logEntry.getIpRangeEnd().longValue() != 0L)) {
            if (result == null) {
                result = new ArrayList<Long>();
            }
            Long start = logEntry.getIpRangeStart();
            Long end = logEntry.getIpRangeEnd();
            if ((start.longValue() + 1) < end.longValue()) {
                result.add(new Long(start.longValue() + 1));
            }
            result.add(start);
            if (start.longValue() != end.longValue()) {
                result.add(end);
            }
        }
        
        return result;
    }
    
    
    /**
     * Returns a random value in specified range.
     */
    public static Long randomLong(int intervalStart, int intervalEnd) {
        long result = Math.abs(rand.nextLong());
        result = intervalStart + (result % (intervalEnd - intervalStart));
        return new Long(result);
    }

    
    /**
     * Extracts hostname from specified URL, or null if not found or
     * domain name contains illegal characters.
     * <p>
     * Example: "http://www.sitic.se/om-sitic" returns "www.sitic.se", 
     * "" returns null.
     */
    public static String extractHostnameFromUrl(String url) {
        if ((url == null) || (url.trim().length() == 0)) {
            return null;
        }
        
        // Syntax: resource_type://username:password@domain:port/path?query_string#anchor
        String result = url.trim(); 
        
        // remove "protocol://"
        int index = result.indexOf("://");
        if (index != -1) {
            if ((index + 3) < result.length())  {
                result = result.substring(index + 3, result.length());
            } else {
                return null;
            }
        }
        
        // remove username and password (supposed to be encoded)
        int indexAt = result.indexOf("@");
        if (indexAt != -1) {
            int indexSlash = result.indexOf("/");
            if ((indexSlash == -1) || ((indexSlash != -1) && (indexAt < indexSlash))) {
                if ((indexAt + 1) < result.length())  {
                    result = result.substring(indexAt + 1, result.length());
                } else {
                    return null;
                }
            }
        }
        
        // copy to ':' or '/'
        int indexColon = result.indexOf(":");
        int indexSlash = result.indexOf("/");
        if ((indexColon != -1) && (indexSlash != -1)) {
            index = Math.min(indexColon, indexSlash);
        } else if (indexColon != -1) {
            index = indexColon; 
        } else if (indexSlash != -1) {
            index = indexSlash;
        } else {
            index = result.length();
        }
        result = result.substring(0, index);
        
        // exists illegal characters?
        for (int i = 0; i < result.length(); i++) {
            char ch = result.charAt(i);
            if (!((('a' <= ch) && (ch <= 'z')) || (('A' <= ch) && (ch <= 'Z')) || (('0' <= ch) && (ch <= '9')) || (((ch == '-') || (ch == '.')) && (i > 0)))) {
                return null;
            }
        }
        if (result.trim().length() == 0) {
            return null;
        }
        
        return result;
    }


    /**
     * Returns true if specified email addresses are valid, otherwise false.
     * 
     * @param emailAddresses comma separated list of email addresses.
     */
    public static boolean isEmailAddressesValid(String emailAddresses) {
        if (StringUtil.isNullOrEmpty(emailAddresses)) {
            return false;
        }
        
        String[] addresses = emailAddresses.trim().split(",");
        if ((addresses == null) || (addresses.length == 0)) {
            return false;
        }

        String regExp = "(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
        Matcher matcher = Pattern.compile(regExp).matcher("");
        for (int i = 0; i < addresses.length; i++) {
            String address = addresses[i].trim();
            matcher.reset(address);
            if (!matcher.find()) {
                return false;
            }
        }

        return true;
    }

}
