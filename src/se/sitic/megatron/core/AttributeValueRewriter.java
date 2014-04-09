package se.sitic.megatron.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import se.sitic.megatron.util.StringUtil;


/**
 * Rewrites attribute values, e.g. rewrites the URL from "http" to "hxxp".
 */
public class AttributeValueRewriter {
    private final Logger log = Logger.getLogger(AttributeValueRewriter.class);

    // Attribute name, Rewriter
    private Map<String, Rewriter> rewriterMap;
    
    
    /**
     * Contructs a rewriter from specified rewriter array (one rewriter for 
     * each attribute to rewrite).
     */
    public AttributeValueRewriter(String[] rewriteArray) throws MegatronException {
        rewriterMap = new HashMap<String, AttributeValueRewriter.Rewriter>();
        for (int i = 0; i < rewriteArray.length; i++) {
            // Syntax: <attribute name>:<from>--><replace with>
            // Example: url:(?i)(h)tt(ps{0,1}://.+)-->$1xx$2
            int hit = rewriteArray[i].indexOf(":");
            if ((hit == -1) || (hit == 0)) {
                throw new MegatronException("Invalid syntax for rewriter property (':' is missing): " + rewriteArray[i]);
            }
            if (hit == (rewriteArray[i].length() - 1)) {
                throw new MegatronException("Invalid syntax for rewriter property (from is missing): " + rewriteArray[i]);
            }
            String attributeName = rewriteArray[i].substring(0, hit);
            String[] fromToArray = StringUtil.splitHeadTail(rewriteArray[i].substring(hit + 1), "-->", false);
            if ((fromToArray == null) || (fromToArray.length != 2) || (fromToArray[0].length() == 0) || (fromToArray[1].length() == 0)) {
                throw new MegatronException("Invalid syntax for rewriter property ('-->', from, or replaceWith is missing): " + rewriteArray[i]);
            }
            Rewriter rewriter = new Rewriter(fromToArray[0], fromToArray[1]);
            rewriterMap.put(attributeName, rewriter);
            log.info("Adding rewriter for " + attributeName + ":'" + fromToArray[0] + "' --> '" + fromToArray[1] + "'");
        }
    }

    
    /**
     * Creates an AttributeValueRewriter-object, or null if rewriteArray is empty.
     */
    public static AttributeValueRewriter createAttributeValueRewriter(String[] rewriterArray) throws MegatronException {
        AttributeValueRewriter result = null;
        if ((rewriterArray != null) && (rewriterArray.length != 0)) {
            result = new AttributeValueRewriter(rewriterArray);
        } else {
            result = null;
        }
        return result;
    }
    
    
    /**
     * Rewrites attribute values in specified map that matches rewriter regular expressions.
     * 
     * @return no. of rewrites.
     */
    public long rewrite(Map<String, String> attributeMap) {
        long result = 0L;
        for (Iterator<String> iterator = rewriterMap.keySet().iterator(); iterator.hasNext(); ) {
            String attributeName = iterator.next();
            Rewriter rewriter = rewriterMap.get(attributeName);
            if (rewriter != null) {
                String attributeValue = attributeMap.get(attributeName);
                if (attributeValue == null) {
                    continue;
                }
                String newValue = rewriter.rewrite(attributeValue);
                if (!newValue.equals(attributeValue)) {
                    ++result;
                    attributeMap.put(attributeName, newValue);
                    if (log.isDebugEnabled()) {
                        log.debug(attributeName + " rewritten: '" + attributeValue + "' --> '" + newValue + "'");
                    }
                }
            }
        }
        return result;
    }
    
    
    /**
     * Matches an attribute value and rewrites it if match.
     */
    private class Rewriter {
        private Matcher fromMatcher;
        private String replaceWith;

        
        public Rewriter(String fromRegExp, String replaceWith) throws MegatronException {
            try {
                this.fromMatcher = Pattern.compile(fromRegExp).matcher("");
                this.replaceWith = replaceWith;
            } catch (PatternSyntaxException e) {
                throw new MegatronException("Cannot compile reg-exp: " + fromRegExp, e);
            }
        }
        
        
        public String rewrite(String attributeValue) {
            fromMatcher.reset(attributeValue);
            return fromMatcher.replaceAll(replaceWith);
        }
        
    }

}
