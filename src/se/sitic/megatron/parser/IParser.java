package se.sitic.megatron.parser;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.entity.LogEntry;


/**
 * Parses a log line to a LogEntry-object. Implementing classes may use for 
 * example regular expression or an XML-parser.  
 */
public interface IParser {

    public void init(JobContext jobContext) throws MegatronException;
    
    public LogEntry parse(String logLine) throws MegatronException;

    public void close() throws MegatronException;
    
}
