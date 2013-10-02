package se.sitic.megatron.filter;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.decorator.OrganizationMatcherDecorator;
import se.sitic.megatron.entity.LogEntry;


/**
 * Filter out log entries that does not match an organization.
 */
public class OrganizationFilter implements ILogEntryFilter {
    private static final Logger log = Logger.getLogger(OrganizationFilter.class);

    private OrganizationMatcherDecorator organizationMatcher; 
    private long noOfFilteredEntries;

    
    public OrganizationFilter() {
        // empty
    }

    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();

        boolean matchIpAddress = props.getBoolean(AppProperties.FILTER_MATCH_IP_ADDRESS_KEY, true);
        boolean matchHostname = props.getBoolean(AppProperties.FILTER_MATCH_HOSTNAME_KEY, true);
        boolean matchAsn = props.getBoolean(AppProperties.FILTER_MATCH_ASN_KEY, true);

        if (matchIpAddress || matchHostname || matchAsn) {
            organizationMatcher = new OrganizationMatcherDecorator();
            organizationMatcher.init(jobContext);
            organizationMatcher.setCheckHighPriorityOrganization(false);
            organizationMatcher.setMatchIpAddress(matchIpAddress);
            organizationMatcher.setMatchHostname(matchHostname);
            organizationMatcher.setMatchAsn(matchAsn);
        }
        
        log.info("Using organization filter; match IP: " + matchIpAddress + ", match hostname: " + matchHostname + ", match ASN: " + matchAsn);
    }

    
    @Override
    public boolean accept(LogEntry logEntry) throws MegatronException {
        boolean result = false;
        
        if (organizationMatcher != null) {
            organizationMatcher.execute(logEntry);
        }
        
        result = (logEntry.getOrganization() != null) || (logEntry.getOrganization2() != null); 
        
        if (!result) {
            ++noOfFilteredEntries;
            // log.debug("Log entry filtered out: " + logEntry.getId());
        }

        return result;
    }

    
    @Override
    public void close() throws MegatronException {
        log.info("No. of filtered log entries (OrganizationFilter): " + noOfFilteredEntries);
    }
    
}
