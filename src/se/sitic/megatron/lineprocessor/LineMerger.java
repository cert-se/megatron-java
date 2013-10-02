package se.sitic.megatron.lineprocessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;


/**
 * Merges lines using reg-exp.
 */
public class LineMerger implements ILineProcessor {
    private static final Logger log = Logger.getLogger(LineMerger.class);

    private Matcher startMatcher;
    private Matcher endMatcher;
    private boolean restartIfStartFound;
    private String separator;
    
    private List<String> lines;
    private boolean startFound;
    private int noOfMergers;
    
    
    public LineMerger() {
        // empty
    }

    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        
        String startRegExp = props.getString(AppProperties.LINE_MERGER_START_REG_EXP_KEY, null);
        String endRegExp = props.getString(AppProperties.LINE_MERGER_END_REG_EXP_KEY, null);
        if ((startRegExp != null) && (endRegExp != null)) {
            String regExp = null;
            try {
                regExp = startRegExp;
                startMatcher = Pattern.compile(regExp).matcher("");
                regExp = endRegExp;
                endMatcher = Pattern.compile(regExp).matcher("");
            } catch (PatternSyntaxException e) {
                String msg = "Cannot compile reg-exp for line merger: " + regExp; 
                throw new MegatronException(msg, e);
            }
        } else {
            String msg = "Mandatory properties for line merger are missing: " + AppProperties.LINE_MERGER_START_REG_EXP_KEY + ", " + AppProperties.LINE_MERGER_END_REG_EXP_KEY;
            throw new MegatronException(msg);
        }

        restartIfStartFound = props.getBoolean(AppProperties.LINE_MERGER_RESTART_IF_START_FOUND_KEY, true);
        separator = props.getString(AppProperties.LINE_MERGER_SEPARATOR_KEY, " ");

        lines = new ArrayList<String>();
        startFound = false;
    }


    @Override
    public List<String> execute(String line) throws MegatronException {
        List<String> result = null;
        
        endMatcher.reset(line);
        if (endMatcher.find()) {
            if (startFound) {
                lines.add(line);
                StringBuilder resultLine = new StringBuilder(128);
                for (Iterator<String> iterator = lines.iterator(); iterator.hasNext(); ) {
                    if (resultLine.length() > 0) {
                        resultLine.append(separator);
                    }
                    resultLine.append(iterator.next());
                }
                log.debug("Merged line: '" +  resultLine.toString() + "'.");
                result = Collections.singletonList(resultLine.toString());
                lines.clear();
                startFound = false;
                ++noOfMergers;
            }
        }

        if (result == null) {
            startMatcher.reset(line);
            if (startMatcher.find()) {
                if (startFound && restartIfStartFound) {
                    log.debug("Restart merger-block. Found a start-line before an end-line.");
                    // return copy so client code does not mess it up
                    result = new ArrayList<String>(lines);
                    lines.clear();
                } else if (startFound) {
                    log.debug("Found a start-line before an end-line, but keeping merger-block.");
                }
                startFound = true;
                lines.add(line);
            } else if (startFound) {
                lines.add(line);
            } else {
                result = Collections.singletonList(line);
            }
        }
        
        return result;
    }

    
    @Override
    public void close() throws MegatronException {
        log.debug("No. of mergers: " + noOfMergers);
    }

}
