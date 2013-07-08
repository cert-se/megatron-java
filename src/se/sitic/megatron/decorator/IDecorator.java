package se.sitic.megatron.decorator;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.entity.LogEntry;


/**
 * Decorates a LogEntry-object with data. Data may be added to a LogEntry-object, or existing 
 * data may be modified. Example: if hostname is missing but ip-address exists, a decorator
 * does a reverse lookup and adds hostname to the LogEntry. 
 */
public interface IDecorator {
    
    public void init(JobContext jobContext) throws MegatronException;

    public void execute(LogEntry logEntry) throws MegatronException;
    
    public void close() throws MegatronException;
    
}
