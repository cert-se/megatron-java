package se.sitic.megatron.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.Constants;


/**
 * Filter log entries using the ASN attribute.
 */
public class AsnFilter implements ILogEntryFilter {
    private static final Logger log = Logger.getLogger(AsnFilter.class);

    private List<String> excludeList;
    private List<String> includeList;
    private String organizationToFilter;
    private long noOfFilteredEntries;

    
    public AsnFilter() {
        // empty
    }

    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        
        String propertyKey = AppProperties.FILTER_EXCLUDE_AS_NUMBERS_KEY;
        String[] asNumbers = props.getStringListFromCommaSeparatedValue(propertyKey, null, true);
        if ((asNumbers != null) && (asNumbers.length > 0)) {
            log.info("Using exclude AS number filter: " + Arrays.asList(asNumbers));
            excludeList = new ArrayList<String>();
            for (int i = 0; i < asNumbers.length; i++) {
                excludeList.add(asNumbers[i]);
            }
        }

        propertyKey = AppProperties.FILTER_INCLUDE_AS_NUMBERS_KEY;
        asNumbers = props.getStringListFromCommaSeparatedValue(propertyKey, null, true);
        if ((asNumbers != null) && (asNumbers.length > 0)) {
            log.info("Using include AS number filter: " + Arrays.asList(asNumbers));
            includeList = new ArrayList<String>();
            for (int i = 0; i < asNumbers.length; i++) {
                includeList.add(asNumbers[i]);
            }
        }

        organizationToFilter = props.getString(AppProperties.FILTER_ASN_ORGANIZATION_KEY, "primary");
        if (!(organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_PRIMARY) || organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_SECONDARY) || 
                organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_BOTH))) {
            throw new MegatronException("Invalid value for property " + AppProperties.FILTER_ASN_ORGANIZATION_KEY + ": " + organizationToFilter);
        }
        
        if ((excludeList == null) && (includeList == null)) {
            String msg = "No AS number list defined; both " + AppProperties.FILTER_EXCLUDE_AS_NUMBERS_KEY + " and " + 
                AppProperties.FILTER_INCLUDE_AS_NUMBERS_KEY + " are undefined.";
            throw new MegatronException(msg);
        }
        if ((excludeList != null) && (includeList != null)) {
            String msg = "Only one of the property excludeAsNumbers or includeAsNumbers can be defined -- not both."; 
            throw new MegatronException(msg);
        }
    }


    @Override
    public boolean accept(LogEntry logEntry) throws MegatronException {
        boolean result = false;
        
        if (excludeList != null) {
            if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_PRIMARY)) {
                result = !excludeList.contains(convertAsn(logEntry.getAsn()));
            } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_SECONDARY)) {
                result = !excludeList.contains(convertAsn(logEntry.getAsn2()));
            } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_BOTH)) {
                result = !(excludeList.contains(convertAsn(logEntry.getAsn())) || excludeList.contains(convertAsn(logEntry.getAsn2())));
            }
        } else {
            if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_PRIMARY)) {
                result = includeList.contains(convertAsn(logEntry.getAsn()));
            } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_SECONDARY)) {
                result = includeList.contains(convertAsn(logEntry.getAsn2()));
            } else if (organizationToFilter.equalsIgnoreCase(Constants.ORGANIZATION_BOTH)) {
                result = includeList.contains(convertAsn(logEntry.getAsn())) || includeList.contains(convertAsn(logEntry.getAsn2()));
            }
        }
        
        if (!result) {
            ++noOfFilteredEntries;
            // log.debug("Log entry filtered out: " + logEntry.getId());
        }
        
        return result;
    }


    @Override
    public void close() throws MegatronException {
        log.info("No. of filtered log entries (AsnFilter): " + noOfFilteredEntries);
    }

    
    private String convertAsn(Long asn) {
        return ((asn == null) || (asn <= 0L)) ? "-" : asn.toString();
    }

}
