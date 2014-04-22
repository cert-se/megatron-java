package se.sitic.megatron.parser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.AttributeValueRewriter;
import se.sitic.megatron.core.ConversionException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.AppUtil;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.IpAddressUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Maps a hash-map to a LogEntry-object, and vice versa.
 */
public class LogEntryMapper {
    /** Values that are considred null in an integer field. */
    private static final String[] INTEGER_NULL_VALUES = { "0", "-", "\"\"", "''" }; 

    // Common item attributes
    // IP_RANGE is expanded to IP_RANGE_START and IP_RANGE_END when parsed
    private static final String LOG_TIMESTAMP = "logTimestamp";
    private static final String URL = "url";
    private static final String IP_RANGE = "ipRange";
    private static final String IP_RANGE_START = "ipRangeStart";
    private static final String IP_RANGE_END = "ipRangeEnd";

    // Primary host attributes
    private static final String IP_ADDRESS = "ipAddress";
    private static final String HOSTNAME = "hostname";        
    private static final String PORT = "port";  
    private static final String ASN = "asn";
    private static final String COUNTRY_CODE = "countryCode";

    // Secondary host attributes
    private static final String IP_ADDRESS2 = "ipAddress2";
    private static final String HOSTNAME2 = "hostname2";        
    private static final String PORT2 = "port2";  
    private static final String ASN2 = "asn2";
    private static final String COUNTRY_CODE2 = "countryCode2";

    // Attributes used only when created from LogEntry-object
    private static final String ORIGINAL_LOG_ENTRY = "originalLogEntry";
    private static final String LOG_ENTRY_ID = "logEntryid";
    private static final String CREATED = "created";
    private static final String ORGANIZATION_NAME = "organizationName";
    private static final String ORGANIZATION_EMAIL_ADDRESSES = "organizationEmailAddresses";
    private static final String ORGANIZATION_NAME2 = "organizationName2";
    private static final String ORGANIZATION_EMAIL_ADDRESSES2 = "organizationEmailAddresses2";
    
    public static final String[] EXPRESSION_VARIABLES = {
        LOG_TIMESTAMP, URL, IP_RANGE, 
        IP_ADDRESS, HOSTNAME, PORT, ASN, COUNTRY_CODE,
        IP_ADDRESS2, HOSTNAME2, PORT2, ASN2, COUNTRY_CODE2,
    };

    public static final String VARIABLE_PREFIX = "$";
    public static final String ADDITIONAL_ITEM_PREFIX = "additionalItem_";
    public static final String FREE_TEXT_PREFIX = "freeText";

    private TypedProperties props;
    private AttributeValueRewriter rewriter;
    private Map<String, String> attrMap;
    private String timestampFormat;


