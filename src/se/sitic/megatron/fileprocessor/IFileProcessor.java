package se.sitic.megatron.fileprocessor;

import java.io.File;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;


/**
 * A file processor handles a whole file, e.g. executes an OS-command to 
 * transform the input file.
 */
public interface IFileProcessor {

    public void init(JobContext jobContext) throws MegatronException;

    
    /**
     * Processes the specified file, and returns the result file.
     */
    public File execute(File inputFile) throws MegatronException;

    
    public void close(boolean jobSuccessful) throws MegatronException;
    
}
