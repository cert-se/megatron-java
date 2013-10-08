package se.sitic.megatron.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.AttributeValueRewriter;
import se.sitic.megatron.core.ConversionException;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.StringUtil;


/**
 * Parses a log entry line using regular expression defined in "parser.lineRegExp".
 */
public class RegExpParser implements IParser {
    // private static final Logger log = Logger.getLogger(RegExpParser.class);
    
    private TypedProperties props;
    private JobContext jobContext;

    private List<String> variables;
    private Matcher matcher;
    private boolean trimValue;
    private String removeEnclosingCharsFromValue;
    private AttributeValueRewriter rewriter;


    public RegExpParser() {
        // empty
    }

    
    @Override
    public void init(JobContext jobContext) throws InvalidExpressionException, MegatronException {
        this.jobContext = jobContext;
        this.props = jobContext.getProps();
        
        String lineRegExp = props.getString(AppProperties.PARSER_LINE_REG_EXP_KEY, "");
        if (lineRegExp.length() == 0) {
            String msg = "Line expression is missing (parser.lineRegExp).";
            throw new InvalidExpressionException(msg);
        }
        LineExpression expression = new LineExpression(props, lineRegExp);
        variables = expression.extractVariables();
        matcher = expression.createRegExp();
        trimValue = props.getBoolean(AppProperties.PARSER_TRIM_VALUE_KEY, false);
        removeEnclosingCharsFromValue = props.getString(AppProperties.PARSER_REMOVE_ENCLOSING_CHARS_FROM_VALUE_KEY, null);
        String[] rewriterArray = props.getStringList(AppProperties.PARSER_REWRITERS_KEY, null);
        rewriter = AttributeValueRewriter.createAttributeValueRewriter(rewriterArray);
    }

    
    @Override
    public LogEntry parse(String logLine) throws ParseException {
        LogEntry result = null;

        matcher.reset(logLine);
        Map<String, String> parsedLogEntry = new HashMap<String, String>();
        if (matcher.find()) {
            if (matcher.groupCount() != variables.size()) {
                String msg = "Cannot parse line at " + jobContext.getLineNo() + "; groups in reg-exp does not match (" + variables.size() + "!=" + 
                    matcher.groupCount() + "). Line: '" + logLine + "'.";
                throw new ParseException(msg);
            }
            
            for (int i = 0; i < variables.size(); i++) {
                String value = matcher.group(i+1);
                if (value == null) {
                    String msg = "Cannot parse line at " + jobContext.getLineNo() + "; group " + i + " is unmatched. Line: '" + logLine + "'.";
                    throw new ParseException(msg);
                }
                if (trimValue) {
                    value = value.trim();
                }
                // empty value is treated as NULL
                if (value.length() != 0) {
                    if ((removeEnclosingCharsFromValue != null) && (removeEnclosingCharsFromValue.length() != 0)) {
                        value = StringUtil.removeEnclosingChars(value, removeEnclosingCharsFromValue);
                    }
                    parsedLogEntry.put(variables.get(i).substring(1), value);
                } else if (variables.get(i).startsWith(LogEntryMapper.VARIABLE_PREFIX + LogEntryMapper.FREE_TEXT_PREFIX)) {
                    // empty value is stored for free text (otherwise list order cannot be preserved) 
                    parsedLogEntry.put(variables.get(i).substring(1), value);
                }
            }
            
            if (rewriter != null) {
                rewriter.rewrite(parsedLogEntry);
            }
        } else {
            String msg = "Cannot parse line at " + jobContext.getLineNo() + "; line is unmatched. Line: '" + logLine + "'.";
            throw new ParseException(msg);
        }
        
        LogEntryMapper mapper = new LogEntryMapper(props, parsedLogEntry);
        try {
            result = mapper.createLogEntry();
        } catch (ConversionException e) {
            String msg = "Cannot parse line at " + jobContext.getLineNo() + "; " + e.getMessage() + " Line: '" + logLine + "'.";
            throw new ParseException(msg, e);
        }

        return result;
    }


    @Override
    public void close() throws MegatronException {
        // empty
    }
    
}
