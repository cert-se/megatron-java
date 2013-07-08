package se.sitic.megatron.filter;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;


/**
 * Filter a log line. This filter can be used before a line is parsed.  
 */
public interface ILineFilter {

    
    public void init(JobContext jobContext) throws MegatronException;
   
    
    /**
     * Tests whether or not the specified line should be included.
     * 
     * @return true if line should be included.
     */
    public boolean accept(String line) throws MegatronException;
    
    
    public void close() throws MegatronException;
    
}
