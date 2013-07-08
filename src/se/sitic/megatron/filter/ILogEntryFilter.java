package se.sitic.megatron.filter;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.entity.LogEntry;


/**
 * Filter a LogEntry-object. This filter can be used after a line have been parsed.
 */
public interface ILogEntryFilter {

    
    public void init(JobContext jobContext) throws MegatronException;

    
    /**
     * Tests whether or not the specified log entry should be included.
     * 
     * @return true if line should be included.
     */
    public boolean accept(LogEntry logEntry) throws MegatronException;
    
    
    public void close() throws MegatronException;

}
