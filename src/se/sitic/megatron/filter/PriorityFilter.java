package se.sitic.megatron.filter;

import java.util.Arrays;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.Interval;
import se.sitic.megatron.core.IntervalList;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.Organization;


/**
 * Filter log entries using priority in matched organization. 
 * Note: Requires that organization matcher have been executed.
 */
public class PriorityFilter implements ILogEntryFilter {
    private static final Logger log = Logger.getLogger(PriorityFilter.class);    

    private IntervalList includeIntervalList;
    private long noOfFilteredEntries;
    private long noOfEntriesWithPrio;

    
    public PriorityFilter() {
        // empty
    }

    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps(); 
        String[] intervalStringList = null;
        if (props.getPrio().length() > 0) {
            intervalStringList = props.getStringListFromCommaSeparatedValue(TypedProperties.CLI_PRIO_KEY, null, true); 
            // add "-" for lonely number, e.g. "60" --> "60-"
            if ((intervalStringList.length == 1) && !intervalStringList[0].contains("-")) {
                intervalStringList[0] = intervalStringList[0] + "-";
            }
        } else {
            String propertyKey = AppProperties.FILTER_PRIORITY_INCLUDE_INTERVALS_KEY;
            intervalStringList = props.getStringListFromCommaSeparatedValue(propertyKey, null, true);
            if ((intervalStringList == null) || (intervalStringList.length == 0)) {
                throw new MegatronException("Mandatory property not defined: " + propertyKey);
            }
        }
        try {
            log.info("Priority intervals used by PriorityFilter: " + Arrays.asList(intervalStringList));
            includeIntervalList = new IntervalList();
            for (int i = 0; i < intervalStringList.length; i++) {
                includeIntervalList.add(new Interval(intervalStringList[i]));
            }
        } catch (MegatronException e) {
            throw new MegatronException("Cannot create include interval for priority filter.", e);
        }
    }

    
    @Override
    public boolean accept(LogEntry logEntry) throws MegatronException {
        // default is not to accept; only include intervals is used 
        boolean result = false;
        Organization organization = logEntry.getOrganization();
        if ((organization != null) && (organization.getPriority() != null) && (organization.getPriority().getPrio() != null)) {
            int prio = organization.getPriority().getPrio().intValue();
            result = (includeIntervalList.findFirstInterval(prio) != null);
            ++noOfEntriesWithPrio;
        }
        
        if (!result) {
            ++noOfFilteredEntries;
            // log.debug("Log entry filtered out: " + logEntry.getId());
        }
        
        return result;
    }

    
    @Override
    public void close() throws MegatronException {
        log.info("No. of filtered log entries (PriorityFilter): " + noOfFilteredEntries);
        if ((noOfEntriesWithPrio == 0) && (noOfFilteredEntries > 0)) {
            log.warn("No log entries had a priority assigned. This may indicate that organization matcher have not been executed.");
        }
    }

}
