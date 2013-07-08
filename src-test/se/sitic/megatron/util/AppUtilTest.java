package se.sitic.megatron.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;


/**
 * JUnit 4 test-case.
 */
public class AppUtilTest {
    private TypedProperties props;


    public AppUtilTest() {
        // empty
    }


    @Before
    public void initProps() {
        Map<String, String> propMap = new HashMap<String, String>();
        propMap.put("regExp.hubba", "hubba-regExp");
        propMap.put("regExp.bubba", "bubba-regExp");
        props = new TypedProperties(propMap, null);
    }


    @Test
    public void expandRegExpTest() {
        final String[][] expandResults = {
                { "xxxxx", "xxxxx" },
                { "@regExp.hubba@", "hubba-regExp" },
                { "@hubba@", "hubba-regExp" },
                { "\\@hubba\\@", "\\@hubba\\@" },
                { "xx@hubba@xx@bubba@xx", "xxhubba-regExpxxbubba-regExpxx" },
                { "@BUBBA@", "@BUBBA@" },
                { "@@", "@@" },
                { "@", "@" },
                { "", "" },
        };

        for (int i = 0; i < expandResults.length; i++) {
            String regExp = expandResults[i][0];
            String expandedRegExp = expandResults[i][1];
            assertEquals(expandedRegExp, AppUtil.expandRegExp(props, regExp));
        }

        assertEquals(null, AppUtil.expandRegExp(props, null));
    }


    @Test
    public void hashStringTest() {
        final String[][] testResultArray = {
                { null, "d41d8cd98f00b204e9800998ecf8427e" },
                { "", "d41d8cd98f00b204e9800998ecf8427e" },
                { "x", "9dd4e461268c8034f5c8564e155c67a6" },
                { "xxxx", "ea416ed0759d46a8de58f63a59077499" },
        };

        for (int i = 0; i < testResultArray.length; i++) {
            String strToHash = testResultArray[i][0];
            String hashedStr = testResultArray[i][1];
            assertEquals(hashedStr, AppUtil.hashString(strToHash));
        }
    }
    
    
    @Test
    public void getIpAddressesToDecorateTest() {
        LogEntry logEntry = new LogEntry();
        List<Long> ipAddresses = null;
        
        ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
        assertEquals(null, ipAddresses);
        
        logEntry.setIpAddress(1L);
        ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
        Long[] expected1 = { 1L }; 
        assertEquals(Arrays.asList(expected1), ipAddresses);
        
        logEntry.setIpAddress(1L);
        logEntry.setIpRangeStart(2L);
        logEntry.setIpRangeEnd(2L);
        ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
        Long[] expected2 = { 1L, 2L }; 
        assertEquals(Arrays.asList(expected2), ipAddresses);

        logEntry.setIpAddress(1L);
        logEntry.setIpRangeStart(2L);
        logEntry.setIpRangeEnd(3L);
        ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
        Long[] expected3 = { 1L, 2L, 3L }; 
        assertEquals(Arrays.asList(expected3), ipAddresses);

        logEntry.setIpAddress(1L);
        logEntry.setIpRangeStart(2L);
        logEntry.setIpRangeEnd(4L);
        ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
        Long[] expected4 = { 1L, 3L, 2L, 4L }; 
        assertEquals(Arrays.asList(expected4), ipAddresses);

        logEntry.setIpAddress(1L);
        logEntry.setIpRangeStart(50L);
        logEntry.setIpRangeEnd(100L);
        ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
        Long[] expected5 = { 1L, 51L, 50L, 100L }; 
        assertEquals(Arrays.asList(expected5), ipAddresses);
    }

    
    @Test
    public void extractHostnameFromUrlTest() {
        final String[][] testResultArray = {
                { "http://www.sitic.se/", "www.sitic.se" },
                { "http://www.sitic.se", "www.sitic.se" },
                { "http://www.Sitic.se", "www.Sitic.se" },
                { "http://www.sitic.se/foo?param1=value1&param2=valueWithAt@", "www.sitic.se" },
                { "http://www.sitic.se/foo?param1=value1&param2=valueWithAt@:hubba", "www.sitic.se" },
                { "http://www.sitic.se:8080/foo?param1=value1&param2=valueWithAt@:hubba", "www.sitic.se" },
                { "http://www.sitic.se/foo?param1=value1&param2=valueWithAt@#anchor", "www.sitic.se" },
                { "http://www.sitic.se:8008", "www.sitic.se" },
                { "http://www.sitic.se:8008/", "www.sitic.se" },
                { "http://foo:bar@www.sitic.se/", "www.sitic.se" },
                { "http://foo:bar@www.sitic.se", "www.sitic.se" },
                { "http://foo:bar@www.sitic.se:8080/foo.html", "www.sitic.se" },
                { "ftp://ftp.sitic.se/foo/bar", "ftp.sitic.se" },
                { "www.sitic.se", "www.sitic.se" },
                { "www.sitic.se/foo.html", "www.sitic.se" },
                { "www.sitic.se/foo//bar.html", "www.sitic.se" },
                { "www.sitic.se/foo?param1=value1&param2=value2", "www.sitic.se" },
                { "www.sitic.se:8080/foo?param1=value1&param2=value2", "www.sitic.se" },
                { "http://X", "X" },
                { "X", "X" },
                { "http://www.sitic.se:", "www.sitic.se" },  // invalid URL but valid domain name
                { "www.sitic.se:", "www.sitic.se" },         // invalid URL but valid domain name 
                
                // invalid domains
                { null, null },
                { "", null },
                { "      ", null },
                { "http://", null },
                { "http:///", null },
                { "http://foo:bar@", null },
                { "www.sitic.se@", null },
        };

        for (int i = 0; i < testResultArray.length; i++) {
            String url = testResultArray[i][0];
            String hostname = testResultArray[i][1];
            assertEquals(hostname, AppUtil.extractHostnameFromUrl(url));
        }
    }

}