    public LogEntryMapper(TypedProperties props, Map<String, String> attributeMap) {
        this.props = props;
        this.attrMap = new HashMap<String, String>(attributeMap);
    }

    
    public LogEntryMapper(TypedProperties props, AttributeValueRewriter rewriter, LogEntry logEntry) {
        this.props = props;
        this.rewriter = rewriter;
        this.attrMap = createAttributeMap(logEntry);
    }

    
    public LogEntryMapper(TypedProperties props, LogEntry logEntry) {
        this(props, null, logEntry);
    }
    
    
    public LogEntry createLogEntry() throws ConversionException {
        LogEntry result = new LogEntry();

        String field = null;
        try {
            // -- common
            field = LOG_TIMESTAMP;
            if (!isNullOrEmpty(attrMap.get(field))) {
                String format = props.getString(AppProperties.PARSER_TIME_STAMP_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss z");
                if (format.equalsIgnoreCase(Constants.TIME_STAMP_FORMAT_EPOCH_IN_SEC) || format.equalsIgnoreCase(Constants.TIME_STAMP_FORMAT_EPOCH_IN_MS) || 
                        format.equalsIgnoreCase(Constants.TIME_STAMP_FORMAT_WINDOWS_EPOCH)) {
                    long epoch = 0L;
                    try {
                        epoch = Long.parseLong(attrMap.get(field));
                    } catch (NumberFormatException e) {
                        String msg = "Cannot parse time-stamp in epoch format: '" + attrMap.get(field) + "'.";
                        throw new ConversionException(msg, e);
                    }
                    if (format.equalsIgnoreCase(Constants.TIME_STAMP_FORMAT_EPOCH_IN_MS)) {
                        epoch = SqlUtil.convertTimestampToSec(epoch);
                    } else if (format.equalsIgnoreCase(Constants.TIME_STAMP_FORMAT_WINDOWS_EPOCH)) {
                        // convert to Unix epoch in seconds
                        long epochDiff = 11644473600L;
                        epoch = (epoch / 10000000L) - epochDiff;
                    }
                    // sanity check
                    if (epoch < 946684800L) {
                        String msg = "Time-stamp not in epoch format; value is before 2000-01-01: " + epoch;
                        throw new ConversionException(msg);
                    }
                    String timeZone = props.getString(AppProperties.PARSER_DEFAULT_TIME_ZONE_KEY, null);
                    if (timeZone != null) {
                        TimeZone tz = TimeZone.getTimeZone(timeZone);
                        epoch += Math.round(tz.getOffset(epoch) / 1000d);
                    }
                    result.setLogTimestamp(epoch);
                } else {
                    // add year if missing in format (otherwise year will be 1970) 
                    String year = "";
                    if (format.indexOf('y') == -1) {
                        format = "yyyy " + format;
                        year = Calendar.getInstance().get(Calendar.YEAR) + " ";
                    }
                    // add time zone if specified 
                    String timeZone = props.getString(AppProperties.PARSER_DEFAULT_TIME_ZONE_KEY, "");
                    if ((timeZone.length() != 0) && !format.toLowerCase().endsWith("z")) {
                        format = format + "z";
                    }
                    StringBuilder timestamp = new StringBuilder(32); 
                    timestamp.append(year).append(attrMap.get(field)).append(timeZone);
                    boolean addCurrentDate = props.getBoolean(AppProperties.PARSER_ADD_CURRENT_DATE_TO_TIMESTAMP_KEY, false);
                    if (addCurrentDate) {
                        String dateStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, new Date()) + " ";
                        timestamp.insert(0, dateStr);
                    }
                    try {
                        Date date = DateUtil.parseDateTime(format, timestamp.toString());
                        result.setLogTimestamp(SqlUtil.convertTimestampToSec(date.getTime()));
                    } catch (ParseException e) {
                        String msg = "Cannot parse time-stamp: '" + timestamp + "'. Expected format: '" + format + "'.";
                        throw new ConversionException(msg, e);
                    }
                }
            }
            field = URL;
            if (!isNullOrEmpty(attrMap.get(field))) {
                result.setUrl(attrMap.get(field));
            }
            field = IP_RANGE;
            if (!isNullOrEmpty(attrMap.get(field))) {
                boolean expandZeroOctets = props.getBoolean(AppProperties.PARSER_EXPAND_IP_RANGE_WITH_ZERO_OCTETS_KEY, false);
                // IP_RANGE is expanded to IP_RANGE_START and IP_RANGE_END 
                long ipRange[] = IpAddressUtil.convertIpRange(attrMap.get(field), expandZeroOctets);
                result.setIpRangeStart(ipRange[0]);
                result.setIpRangeEnd(ipRange[1]);
                // Set ipAddress to start address. Makes manual searches easier and simplifies DbManager.existsMailForIp
                result.setIpAddress(ipRange[0]);
            }
            
            // -- Primary host
            field = IP_ADDRESS;
            if (!isNullOrEmpty(attrMap.get(field), "0.0.0.0")) {
                result.setIpAddress(IpAddressUtil.convertIpAddress(attrMap.get(field)));
            }
            field = HOSTNAME;
            if (!isNullOrEmpty(attrMap.get(field))) {
                result.setHostname(attrMap.get(field));
            }
            field = PORT;
            if (!isNullOrEmpty(attrMap.get(field), INTEGER_NULL_VALUES)) {
                result.setPort(new Integer(attrMap.get(field)));
            }
            field = ASN;
            if (!isNullOrEmpty(attrMap.get(field), INTEGER_NULL_VALUES)) {
                result.setAsn(Long.parseLong(attrMap.get(field)));
            }
            field = COUNTRY_CODE;
            if (!isNullOrEmpty(attrMap.get(field), "-")) {
                result.setCountryCode(attrMap.get(field));
            }

            // -- Secondary host
            field = IP_ADDRESS2;
            if (!isNullOrEmpty(attrMap.get(field), "0.0.0.0")) {
                result.setIpAddress2(IpAddressUtil.convertIpAddress(attrMap.get(field)));
            }
            field = HOSTNAME2;
            if (!isNullOrEmpty(attrMap.get(field))) {
                result.setHostname2(attrMap.get(field));
            }
            field = PORT2;
            if (!isNullOrEmpty(attrMap.get(field), INTEGER_NULL_VALUES)) {
                result.setPort2(new Integer(attrMap.get(field)));
            }
            field = ASN2;
            if (!isNullOrEmpty(attrMap.get(field), INTEGER_NULL_VALUES)) {
                result.setAsn2(Long.parseLong(attrMap.get(field)));
            }
            field = COUNTRY_CODE2;
            if (!isNullOrEmpty(attrMap.get(field), "-")) {
                result.setCountryCode2(attrMap.get(field));
            }
        } catch (Exception e) {
            if (e instanceof ConversionException) {
                throw (ConversionException)e;
            }
            // UnknownHostException, NumberFormatException 
            String msg = "Cannot convert field: " + field + "='" + attrMap.get(field) + "'.";
            throw new ConversionException(msg, e);
        }
        
        // -- additionalItems
        Map<String, String> additionalItems = new HashMap<String, String>();
        for (Iterator<String> iterator = attrMap.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            if (key.startsWith(ADDITIONAL_ITEM_PREFIX)) {
                String[] headTail = StringUtil.splitHeadTail(key, "_", false);
                additionalItems.put(headTail[1], attrMap.get(key));
            }
        }
        result.setAdditionalItems(additionalItems);

        // -- freeTexts
        List<String> freeTexts = new ArrayList<String>();
        int freeTextIndex = 0; 
        boolean freeTextFound = true; 
        while (freeTextFound) {
            String key = FREE_TEXT_PREFIX + (freeTextIndex++);
            if (attrMap.get(key) != null) {
                freeTexts.add(attrMap.get(key));
            } else {
                freeTextFound = false;
            }
        }
        result.setFreeTexts(freeTexts);
        
        // -- Check fields
        // check freeText index
        if ((attrMap.get(FREE_TEXT_PREFIX + "0") == null) && (attrMap.get(FREE_TEXT_PREFIX + "1") != null)) {
            String msg = "Cannot convert free text; freeText1 exists, but not freeText0. Index for freetext starts at 0.";
            throw new ConversionException(msg);
        }
        // check country code
        if ((result.getCountryCode() != null) && (result.getCountryCode().length() != 2)) {
            String msg = "Country code must be 2 characters long. countryCode='" + result.getCountryCode() + "'.";
            throw new ConversionException(msg);
        }
        if ((result.getCountryCode2() != null) && (result.getCountryCode2().length() != 2)) {
            String msg = "Country code must be 2 characters long. countryCode2='" + result.getCountryCode2() + "'.";
            throw new ConversionException(msg);
        }
        
        // -- Convert fields
        if (result.getCountryCode() != null) {
            result.setCountryCode(result.getCountryCode().toUpperCase());
        }
        if (result.getCountryCode2() != null) {
            result.setCountryCode2(result.getCountryCode2().toUpperCase());
        }
        
        return result;
    }

    
    /**
     * Expands variables in specified template. Rewrites, if any rewriters
     * are defined, attribute values before expansion. 
     * 
     * @throws ConversionException if not all variables have been expanded.
     */
    public String replaceVariables(String template, boolean isXml, String templateName) throws ConversionException {
        if (rewriter != null) {
            rewriter.rewrite(attrMap);
        }
        return AppUtil.replaceVariables(template, attrMap, isXml, templateName);
    }

    
    /**
     * Returns attribute value for specified key.
     * 
     * @param key attribute namn without variable prefix, e.g. "additionalItem_httpStatusCode".
     */
    public String getAttribute(String key) {
        return attrMap.get(key);
    }
    
    
    private boolean isNullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }

    
    private boolean isNullOrEmpty(String str, String nullValue) {
        return str == null || str.length() == 0 || nullValue.equals(str);
    }

    
    private boolean isNullOrEmpty(String str, String[] nullValues) {
        if ((str == null) || (str.length() == 0)) {
            return true;
        }
        
        for (int i = 0; i < nullValues.length; i++) {
            if (str.equals(nullValues[i])) {
                return true;
            }
        }
        
        return false;
    }


    private Map<String, String> createAttributeMap(LogEntry logEntry) {
        Map<String, String> result = new HashMap<String, String>();

        // -- common
        addString(result, URL, logEntry.getUrl());
        addIpAddress(result, IP_RANGE_START, logEntry.getIpRangeStart());
        addIpAddress(result, IP_RANGE_END, logEntry.getIpRangeEnd());
        result.put(LOG_ENTRY_ID, "" + logEntry.getId());
        addTimestamp(result, CREATED, logEntry.getCreated());
        addTimestamp(result, LOG_TIMESTAMP, logEntry.getLogTimestamp());

        if (logEntry.getOriginalLogEntry() != null) {
            addString(result, ORIGINAL_LOG_ENTRY, logEntry.getOriginalLogEntry().getEntry());
        } else {
            addString(result, ORIGINAL_LOG_ENTRY, null);
        }
        if (logEntry.getOrganization() != null) {
            addString(result, ORGANIZATION_NAME, logEntry.getOrganization().getName());
            addString(result, ORGANIZATION_EMAIL_ADDRESSES, logEntry.getOrganization().getEmailAddresses());
        } else {
            addString(result, ORGANIZATION_NAME, null);
            addString(result, ORGANIZATION_EMAIL_ADDRESSES, null);
        }
        if (logEntry.getOrganization2() != null) {
            addString(result, ORGANIZATION_NAME2, logEntry.getOrganization2().getName());
            addString(result, ORGANIZATION_EMAIL_ADDRESSES2, logEntry.getOrganization2().getEmailAddresses());
        } else {
            addString(result, ORGANIZATION_NAME2, null);
            addString(result, ORGANIZATION_EMAIL_ADDRESSES2, null);
        }
        
        // -- primary host
        addIpAddress(result, IP_ADDRESS, logEntry.getIpAddress());
        addString(result, HOSTNAME, logEntry.getHostname());
        addInteger(result, PORT, logEntry.getPort());
        addLong(result, ASN, logEntry.getAsn());
        addString(result, COUNTRY_CODE, logEntry.getCountryCode());

        // -- secondary host
        addIpAddress(result, IP_ADDRESS2, logEntry.getIpAddress2());
        addString(result, HOSTNAME2, logEntry.getHostname2());
        addInteger(result, PORT2, logEntry.getPort2());
        addLong(result, ASN2, logEntry.getAsn2());
        addString(result, COUNTRY_CODE2, logEntry.getCountryCode2());

        // -- additionalItems
        String lineRegExp = props.getString(AppProperties.PARSER_LINE_REG_EXP_KEY, ""); 
        Map<String, String> items = logEntry.getAdditionalItems();
        // decorators may add additional items without any corresponding variables
        if (lineRegExp.contains(VARIABLE_PREFIX + ADDITIONAL_ITEM_PREFIX) || ((items != null) && !items.isEmpty())) {
            if (items != null) {
                for (Iterator<String> iterator = items.keySet().iterator(); iterator.hasNext(); ) {
                    String key = iterator.next();
                    addString(result, ADDITIONAL_ITEM_PREFIX + key,  items.get(key));
                }
            }
            addEmptyAdditionalItems(result);
        }
        
        // -- freeTexts
        if (lineRegExp.contains(VARIABLE_PREFIX + FREE_TEXT_PREFIX)) {
            List<String> freeTexts = logEntry.getFreeTexts();
            if (freeTexts != null) {
                int index = 0;
                for (Iterator<String> iterator = freeTexts.iterator(); iterator.hasNext(); ) {
                    addString(result, FREE_TEXT_PREFIX + (index++),  iterator.next());
                }
            }
            // Empty free text values are stored in the database, otherwise list order
            // cannot be preserved for multiple free texts in case some field is empty.
            // Thus, no addEmptyFreeTexts-method is needed.  
            // This use case is ignored: a free text field is added to an existing 
            // configuration, and the new template uses this variable on an old job 
            // whithout this free text field.  
        }
        
        return result;
    }

    
    /**
     * Adds empty additional items to specified map that are defined in the
     * job type, but does not exist in the map. Empty or null values for 
     * additional items are not stored in the database. All variables in the
     * job type may be used in a template, and no values can be null.
     */
    private void addEmptyAdditionalItems(Map<String, String> attributeMap) {
        List<String> vars = LineExpression.getAdditionalItemVariables(props);
        for (Iterator<String> iterator = vars.iterator(); iterator.hasNext(); ) {
            String var = iterator.next();
            if (!attributeMap.containsKey(var)) {
                attributeMap.put(var, "");
            }
        }
    }
    
    
    private void addString(Map<String, String> map, String key, String str) {
        String value = (str != null) ? str : ""; 
        map.put(key, value);
    }

    
    private void addIpAddress(Map<String, String> map, String key, Long ipAddress) {
        String value = ((ipAddress != null) && (ipAddress > 0)) ? IpAddressUtil.convertIpAddress(ipAddress, false) : "";
        map.put(key, value);
    }
    
    
    private void addInteger(Map<String, String> map, String key, Integer num) {
        String value = (num != null) ? num.toString() : ""; 
        map.put(key, value);
    }
    
    
    private void addLong(Map<String, String> map, String key, Long num) {
        String value = ((num != null) && (num >= 0)) ? "" + num : "";
        map.put(key, value);
    }

    
    private void addTimestamp(Map<String, String> map, String key, Long timestampInUtc) {
        if (timestampFormat == null) {
            timestampFormat = props.getString(AppProperties.EXPORT_TIMESTAMP_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss z");
        }
        Date date = SqlUtil.convertTimestamp(timestampInUtc);
        String value = ((timestampInUtc != null) && (timestampInUtc > 0)) ? DateUtil.formatDateTime(timestampFormat, date) : "";
        map.put(key, value);
    }

}
