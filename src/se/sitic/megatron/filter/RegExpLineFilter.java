package se.sitic.megatron.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;


/**
 * Filter log lines using regular expressions. 
 */
public class RegExpLineFilter implements ILineFilter {
    private static final Logger log = Logger.getLogger(RegExpLineFilter.class);    

    // UNUSED: private JobContext jobContext;
    private Matcher excludeMatcher;
    private Matcher includeMatcher;
    private long noOfFilteredLines;
    
    
    public RegExpLineFilter() {
        // empty
    }

    
    public void init(JobContext jobContext) throws MegatronException {
        // UNUSED: this.jobContext = jobContext;
    
        TypedProperties props = jobContext.getProps();
        String excludeRegExp = props.getString(AppProperties.FILTER_EXCLUDE_REG_EXP_KEY, null);
        if (excludeRegExp != null) {
            try {
                excludeMatcher = Pattern.compile(excludeRegExp).matcher("");
                log.info("Using line filter; " + AppProperties.FILTER_EXCLUDE_REG_EXP_KEY + "=" + excludeRegExp);
            } catch (PatternSyntaxException e) {
                String msg = "Cannot compile reg-exp (" + AppProperties.FILTER_EXCLUDE_REG_EXP_KEY + "): " + excludeRegExp; 
                throw new MegatronException(msg, e);
            }
        }
        String includeRegExp = props.getString(AppProperties.FILTER_INCLUDE_REG_EXP_KEY, null);
        if (includeRegExp != null) {
            try {
                includeMatcher = Pattern.compile(includeRegExp).matcher("");
                log.info("Using line filter; " + AppProperties.FILTER_INCLUDE_REG_EXP_KEY + "=" + includeRegExp);
            } catch (PatternSyntaxException e) {
                String msg = "Cannot compile reg-exp (" + AppProperties.FILTER_INCLUDE_REG_EXP_KEY + "): " + includeRegExp; 
                throw new MegatronException(msg, e);
            }
        }

        if ((excludeMatcher == null) && (includeMatcher == null)) {
            String msg = "No reg-exp defined; both " + AppProperties.FILTER_EXCLUDE_REG_EXP_KEY + " and " + AppProperties.FILTER_INCLUDE_REG_EXP_KEY + " are undefined.";
            throw new MegatronException(msg);
        }
        if ((excludeMatcher != null) && (includeMatcher != null)) {
            String msg = "Only one of the property excludeRegExp or includeRegExp can be defined -- not both."; 
            throw new MegatronException(msg);
        }
    }


    public boolean accept(String line) throws MegatronException {
        boolean result = true;

        if (excludeMatcher != null) {
            excludeMatcher.reset(line);
            result = !excludeMatcher.find();
        }

        if (includeMatcher != null) {
            includeMatcher.reset(line);
            result = includeMatcher.find();
        }

        if (!result) {
            // log.debug("Line filtered out: " + line);
            ++noOfFilteredLines;
        }
        
        return result;
    }


    public void close() throws MegatronException {
        log.info("No. of filtered lines (RegExpLineFilter): " + noOfFilteredLines);
    }

}
