package se.sitic.megatron.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.xbill.DNS.Address;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;


/**
 * Contains static utility-methods for IP-adress conversion, 
 * reverse DNS lookup etc.
 */
public abstract class IpAddressUtil {
    private static final Logger log = Logger.getLogger(IpAddressUtil.class);
    private static final int HOST_NAME_CACHE_MAX_ENTRIES = 2048;
    
    private static Map<Long, String> hostNameCache;
    private static boolean useDnsJava;
    private static boolean useSimpleResolver; 
    private static SimpleResolver simpleResolver;
    
    private static final Matcher IP_ADDRESS_MATCHER = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})").matcher("");
     
    
    static {
        TypedProperties globalProps = AppProperties.getInstance().getGlobalProperties();

        if (globalProps.existsProperty(AppProperties.USE_DNS_JAVA_KEY) && !globalProps.existsProperty(AppProperties.DNS_JAVA_USE_DNS_JAVA_KEY)) {
            // use deprecated property
            useDnsJava = globalProps.getBoolean(AppProperties.USE_DNS_JAVA_KEY, true);
        } else {
            useDnsJava = globalProps.getBoolean(AppProperties.DNS_JAVA_USE_DNS_JAVA_KEY, true);
        }
        if (useDnsJava) {
            String dnsServers = globalProps.getString(AppProperties.DNS_JAVA_DNS_SERVERS_KEY, null);
            if (dnsServers != null) {
                System.getProperties().put("dns.server", dnsServers);
            }
            int timeOut = globalProps.getInt(AppProperties.DNS_JAVA_TIME_OUT_KEY, 4);
            useSimpleResolver = globalProps.getBoolean(AppProperties.DNS_JAVA_USE_SIMPLE_RESOLVER_KEY, true);
            if (useSimpleResolver) {
                try {
                    simpleResolver = new SimpleResolver();
                    simpleResolver.setTimeout(timeOut);
                } catch (UnknownHostException e) {
                    log.error("Cannot initialize DNS service (SimpleResolver). Bad DNS server?", e);
                }
            }
            Lookup.getDefaultResolver().setTimeout(timeOut);
        } 
        
        hostNameCache = Collections.synchronizedMap(new LinkedHashMap<Long, String>(HOST_NAME_CACHE_MAX_ENTRIES, .75F, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
                return size() > HOST_NAME_CACHE_MAX_ENTRIES;
            }
        });
    }

    
    /**
     * Converts specified IP-range as a string to numeric values.
     * <p>
     * Supported formats:<ul>
     * <li>192.121.218.4
     * <li>192.121.218.0/20
     * <li>192.121.x.x
     * <li>192.121.218.0-255
     * <li>192.121.218.0-192.121.220.255
     * </li>
     * 
     * @return IP-range in an array with two elements (start address, end address).
     */
    public static long[] convertIpRange(String ipRange) throws MegatronException {
        // param check
        if (StringUtil.isNullOrEmpty(ipRange) || (ipRange.trim().length() < "0.0.0.0".length())) {
            throw new MegatronException("IP-range is null or too small: " + ipRange);
        }
        
        if (ipRange.toLowerCase().contains("x")) {
            ipRange = expandWildCardOctets(ipRange.toLowerCase().trim(), "x");
        }
        
        long[] result = null;
        try {
            if (ipRange.contains("/")) {
                // format: 192.121.218.0/20
                result = convertBgpPrefix(ipRange, 32);
            } else if (ipRange.contains("-")) {
                result = new long[2];
                String[] headTail = StringUtil.splitHeadTail(ipRange.trim(), "-", false);
                if ((headTail[0].length() == 0) || (headTail[1].length() == 0)) {
                    throw new MegatronException("Invalid format for IP-range: " + ipRange);
                }
                if (headTail[1].contains(".")) {
                    // format: 192.121.218.0-192.121.220.255
                    result[0] = convertIpAddress(headTail[0]);
                    result[1] = convertIpAddress(headTail[1]);
                } else {
                    // format: 192.121.218.0-255
                    String[] ipAddressHeadTail = StringUtil.splitHeadTail(headTail[0].trim(), ".", true);
                    if ((ipAddressHeadTail[0].length() == 0) || (ipAddressHeadTail[1].length() == 0)) {
                        throw new MegatronException("Invalid start IP-address in IP-range: " + ipRange);
                    }
                    String endIpAddress = ipAddressHeadTail[0] + "." + headTail[1];
                    result[0] = convertIpAddress(headTail[0]);
                    result[1] = convertIpAddress(endIpAddress);
                }
            } else {
                // format: 192.121.218.4
                result = new long[2];
                result[0] = convertIpAddress(ipRange);
                result[1] = convertIpAddress(ipRange);
            }
        } catch (UnknownHostException e) {
            throw new MegatronException("Cannot convert IP-address in IP-range: " + ipRange, e);
        }
        
        // check range
        if (result[0] > result[1]) {
            throw new MegatronException("Start address is greater than end address in IP-range: " + ipRange);
        }
        
        return result;
    }

    
    /**
     * As convertIpRange(String) but with the option to expand trailing 
     * zero octets.
     *  
     * @param expandZeroOctets expand trailing zero octets? If true, e.g. 
     *      "202.131.0.0" will be expanded to "202.131.0.0/16".
     */
    public static long[] convertIpRange(String ipRange, boolean expandZeroOctets) throws MegatronException {
        if (!expandZeroOctets) {
            return convertIpRange(ipRange);
        }
        
        // param check
        if (StringUtil.isNullOrEmpty(ipRange) || (ipRange.trim().length() < "0.0.0.0".length())) {
            throw new MegatronException("IP-range is null or too small: " + ipRange);
        }

        if (ipRange.contains(".0")) {
            ipRange = expandWildCardOctets(ipRange, "0");
        } else if (ipRange.contains(".000")) {
            ipRange = expandWildCardOctets(ipRange, "000");
        }
        
        return convertIpRange(ipRange);
    }

    
    /**
     * Converts a BGP-prefix to an IP-range.
     * 
     * @param bgpPrefix may include slash or not. Example: "202.88.48.0/20" or "202.131.0.0".
     *  
     * @return IP-range in an array with two elements (start address, end address).
     */
    public static long[] convertBgpPrefix(String bgpPrefix) throws MegatronException {
        return convertBgpPrefix(bgpPrefix, 24);
    }
    

    /**
     * Converts specified integer IP-address to an InetAddress-object.
     *    
     * @throws UnknownHostException if specified IP-address is invalid.
     */
    public static InetAddress convertIpAddress(long ipAddress) throws UnknownHostException {
        // How to convert ip-number to integer:
        // http://www.aboutmyip.com/AboutMyXApp/IP2Integer.jsp
        
        // ff.ff.ff.ff = 256*16777216 + 256*65536 + 256*256 + 256 = 4311810304
        if ((ipAddress <= 0L) || (ipAddress >= 4311810304L)) {
            throw new UnknownHostException("IP-address out of range: " + ipAddress);
        }
    
        byte[] ip4Address = convertLongAddressToBuf(ipAddress);
        return InetAddress.getByAddress(ip4Address);
    }

    
    /**
     * Converts specified integer IP-address in the format XX.XX.XX.XX to an InetAddress-object.
     *    
     * @throws UnknownHostException if specified IP-address is invalid.
     */
    public static long convertIpAddress(String ipAddress) throws UnknownHostException {
        if (ipAddress == null) {
            throw new UnknownHostException("Invalid IP-address; is null.");
        }
                
        String[] tokens = ipAddress.trim().split("\\.");
        if ((tokens == null) || (tokens.length != 4)) {
            throw new UnknownHostException("Invalid IP-address: " + ipAddress);
        }
        
        long result = 0L;
        long factor = 1;
        for (int i = 3; i >= 0; i--) {
            try {
                int intToken = Integer.parseInt(tokens[i]);
                if ((intToken < 0) || (intToken > 255)) {
                    throw new UnknownHostException("Invalid IP-address: " + ipAddress);
                }
                result += intToken*factor;
                factor = factor << 8;
            } catch (NumberFormatException e) {
                throw new UnknownHostException("Invalid IP-address: " + ipAddress);
            }
        }
        
        return result;
    }


    /**
     * Converts specified integer IP-address to its string representation.
     * 
     * @param includeHostName will do a reverse name lookup and append host 
     *      name to result if lookup successful.
     *    
     * @return ip-adress, or empty string if specified address is invalid or
     *      in case of an exception. Example: "130.239.8.25 (wsunet.umdc.umu.se)". 
     */
    public static String convertIpAddress(long ipAddress, boolean includeHostName) {
        if (ipAddress == 0L) {
            return "";
        }

        String hostAddress = null;
        try {
            hostAddress = convertIpAddressToString(ipAddress);
        } catch (UnknownHostException e) {
            String msg = "Cannot convert IP-address (address probably invalid): " + ipAddress;
            log.warn(msg, e);
            hostAddress = "";
        }
        
        String hostName = null;
        if (includeHostName) {
            hostName = reverseDnsLookupInternal(ipAddress);
        }
        
        StringBuilder result = new StringBuilder(256); 
        result.append(hostAddress);
        if (!StringUtil.isNullOrEmpty(hostName)) {
            result.append(" (").append(hostName).append(")");
        }

        return result.toString();
    }

    
    /**
     * Converts specified integer IP-address and mask the two last octets.
     * <p>Note: This method is not thread-safe.
     * 
     * @return ip-adress, or empty string if specified address is invalid or
     *      in case of an exception. Example: "130.239.x.x". 
     */
    public static String convertAndMaskIpAddress(long ipAddress) {
        String result = convertIpAddress(ipAddress, false);
        if (!StringUtil.isNullOrEmpty(result)) {
            // keep first two numbers in ip-address
            String replaceStr = "$1.$2.x.x";
            IP_ADDRESS_MATCHER.reset(result);
            String resultMasked = IP_ADDRESS_MATCHER.replaceFirst(replaceStr);
            if (result.equals(resultMasked)) {
                // may be some error if not masked at all; mask everything
                resultMasked = "x.x.x.x";
            }
            // log.debug("Masked IP-address: " + result + " --> " + resultMasked);
            result = resultMasked;
        }
        
        return result;
    }
    
    
    /**
     * Returns IP-address as an integer for specified hostname, or 0L
     * if lookup fails.
     */
    public static long dnsLookup(String hostname) {
        long result = 0L;
        String hostAddress = null;

        try {
            hostname = hostname.trim();
            InetAddress inetAddress = null;
            if (useDnsJava) {
                inetAddress = Address.getByName(hostname);
            } else {
                inetAddress = InetAddress.getByName(hostname);
            }
            hostAddress = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            String msg = "DNS lookup failed for hostname: " + hostname;
            log.warn(msg, e);
        }
        
        try {
            if (hostAddress != null) {
                result = convertIpAddress(hostAddress);
            }
        } catch (UnknownHostException e) {
            String msg = "Connot convert IP-address to an integer: " + hostAddress;
            log.warn(msg, e);
        }

        return result;
    }

    
    /**
     * Returns hostname from specified ip-address, or empty string
     * if lookup fails. 
     */
    public static String reverseDnsLookup(long ipAddress) {
        return reverseDnsLookupInternal(ipAddress);
    }

    
    private static String reverseDnsLookupInternal(long ipAddress) {
        String result = null;
        
        // -- Cache lookup
        Long cacheKey = new Long(ipAddress);
        result = hostNameCache.get(cacheKey);
        if (result != null) {
            // log.debug("Cache hit: " + ipAddress + ":" + result);
            return result;
        }
        
        // -- Reverse DNS lookup 
        try {
            if (useDnsJava) {
                if (useSimpleResolver) {
                    result = reverseDnsLookupUsingDnsJavaSimpleResolver(ipAddress);
                } else {
                    result = reverseDnsLookupUsingDnsJavaExtendedResolver(ipAddress);
                }
            } else {
                result = reverseDnsLookupUsingJdk(ipAddress);
            }
            // -- Validate hostname
            validateHostname(result);
        } catch(IOException e) {
            // UnknownHostException, IOException
            result = "";
            if (log.isDebugEnabled()) {
                String ipAddressStr = null;
                try {
                    ipAddressStr = convertIpAddressToString(ipAddress);
                } catch (UnknownHostException e2) {
                    ipAddressStr = Long.toString(ipAddress);
                }
                String msg = "Reverse DNS lookup failed for IP-address: " + ipAddressStr;
                log.info(msg, e);
            } else {
                // Do not include stacktrace; log will be too verbose.
                String msg = "Reverse DNS lookup failed for IP-address: " + ipAddress;
                log.info(msg);
            }
        }
        
        // -- Add to cache (even empty entries)
        if (log.isDebugEnabled()) {
            String ipAddressStr = null;
            try {
                ipAddressStr = convertIpAddressToString(ipAddress);
            } catch (UnknownHostException e2) {
                ipAddressStr = Long.toString(ipAddress);
            }
            log.debug("Adds hostname to cache: " + ipAddress + " [" + ipAddressStr + "]:" + result);
        }
        hostNameCache.put(cacheKey, result);

        return result;
    }

    
    private static String reverseDnsLookupUsingJdk(long ipAddress) throws UnknownHostException {
        // -- Convert ip-address (long) to string
        InetAddress inetAddress = convertIpAddress(ipAddress);
        String hostAddress = inetAddress.getHostAddress();
        if (StringUtil.isNullOrEmpty(hostAddress)) {
            throw new UnknownHostException("Cannot convert IP-address: " + ipAddress);
        }

        // -- Reverse DNS lookup 
        log.debug("Making a reverse DNS lookup for " + ipAddress);
        String result = inetAddress.getHostName();
        log.debug("Lookup result: " + result);
        if (StringUtil.isNullOrEmpty(result) || result.equals(hostAddress)) {
            throw new UnknownHostException("Reverse DNS lookup failed: " + hostAddress);
        }
        return result;
    }

    
    private static String reverseDnsLookupUsingDnsJavaExtendedResolver(long ipAddress) throws UnknownHostException {
        byte[] address = convertLongAddressToBuf(ipAddress);
        Name name = ReverseMap.fromAddress(InetAddress.getByAddress(address));
        Record[] records = new Lookup(name, Type.PTR).run();
        if (records == null) {
            throw new UnknownHostException();
        }
        String result = ((PTRRecord)records[0]).getTarget().toString();
        // remove trailing "."
        result = result.endsWith(".") ? result.substring(0, result.length() - 1) : result;
        return result;
    }

    
    private static String reverseDnsLookupUsingDnsJavaSimpleResolver(long ipAddress) throws IOException {
        String result = null;
        byte[] address = convertLongAddressToBuf(ipAddress);
        Name name = ReverseMap.fromAddress(InetAddress.getByAddress(address));
        Record record = Record.newRecord(name, Type.PTR, DClass.IN);
        Message query = Message.newQuery(record);
        Message response = simpleResolver.send(query);
        Record[] answers = response.getSectionArray(Section.ANSWER);
        if (answers.length != 0) {
            // If PTR-record exists this will be at index 1 or above (more than one PTR-record may exist)
            Record answer = (answers.length > 1) ? answers[1] : answers[0];  
            result = answer.rdataToString();
            // remove trailing "."
            result = result.endsWith(".") ? result.substring(0, result.length() - 1) : result;
        } else {
            throw new IOException("Empty DNS response.");
        }
        return result;
    }
    
    
    private static String convertIpAddressToString(long ipAddress) throws UnknownHostException {
        InetAddress inetAddress = convertIpAddress(ipAddress);
        String result = inetAddress.getHostAddress();
        if (StringUtil.isNullOrEmpty(result)) {
            throw new UnknownHostException("Cannot convert IP-address: " + ipAddress);
        }
        return result;
    }
    
    
    private static byte[] convertLongAddressToBuf(long ipAddress) {
        byte[] result = new byte[4];
        result[0] = (byte)((ipAddress >> 24) & 0xFF);
        result[1] = (byte)((ipAddress >> 16) & 0xFF);
        result[2] = (byte)((ipAddress >> 8) & 0xFF);
        result[3] = (byte)((ipAddress >> 0) & 0xFF);
        return result;
    }

    
    /**
     * Validates specified hostname.
     * 
     * @throws UnknownHostException if hostname contain malicious or invalid content.
     */
    private static void validateHostname(String hostName) throws UnknownHostException {
        // More info: http://en.wikipedia.org/wiki/Hostname
        
        // Valid letters: a..z, A..Z, 0..9, -
        for (int i = 0; i < hostName.length(); i++) {
            char ch = hostName.charAt(i);
            if (!((('a' <= ch) && (ch <= 'z')) || (('A' <= ch) && (ch <= 'Z')) || (('0' <= ch) && (ch <= '9')) || (((ch == '-') || (ch == '.')) && (i > 0)))) {
                throw new UnknownHostException("Host name contains illegal character: " + ch + ". Host name: " + hostName);
            }
        }
        // Validate total length
        if (hostName.length() > 255) {
            throw new UnknownHostException("Host name too long: " + hostName);
        }
        // Exists empty labels?
        if (hostName.indexOf("..") != -1) {
            throw new UnknownHostException("Host name contains an empty label: " + hostName);
        }
        // Validate label length
        String[] labels = hostName.split("\\.");
        for (int i = 0; i < labels.length; i++) {
            if (StringUtil.isNullOrEmpty(labels[i])) {
                throw new UnknownHostException("Host name contains an empty label: " + hostName);
            }
            if (labels[i].length() > 63) {
                throw new UnknownHostException("Host name contains an label that is too long: " + hostName);
            }
        }
    }

    
    /**
     * Expands trailing octets that contains specified wildcard. 
     * A BGP prefix is returned.
     * <p>
     * Example: ("202.131.x.x", "x") returns "202.131.0.0/16", 
     * ("202.131.101.0", "0") returns "202.131.101.0/24".
     */
    private static String expandWildCardOctets(String ipRange, String wildCard) throws MegatronException {
        String result = ipRange;
        int mask = 32;
        boolean wildcardFound = true;
        while (wildcardFound) {
            int lengthBefore = result.length();
            result = StringUtil.removeSuffix(result, "." + wildCard);
            wildcardFound = result.length() < lengthBefore;
            if (wildcardFound) {
                mask -= 8;
            }
        }
        if (mask == 32) {
            result = ipRange;
        } else if ((mask < 8) || result.equals(wildCard)) {
            throw new MegatronException("Cannot expand wildcard IP-range: " + ipRange);
        } else {
            String suffix = StringUtil.nCopyString(".0", (32 - mask) / 8);
            result = result + suffix + "/" + mask;
        }
        
        return result;
    }

    
    private static long[] convertBgpPrefix(String bgpPrefix, int maxMask) throws MegatronException {
        // param check
        if (StringUtil.isNullOrEmpty(bgpPrefix) || (bgpPrefix.trim().length() < "0.0.0.0".length())) {
            throw new MegatronException("BGP-prefix is null or too small: " + bgpPrefix);
        }
        
        long[] result = new long[2];
        bgpPrefix = bgpPrefix.trim(); 
        bgpPrefix = bgpPrefix.endsWith("/") ? bgpPrefix.substring(0, bgpPrefix.length()-1) : bgpPrefix;
        int slashIndex = bgpPrefix.indexOf("/");
        if (slashIndex != -1) {
            // Format: 202.88.48.0/20 (endAddress: 202.88.63.255)
            // 202      . 88       . 48       . 0
            // =
            // 11001010 . 01011000 . 00110000 . 00000000
            // and
            // 11111111 . 11111111 . 11110000 . 00000000 mask with 20 1's; bitMaskHead
            // =
            // 11001010 . 01011000 . 0011
            // or
            //                           1111 . 11111111 bitMaskTail
            //
            // 11001010 . 01011000 . 00111111 . 11111111 (= 202.88.63.255)
            
            String startAddressStr = bgpPrefix.substring(0, slashIndex);
            try {
                result[0] = convertIpAddress(startAddressStr);
            } catch (UnknownHostException e) {
                throw new MegatronException("Cannot convert IP-address in BGP-prefix: " + bgpPrefix, e);
            }
            String maskStr = bgpPrefix.substring(slashIndex+1);
            int mask = 0;
            try {
                mask = Integer.parseInt(maskStr);
            } catch (NumberFormatException e) {
                throw new MegatronException("Cannot parse mask in BGP-prefix: " + maskStr + " (" + bgpPrefix + ").", e);
            }
            if ((mask <= 1) || (mask > maxMask)) {
                throw new MegatronException("Invalid BGP-prefix mask: " + mask + " (" + bgpPrefix + ").");
            }
            long bitMaskHead = ((1L << mask) - 1) << (32 - mask);
            long bitMaskTail = (1L << (32 - mask)) - 1;
            result[1] = (result[0] & bitMaskHead) | bitMaskTail;   
        } else {
            // Format: 202.131.0.0 (endAddress: 202.131.255.255)
            try {
                result[0] = convertIpAddress(bgpPrefix);
            } catch (UnknownHostException e) {
                throw new MegatronException("Cannot convert IP-address in BGP-prefix: " + bgpPrefix, e);
            }
            // add "#" as suffix temporary, e.g. "202.131.0.0#"
            String endAddressStr = bgpPrefix + "#";
            endAddressStr = StringUtil.replace(endAddressStr, ".0.0.0#", ".255.255.255#");
            endAddressStr = StringUtil.replace(endAddressStr, ".0.0#", ".255.255#");
            endAddressStr = StringUtil.replace(endAddressStr, ".0#", ".255#");
            endAddressStr = endAddressStr.substring(0, endAddressStr.length()-1);
            try {
                result[1] = convertIpAddress(endAddressStr);
            } catch (UnknownHostException e) {
                throw new MegatronException("Cannot convert end IP-address in BGP-prefix: " + bgpPrefix, e);
            }
        }
        return result;
    }

}
