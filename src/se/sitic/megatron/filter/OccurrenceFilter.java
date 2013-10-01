package se.sitic.megatron.filter;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.IntervalList;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.parser.LogEntryMapper;


/**
 * Filter log entries by occurrence, e.g. "include first 20 matches of the same IP address" or
 * "include log entries with more than 10 occurrences of the same URL".
 * <p>
 * Note: This filter can consume a lot of memory if fileSorted==false, because attribute values 
 * are then kept in memory.  
 */
public class OccurrenceFilter implements ILogEntryFilter {
    private static final Logger log = Logger.getLogger(OccurrenceFilter.class);    

    private JobContext jobContext;
    private String[] attributeNames;    
    private IntervalList excludeIntervals;
    private IntervalList includeIntervals;
    private long noOfFilteredLines;
    private boolean fileSorted;
    private Map<String, Long> attributeValueOccurrenceMap;
    private String prevAttributeValueStr;
    private long noOfOccurrences;

    
    public OccurrenceFilter() {
        // empty
    }

    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        this.jobContext = jobContext;
        TypedProperties props = jobContext.getProps();
        
        attributeNames = props.getStringListFromCommaSeparatedValue(AppProperties.FILTER_OCCURRENCE_ATTRIBUTE_NAMES_KEY, null, true);
        if (attributeNames == null) {
            throw new MegatronException("No attribute names defined: " + AppProperties.FILTER_OCCURRENCE_ATTRIBUTE_NAMES_KEY);
        }
        
        String[] intervals = props.getStringListFromCommaSeparatedValue(AppProperties.FILTER_OCCURRENCE_EXCLUDE_INTERVALS_KEY, null, true);
        excludeIntervals = IntervalList.createIntervalList(intervals);
        if (excludeIntervals != null) {
            log.info("Using exclude occurrence filter: " + excludeIntervals.toString());
        }
        intervals = props.getStringListFromCommaSeparatedValue(AppProperties.FILTER_OCCURRENCE_INCLUDE_INTERVALS_KEY, null, true);
        includeIntervals = IntervalList.createIntervalList(intervals);
        if (includeIntervals != null) {
            log.info("Using include occurrence filter: " + includeIntervals.toString());
        }

        if ((excludeIntervals == null) && (includeIntervals == null)) {
            String msg = "No occurrence intervals defined; both " + AppProperties.FILTER_OCCURRENCE_EXCLUDE_INTERVALS_KEY + " and " + 
                AppProperties.FILTER_OCCURRENCE_INCLUDE_INTERVALS_KEY + " are undefined.";
            throw new MegatronException(msg);
        }
        if ((excludeIntervals != null) && (includeIntervals != null)) {
            String msg = "Only one of the property excludeIntervals or includeIntervals can be defined -- not both."; 
            throw new MegatronException(msg);
        }
        
        fileSorted = props.getBoolean(AppProperties.FILTER_OCCURRENCE_FILE_SORTED_KEY, false);
        if (!fileSorted) {
            attributeValueOccurrenceMap = new HashMap<String, Long>();            
        }
        prevAttributeValueStr = null;
        noOfOccurrences = 0L;
    }

    
    @Override
    public boolean accept(LogEntry logEntry) throws MegatronException {
        boolean result = true;
        
        LogEntryMapper mapper = new LogEntryMapper(jobContext.getProps(), logEntry);

        // -- get attribute values
        StringBuilder valueStringBuilder = new StringBuilder(256);
        for (int i = 0; i < attributeNames.length; i++) {
            String attributeVal = mapper.getAttribute(attributeNames[i]);
            attributeVal = (attributeVal != null) ? attributeVal : "-";
            valueStringBuilder.append(attributeVal);
        }
        String attributeValueStr = valueStringBuilder.toString();

        // -- set noOfOccurrences
        if (fileSorted) {
            if ((prevAttributeValueStr != null) && attributeValueStr.equals(prevAttributeValueStr)) {
                ++noOfOccurrences;
            } else {
                noOfOccurrences = 1L;
            }
            prevAttributeValueStr = attributeValueStr;
        } else {
            Long val = attributeValueOccurrenceMap.get(attributeValueStr);
            if (val != null) {
                val = new Long(val.longValue() + 1L);
            } else {
                val = new Long(1L);                
            }
            attributeValueOccurrenceMap.put(attributeValueStr, val);
            noOfOccurrences = val.longValue();
        }
        
        // -- check intervals
        if (excludeIntervals != null) {
            result = (excludeIntervals.findFirstInterval(noOfOccurrences) == null); 
        }

        if (includeIntervals != null) {
            result = (includeIntervals.findFirstInterval(noOfOccurrences) != null); 
        }

        if (!result) {
            ++noOfFilteredLines;
            // log.debug("Line filtered out: " + line);
        }
        
        return result;
    }
    
    
    @Override
    public void close() throws MegatronException {
        log.info("No. of filtered lines (OccurrenceFilter): " + noOfFilteredLines);
    }

}
