package se.sitic.megatron.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.decorator.CountryCodeDecorator;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.StringUtil;


/**
 * Filter log entries using the country code and TLD in the hostname.
 * <p> 
 * If country code is missing, CountryCodeDecorator will be used to add one.
 */
public class CountryCodeFilter implements ILogEntryFilter {
    private static final Logger log = Logger.getLogger(CountryCodeFilter.class);    
    private static final String COUNTRY_CODE_IN_HOSTNAME_REG_EXP = "\\.([a-zA-Z]+?)$";

    private JobContext jobContext;
    private List<String> excludeList;
    private List<String> includeList;
    private Matcher hostnameMatcher;
    private String organizationToFilter;
    private CountryCodeDecorator countryCodeDecorator;
    private long noOfFilteredEntries;
    private long noOfDecoratedEntries;
    

    public void init(JobContext jobContext) throws MegatronException {
        this.jobContext = jobContext;
        TypedProperties props = jobContext.getProps();
        
        String propertyKey = AppProperties.FILTER_EXCLUDE_COUNTRY_CODES_KEY;
        String[] countryCodes = props.getStringListFromCommaSeparatedValue(propertyKey, null, true);
        if ((countryCodes != null) && (countryCodes.length > 0)) {
            log.info("Using exclude country codes filter: " + Arrays.asList(countryCodes));
            excludeList = new ArrayList<String>();
            for (int i = 0; i < countryCodes.length; i++) {
                excludeList.add(countryCodes[i].toUpperCase());
            }
        }

        propertyKey = AppProperties.FILTER_INCLUDE_COUNTRY_CODES_KEY;
        countryCodes = props.getStringListFromCommaSeparatedValue(propertyKey, null, true);
        if ((countryCodes != null) && (countryCodes.length > 0)) {
            log.info("Using include country codes filter: " + Arrays.asList(countryCodes));
            includeList = new ArrayList<String>();
            for (int i = 0; i < countryCodes.length; i++) {
                includeList.add(countryCodes[i].toUpperCase());
            }
        }

        organizationToFilter = props.getString(AppProperties.FILTER_COUNTRY_CODE_ORGANIZATION_KEY, "primary");
        if (!(organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_PRIMARY) || organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_SECONDARY) || 
                organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_BOTH))) {
            throw new MegatronException("Invalid value for property " + AppProperties.FILTER_COUNTRY_CODE_ORGANIZATION_KEY + ": " + organizationToFilter);
        }
        
        if ((excludeList == null) && (includeList == null)) {
            String msg = "No country code list defined; both " + AppProperties.FILTER_EXCLUDE_COUNTRY_CODES_KEY + " and " + 
                AppProperties.FILTER_INCLUDE_COUNTRY_CODES_KEY + " are undefined.";
            throw new MegatronException(msg);
        }
        if ((excludeList != null) && (includeList != null)) {
            String msg = "Only one of the property excludeCountryCodes or excludeCountryCodes can be defined -- not both."; 
            throw new MegatronException(msg);
        }
        
        hostnameMatcher = Pattern.compile(COUNTRY_CODE_IN_HOSTNAME_REG_EXP).matcher("");
    }


    public boolean accept(LogEntry logEntry) throws MegatronException {
        boolean result = false;
        
        // decorate countryCode if missing
        if ((logEntry.getCountryCode() == null) && (logEntry.getCountryCode2() == null)) {
            if (countryCodeDecorator == null) {
                countryCodeDecorator = new CountryCodeDecorator();
                countryCodeDecorator.init(jobContext);
            }
            countryCodeDecorator.execute(logEntry);
            ++noOfDecoratedEntries;
        }
        
        if (excludeList != null) {
            // countryCode
            if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_PRIMARY)) {
                result = !excludeList.contains(convertCountryCode(logEntry.getCountryCode()));
            } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_SECONDARY)) {
                result = !excludeList.contains(convertCountryCode(logEntry.getCountryCode2()));
            } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_BOTH)) {
                result = !(excludeList.contains(convertCountryCode(logEntry.getCountryCode())) || excludeList.contains(convertCountryCode(logEntry.getCountryCode2())));
            }
            
            // hostname
            if (result) {
                if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_PRIMARY)) {
                    result = !excludeList.contains(extractCountryCodeFromHostname(logEntry.getHostname()));
                } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_SECONDARY)) {
                    result = !excludeList.contains(extractCountryCodeFromHostname(logEntry.getHostname2()));
                } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_BOTH)) {
                    result = !(excludeList.contains(extractCountryCodeFromHostname(logEntry.getHostname())) || excludeList.contains(extractCountryCodeFromHostname(logEntry.getHostname2())));
                }
            }
        } else {
            // countryCode
            if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_PRIMARY)) {
                result = includeList.contains(convertCountryCode(logEntry.getCountryCode()));
            } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_SECONDARY)) {
                result = includeList.contains(convertCountryCode(logEntry.getCountryCode2()));
            } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_BOTH)) {
                result = includeList.contains(convertCountryCode(logEntry.getCountryCode())) || includeList.contains(convertCountryCode(logEntry.getCountryCode2()));
            }
            
            // hostname
            if (!result) {
                if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_PRIMARY)) {
                    result = includeList.contains(extractCountryCodeFromHostname(logEntry.getHostname()));
                } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_SECONDARY)) {
                    result = includeList.contains(extractCountryCodeFromHostname(logEntry.getHostname2()));
                } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_BOTH)) {
                    result = includeList.contains(extractCountryCodeFromHostname(logEntry.getHostname())) || includeList.contains(extractCountryCodeFromHostname(logEntry.getHostname2()));
                }
            }
        }
        
        if (!result) {
            ++noOfFilteredEntries;
            // log.debug("Log entry filtered out: " + logEntry.getId());
        }
        
        return result;
    }

    
    public void close() throws MegatronException {
        if (countryCodeDecorator != null) {
            countryCodeDecorator.close();
        }
        
        log.info("No. of filtered log entries (CountryCodeFilter): " + noOfFilteredEntries);
        log.info("No. of entries for which country code have been added: " + noOfDecoratedEntries);
    }
    
    
    private String convertCountryCode(String countryCode) {
        return StringUtil.isNullOrEmpty(countryCode) ? "-" : countryCode;
    }
    
    
    @SuppressWarnings("null")
    private String extractCountryCodeFromHostname(String hostname) {
        if (StringUtil.isNullOrEmpty(hostname)) {
            return "-";
        }
        
        String result = null;
        hostnameMatcher.reset(hostname);
        if (hostnameMatcher.find() && (hostnameMatcher.groupCount() == 1)) {
            result = hostnameMatcher.group(1);
        }
        
        return StringUtil.isNullOrEmpty(result) ? "-" : result.toUpperCase();
    }

}
