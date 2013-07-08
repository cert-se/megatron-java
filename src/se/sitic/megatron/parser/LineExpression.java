package se.sitic.megatron.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.ObjectStringSorter;
import se.sitic.megatron.util.StringUtil;


/**
 * Handles a line expression with item variables, i.e. handles a
 * parser.lineRegExp-expression.
 * <p>
 * Expression example: 
 * ^$asn\s*\|\s*$ipAddress\s*\|\s*.+\s*\|\s*$countryCode\s*\|\s*.+\s*\|$freeText0$  
 */
public class LineExpression {
    private static final Logger log = Logger.getLogger(LineExpression.class);
    
    private TypedProperties props;
    private String lineRegExp;
    
    
    public LineExpression(TypedProperties props, String lineRegExp) {
        this.props = props;
        this.lineRegExp = lineRegExp;
    }

    
    /**
     * Returns a list with variables in the expression, sorted by occurance.
     *  
     * @throws InvalidExpressionException if expression is invalid, e.g. contains duplicates.
     */
    
    public List<String> extractVariables() throws InvalidExpressionException {
        final List<String> result = new ArrayList<String>();
        String replacedLineRegExp = lineRegExp;
        
        // -- extract variables (will not contain duplicates)
        List<String> variableCandidates = new ArrayList<String>();
        variableCandidates.addAll(Arrays.asList(LogEntryMapper.EXPRESSION_VARIABLES));
        variableCandidates.addAll(getFreeTextVariables(props));
        variableCandidates.addAll(getAdditionalItemVariables(props));
        sortByLength(variableCandidates);
        for (Iterator<String> iterator = variableCandidates.iterator(); iterator.hasNext(); ) {
            String variable = LogEntryMapper.VARIABLE_PREFIX + iterator.next();
            if (replacedLineRegExp.indexOf(variable) != -1) {
                result.add(variable);
                String newVariable = "@" + variable.substring(1) + "@";
                replacedLineRegExp = StringUtil.replaceFirst(replacedLineRegExp, variable, newVariable);
            }
        }
        
        // -- sort variables by occurance
        sortVariablesByOccurance(result);
        
        // -- check for duplicates
        for (Iterator<String> iterator = result.iterator(); iterator.hasNext(); ) {
            String variable = iterator.next();
            if (replacedLineRegExp.indexOf(variable) != -1) {
                String msg = "The regular expression (parser.lineRegExp) contains duplicated variables: " + variable;
                throw new InvalidExpressionException(msg);
            }
        }
        
        // -- check for unused variables
        boolean doCheck = props.getBoolean(AppProperties.PARSER_CHECK_UNUSED_VARIABLES_KEY, true);
        if (doCheck) {
            int indexOfVariable = replacedLineRegExp.indexOf(LogEntryMapper.VARIABLE_PREFIX);
            if ((indexOfVariable != -1) && (indexOfVariable != (replacedLineRegExp.length() - 1))) {
                String msg = "The regular expression (parser.lineRegExp) may contain unused variables because '$' is present. " +
                    "Use parser.checkUnusedVariables to turn off this check. Expression (\"@variable@\" will be expanded): " + replacedLineRegExp;
                throw new InvalidExpressionException(msg);
            }
        }
        
        return result;
    }

    
    /**
     * Creates a regular expression from this line expression.
     */
    public Matcher createRegExp() throws InvalidExpressionException {
        Matcher result = Pattern.compile(expandRegExp()).matcher("");
        return result;
    }

    
    public String getLineRegExp() {
        return this.lineRegExp;
    }
    
    
    /**
     * Returns list of free text variables that are defined in the config. 
     */
    public static List<String> getFreeTextVariables(TypedProperties typedProperties) {
        List<String> result = new ArrayList<String>();
        String prefix = AppProperties.PARSER_FREE_TEXT_KEY + ".";
        for (Iterator<String> iterator = typedProperties.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            if (key.startsWith(prefix) && (key.matches(prefix + "\\d+"))) {
                String variable = LogEntryMapper.FREE_TEXT_PREFIX + StringUtil.removePrefix(key, prefix); 
                result.add(variable);
            }
        }
        Collections.sort(result, ObjectStringSorter.createDefaultSorter());
        return result;
    }

    
    /**
     * Returns list of additional item variables that are defined in the config. 
     */
    public static List<String> getAdditionalItemVariables(TypedProperties typedProperties) {
        List<String> result = new ArrayList<String>();
        String prefix = AppProperties.PARSER_ADDITIONAL_ITEM_PREFIX;
        for (Iterator<String> iterator = typedProperties.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            if (key.startsWith(prefix)) {
                result.add(LogEntryMapper.ADDITIONAL_ITEM_PREFIX + StringUtil.removePrefix(key, prefix));
            }
        }
        return result;
    }

    
    private String expandRegExp() throws InvalidExpressionException {
        String result = lineRegExp;

        List<String> variables = extractVariables();

        // -- Sort variables by length, e.g. $ipAddress2 must come before $ipAddress (otherwise "2" will not be replaced)
        sortByLength(variables);
        
        for (Iterator<String> iterator = variables.iterator(); iterator.hasNext(); ) {
            String variable = iterator.next().substring(1);
            String key = null;
            if (variable.startsWith(LogEntryMapper.ADDITIONAL_ITEM_PREFIX)) {
                String[] headTail = StringUtil.splitHeadTail(variable, "_", false);
                key = AppProperties.PARSER_ADDITIONAL_ITEM_PREFIX + headTail[1];
            } else if (variable.startsWith(LogEntryMapper.FREE_TEXT_PREFIX)) {
                String numStr = StringUtil.removePrefix(variable, LogEntryMapper.FREE_TEXT_PREFIX);
                key = AppProperties.PARSER_FREE_TEXT_KEY + "." + numStr;
            } else {
                key = AppProperties.PARSER_ITEM_PREFIX + variable;
            }
            String regExp = props.getString(key, "");
            if (regExp.length() == 0) {
                throw new InvalidExpressionException("Invalid expression (parser.lineRegExp); variable not defined: " + key);
            }
            // a variable is a group  
            regExp = "(" + regExp + ")";
            result = StringUtil.replace(result, LogEntryMapper.VARIABLE_PREFIX + variable, regExp);
        }
        
        log.info("Expanded reg-exp (parser.lineRegExp): " + result);

        return result;
    }

    
    private void sortByLength(List<String> strings) {
        Collections.sort(strings, new Comparator<String>() {
                public int compare(String o1, String o2) {
                    // reverse sort order
                    return (o1.length() > o2.length()) ? -1 : (o1.length() == o2.length() ? 0 : 1);
                }
            });
    }
    
    
    private void sortVariablesByOccurance(final List<String> variables) {
        // checks for longer variables with same prefix, e.g. $ipAddress2 may come before $ipAddress
        Collections.sort(variables, new Comparator<String>() {
                public int compare(String var1, String var2) {
                    int index1 = indexOf(getLineRegExp(), var1, 0);
                    int index2 = indexOf(getLineRegExp(), var2, 0);
                    return (index1 < index2) ? -1 : (index1 == index2 ? 0 : 1);  
                }

                private int indexOf(String lineRegExp, String variable, int fromIndex) {
                    int result = -1;
                    if (fromIndex < (lineRegExp.length() - 1)) {
                        result = lineRegExp.indexOf(variable, fromIndex);
                        if (existLongerVariableAtIndex(lineRegExp, result, variable)) {
                            // keep looking; recursive
                            result = indexOf(lineRegExp, variable, fromIndex + 1);
                        }
                    }
                    return result;
                }
                
                private boolean existLongerVariableAtIndex(String lineRegExp, int index, String variable) {
                    if ((index == -1) || ((index + variable.length()) >= (lineRegExp.length() - 1))) {
                        return false;
                    }
                    for (Iterator<String> iterator = variables.iterator(); iterator.hasNext(); ) {
                        String candidateVariable = iterator.next();
                        if (!candidateVariable.equals(variable) && (candidateVariable.length() > variable.length()) && (lineRegExp.indexOf(candidateVariable, index) == index)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
    }

}
