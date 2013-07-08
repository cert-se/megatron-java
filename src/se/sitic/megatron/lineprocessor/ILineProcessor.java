package se.sitic.megatron.lineprocessor;

import java.util.List;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;


/**
 * A line processor merges or splits a line, and can be one of the following 
 * two types:<ul>
 * <li>Merger: Serveral lines are merged into a single line.
 * <li>Splitter: One line is split to several lines.
 * </ul> 
 */
public interface ILineProcessor {

    
    public void init(JobContext jobContext) throws MegatronException;
   
    
    /**
     * Merges or splits specified line. If a line is merged, null is returned 
     * at least one time.
     */
    public List<String> execute(String line) throws MegatronException;
    
    
    public void close() throws MegatronException;

}
