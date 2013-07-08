package se.sitic.megatron.filter;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.entity.LogEntry;


/**
 * Combines OrganizationFilter and CountryCodeFilter: log entry is accepted if
 * OrganizationFilter OR CountryCodeFilter accepts it.
 */
public class OrganizationOrCountryCodeFilter implements ILogEntryFilter {
    private static final Logger log = Logger.getLogger(OrganizationOrCountryCodeFilter.class);

    private OrganizationFilter organizationFilter;
    private CountryCodeFilter countryCodeFilter;
    private long noOfFilteredEntries;

    
    public OrganizationOrCountryCodeFilter() {
        // empty
    }

    
    public void init(JobContext jobContext) throws MegatronException {
        organizationFilter = new OrganizationFilter();
        organizationFilter.init(jobContext);
        countryCodeFilter = new CountryCodeFilter();
        countryCodeFilter.init(jobContext);
    }


    public boolean accept(LogEntry logEntry) throws MegatronException {
        boolean result = organizationFilter.accept(logEntry) || countryCodeFilter.accept(logEntry);
        if (!result) {
            ++noOfFilteredEntries;
            // log.debug("Log entry filtered out: " + logEntry.getId());
        }
        return result;
    }


    public void close() throws MegatronException {
        log.info("No. of filtered log entries (OrganizationOrCountryCodeFilter): " + noOfFilteredEntries);
        if (organizationFilter != null) {
            organizationFilter.close();
        }
        if (countryCodeFilter != null) {
            countryCodeFilter.close();
        }
    }
    
}
