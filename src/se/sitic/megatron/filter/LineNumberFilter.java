package se.sitic.megatron.filter;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
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

    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        this.jobContext = jobContext;
        TypedProperties props = jobContext.getProps();
        
        String[] intervals = props.getStringListFromCommaSeparatedValue(AppProperties.FILTER_LINE_NUMBER_EXCLUDE_INTERVALS_KEY, null, true);
        excludeIntervals = IntervalList.createIntervalList(intervals);
        if (excludeIntervals != null) {
            log.info("Using exclude line number filter: " + excludeIntervals.toString());
        }
        intervals = props.getStringListFromCommaSeparatedValue(AppProperties.FILTER_LINE_NUMBER_INCLUDE_INTERVALS_KEY, null, true);
        includeIntervals = IntervalList.createIntervalList(intervals);
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

    
    @Override
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
    
    
    @Override
    public void close() throws MegatronException {
        log.info("No. of filtered lines (LineNumberFilter): " + noOfFilteredLines);
    }

}
