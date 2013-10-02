package se.sitic.megatron.lineprocessor;

import java.util.ArrayList;
import java.util.Arrays;
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
 * Splits one line to several lines using reg-exp for separator or item.
 */
public class LineSplitter implements ILineProcessor {
    private static final Logger log = Logger.getLogger(LineSplitter.class);

    private Pattern separatorPattern;
    private Matcher itemMatcher;
    private boolean appendOriginalLogRow;
    
    
    public LineSplitter() {
        // empty
    }

    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();

        String separatorRegExp = props.getString(AppProperties.LINE_SPLITTER_SEPARATOR_REG_EXP_KEY, null);
        if (separatorRegExp != null) {
            try {
                separatorPattern = Pattern.compile(separatorRegExp);
                log.info("Using line splitter; " + AppProperties.LINE_SPLITTER_SEPARATOR_REG_EXP_KEY + "=" + separatorRegExp);
            } catch (PatternSyntaxException e) {
                String msg = "Cannot compile reg-exp (" + AppProperties.LINE_SPLITTER_SEPARATOR_REG_EXP_KEY + "): " + separatorRegExp; 
                throw new MegatronException(msg, e);
            }
        }

        String itemRegExp = props.getString(AppProperties.LINE_SPLITTER_ITEM_REG_EXP_KEY, null);
        if (itemRegExp != null) {
            try {
                itemMatcher = Pattern.compile(itemRegExp).matcher("");
                log.info("Using line splitter; " + AppProperties.LINE_SPLITTER_ITEM_REG_EXP_KEY + "=" + itemRegExp);
            } catch (PatternSyntaxException e) {
                String msg = "Cannot compile reg-exp (" + AppProperties.LINE_SPLITTER_ITEM_REG_EXP_KEY + "): " + itemRegExp; 
                throw new MegatronException(msg, e);
            }
        }

        if ((separatorPattern == null) && (itemMatcher == null)) {
            String msg = "No reg-exp defined; both " + AppProperties.LINE_SPLITTER_SEPARATOR_REG_EXP_KEY + " and " + AppProperties.LINE_SPLITTER_ITEM_REG_EXP_KEY + " are undefined.";
            throw new MegatronException(msg);
        }
        if ((separatorPattern != null) && (itemMatcher != null)) {
            String msg = "Only one of the property separatorRegExp or itemRegExp can be defined -- not both."; 
            throw new MegatronException(msg);
        }
        
        appendOriginalLogRow = props.getBoolean(AppProperties.LINE_SPLITTER_APPEND_ORIGINAL_LOG_ROW_KEY, false);
    }

    
    @Override
    public List<String> execute(String line) throws MegatronException {
        List<String> result = null;
        
        if (separatorPattern != null) {
            String[] lines = separatorPattern.split(line);
            result = new ArrayList<String>(Arrays.asList(lines));
        } else {
            // if itemMatcher does not match, null is returned
            itemMatcher.reset(line);
            while (itemMatcher.find()) {
                if (result == null) {
                    result = new ArrayList<String>();
                }
                result.add(itemMatcher.group(0));
            }
        }

        if (appendOriginalLogRow && (result != null)) {
            List<String> resultExtended = new ArrayList<String>(result.size());
            for (Iterator<String> iterator = result.iterator(); iterator.hasNext(); ) {
                resultExtended.add(iterator.next() + "\t" + line);
            }
            result = resultExtended;
        }
        
        if (log.isDebugEnabled() && (result != null) && (result.size() > 1)) {
            log.debug("Line split in " + result.size() + " parts. Line: '" + line + "' --> " + result);
        }

        return result;
    }

    
    @Override
    public void close() throws MegatronException {
        // empty
    }

}
