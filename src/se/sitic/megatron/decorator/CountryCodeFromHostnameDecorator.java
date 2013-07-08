package se.sitic.megatron.decorator;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.StringUtil;


/**
 * Adds country code if missing and hostname exists.
 */
public class CountryCodeFromHostnameDecorator implements IDecorator {
    private static final Logger log = Logger.getLogger(CountryCodeFromHostnameDecorator.class);

    private Set<String> countryCodesToAdd;
    private long noOfAssigments;
    private long noOfAssigments2;
    

    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();

        String[] ccArray = props.getStringListFromCommaSeparatedValue(AppProperties.DECORATOR_COUNTRY_CODES_TO_ADD_KEY, null, true);
        if ((ccArray != null) && (ccArray.length > 0)) {
            countryCodesToAdd = new HashSet<String>();
            for (int i = 0; i < ccArray.length; i++) {
                countryCodesToAdd.add(ccArray[i].toUpperCase());
            }
        }
    }

    
    public void execute(LogEntry logEntry) throws MegatronException {
        if ((logEntry.getCountryCode() == null) && (logEntry.getHostname() != null)) {
            String countryCode = extractCountryCodeTld(logEntry.getHostname());
            if (countryCode != null) {
                countryCode = countryCode.toUpperCase();
                if ((countryCodesToAdd == null) || countryCodesToAdd.contains(countryCode)) {
                    logEntry.setCountryCode(countryCode);
                    ++noOfAssigments;
                }
            }
        }

        if ((logEntry.getCountryCode2() == null) && (logEntry.getHostname2() != null)) {
            String countryCode = extractCountryCodeTld(logEntry.getHostname2());
            if (countryCode != null) {
                countryCode = countryCode.toUpperCase();
                if ((countryCodesToAdd == null) || countryCodesToAdd.contains(countryCode)) {
                    logEntry.setCountryCode2(countryCode);
                    ++noOfAssigments2;
                }
            }
        }
    }
    
    
    public void close() {
        long noOfTotalAssigments = noOfAssigments + noOfAssigments2;
        log.info("No. of assignments (hostname --> country code): " + noOfTotalAssigments + " (" + noOfAssigments + "+" + noOfAssigments2 + ")."); 
    }

    
    /**
     * Extracts country code TLD from specified hostname.
     * 
     * @return country code TLD, or null. Case same as in hostname. 
     */
    private String extractCountryCodeTld(String hostname) {
        // No generic TLD with 2 letters exists:
        // http://en.wikipedia.org/wiki/List_of_Internet_top-level_domains
        String[] headTail = StringUtil.splitHeadTail(hostname, ".", true);
        return ((headTail == null) || (headTail[1].length() != 2)) ? null : headTail[1];
    }

}
