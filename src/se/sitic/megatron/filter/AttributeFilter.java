package se.sitic.megatron.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.parser.LogEntryMapper;


/**
 * Filter log entries by matching an attribute to a regular expression. All 
 * template variables can be used, e.g. "ipAddress", "url", "hostname", 
 * "hostname2", "logTimestamp", "organizationName", "originalLogEntry", 
 * "additionalItem_httpStatusCode", or "freeText0". 
 */
public class AttributeFilter implements ILogEntryFilter {
    private static final Logger log = Logger.getLogger(AttributeFilter.class);

    private TypedProperties props;
    private String attributeName;
    private Matcher excludeMatcher;
    private Matcher includeMatcher;
    private long noOfFilteredEntries;

    
    public AttributeFilter() {
        // empty
    }

    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        props = jobContext.getProps();

        attributeName = props.getString(AppProperties.FILTER_ATTRIBUTE_NAME_KEY, null);
        if (attributeName == null) {
            throw new MegatronException("No attribute name defined: " + AppProperties.FILTER_ATTRIBUTE_NAME_KEY);
        }

        String excludeRegExp = props.getString(AppProperties.FILTER_ATTRIBUTE_EXCLUDE_REG_EXP_KEY, null);
        if (excludeRegExp != null) {
            try {
                excludeMatcher = Pattern.compile(excludeRegExp).matcher("");
                log.info("Using attribute filter; " + AppProperties.FILTER_ATTRIBUTE_EXCLUDE_REG_EXP_KEY + "=" + excludeRegExp);
            } catch (PatternSyntaxException e) {
                String msg = "Cannot compile reg-exp (" + AppProperties.FILTER_ATTRIBUTE_EXCLUDE_REG_EXP_KEY + "): " + excludeRegExp; 
                throw new MegatronException(msg, e);
            }
        }
        String includeRegExp = props.getString(AppProperties.FILTER_ATTRIBUTE_INCLUDE_REG_EXP_KEY, null);
        if (includeRegExp != null) {
            try {
                includeMatcher = Pattern.compile(includeRegExp).matcher("");
                log.info("Using attribute filter; " + AppProperties.FILTER_ATTRIBUTE_INCLUDE_REG_EXP_KEY + "=" + includeRegExp);
            } catch (PatternSyntaxException e) {
                String msg = "Cannot compile reg-exp (" + AppProperties.FILTER_ATTRIBUTE_INCLUDE_REG_EXP_KEY + "): " + includeRegExp; 
                throw new MegatronException(msg, e);
            }
        }

        if ((excludeMatcher == null) && (includeMatcher == null)) {
            String msg = "No reg-exp defined; both " + AppProperties.FILTER_ATTRIBUTE_EXCLUDE_REG_EXP_KEY + " and " + AppProperties.FILTER_ATTRIBUTE_INCLUDE_REG_EXP_KEY + " are undefined.";
            throw new MegatronException(msg);
        }
        if ((excludeMatcher != null) && (includeMatcher != null)) {
            String msg = "Only one of the property excludeRegExp or includeRegExp can be defined -- not both."; 
            throw new MegatronException(msg);
        }
    }
    
    
    @Override
    public boolean accept(LogEntry logEntry) throws MegatronException {
        boolean result = false;

        // -- create mapper
// Note: It's rather expensive to create a LogEntryMapper-object: 3-4 ms
//        long t1 = System.currentTimeMillis();
//        LogEntryMapper mapper = new LogEntryMapper(props, logEntry);
//        if (log.isDebugEnabled()) {
//            long diff = System.currentTimeMillis() - t1;
//            String durationStr = DateUtil.formatDuration(diff);
//            log.debug("Time to create LogEntryMapper-object: " + durationStr);
//        }
        LogEntryMapper mapper = new LogEntryMapper(props, logEntry);

        // -- get attribute and match
        String attributeVal = mapper.getAttribute(attributeName);
        attributeVal = (attributeVal != null) ? attributeVal : "-";
        
        if (excludeMatcher != null) {
            excludeMatcher.reset(attributeVal);
            result = !excludeMatcher.find();
        }

        if (includeMatcher != null) {
            includeMatcher.reset(attributeVal);
            result = includeMatcher.find();
        }

        if (!result) {
            ++noOfFilteredEntries;
            // log.debug("Log entry filtered out: " + logEntry.getId());
        }
        
        return result;
    }
    
    
    @Override
    public void close() throws MegatronException {
        log.info("No. of filtered log entries (AttributeFilter): " + noOfFilteredEntries);
    }

}
