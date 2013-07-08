package se.sitic.megatron.filter;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.Interval;
import se.sitic.megatron.core.IntervalList;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;


/**
 * Filter log lines using intervals of line numbers. 
 */
public class LineNumberFilter implements ILineFilter {
    private static final Logger log = Logger.getLogger(LineNumberFilter.class);    

    private JobContext jobContext;
    private IntervalList excludeIntervals;
    private IntervalList includeIntervals;
    private long noOfFilteredLines;
    
    
    public LineNumberFilter() {
        // empty
    }

    
    public void init(JobContext jobContext) throws MegatronException {
        this.jobContext = jobContext;
        TypedProperties props = jobContext.getProps();
        
        excludeIntervals = createIntervalList(props.getStringListFromCommaSeparatedValue(AppProperties.FILTER_LINE_NUMBER_EXCLUDE_INTERVALS_KEY, null, true));
        if (excludeIntervals != null) {
            log.info("Using exclude line number filter: " + excludeIntervals.toString());
        }
        includeIntervals = createIntervalList(props.getStringListFromCommaSeparatedValue(AppProperties.FILTER_LINE_NUMBER_INCLUDE_INTERVALS_KEY, null, true));
        if (includeIntervals != null) {
            log.info("Using include line number filter: " + includeIntervals.toString());
        }

        if ((excludeIntervals == null) && (includeIntervals == null)) {
            String msg = "No line number intervals defined; both " + AppProperties.FILTER_LINE_NUMBER_EXCLUDE_INTERVALS_KEY + " and " + 
                AppProperties.FILTER_LINE_NUMBER_INCLUDE_INTERVALS_KEY + " are undefined.";
            throw new MegatronException(msg);
        }
        if ((excludeIntervals != null) && (includeIntervals != null)) {
            String msg = "Only one of the property excludeIntervals or includeIntervals can be defined -- not both."; 
            throw new MegatronException(msg);
        }
    }

    
    public boolean accept(String line) throws MegatronException {
        boolean result = true;
        
        if (excludeIntervals != null) {
            result = (excludeIntervals.findFirstInterval(jobContext.getLineNo()) == null); 
        }

        if (includeIntervals != null) {
            result = (includeIntervals.findFirstInterval(jobContext.getLineNo()) != null); 
        }

        if (!result) {
            ++noOfFilteredLines;
            // log.debug("Line filtered out: " + line);
        }
        
        return result;
    }
    
    
    public void close() throws MegatronException {
        log.info("No. of filtered lines (LineNumberFilter): " + noOfFilteredLines);
    }


    private IntervalList createIntervalList(String[] intervalStrings) throws MegatronException {
        if ((intervalStrings == null) || (intervalStrings.length == 0)) {
            return null;
        }
        
        IntervalList result = new IntervalList();
        for (int i = 0; i < intervalStrings.length; i++) {
            try {
                result.add(new Interval(intervalStrings[i]));
            } catch (MegatronException e) {
                String msg = "Cannot create interval from string: " + intervalStrings[i]; 
                throw new MegatronException(msg, e);
            }
        }
    
        return result;
    }

}
