package se.sitic.megatron.core;

import org.apache.log4j.Logger;

import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.decorator.DecoratorManager;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.filter.LogEntryFilterManager;


/**
 * Class to extend for managers that coordinates export of log entries from
 * the database. 
 */
public abstract class AbstractExportManager {
    private final Logger log = Logger.getLogger(AbstractExportManager.class);
    
    protected TypedProperties props;
    protected String jobName;
    protected Job job;
    protected JobContext jobContext; 
    protected DecoratorManager decoratorManager;
    protected LogEntryFilterManager filterManager;
    protected DbManager dbManager;

    
    public AbstractExportManager(TypedProperties props) {
        this.props = props;
    }

    
    protected abstract JobContext createJobContext() throws MegatronException;

    
    protected abstract String getPreExportFiltersPropertyKey();

    
    protected abstract String getPreExportDecoratorsPropertyKey();
    
    
    protected void init() throws MegatronException {
        dbManager = DbManager.createDbManager(props);
        log.debug("Fetching job from db: " + jobName);
        job = dbManager.searchLogJob(jobName);
        if (job == null) {
            throw new MegatronException("Job not found in db: " + jobName);
        }
        jobContext = createJobContext();
        decoratorManager = new DecoratorManager(jobContext);
        decoratorManager.init(getPreExportDecoratorsPropertyKey(), false);
        filterManager = new LogEntryFilterManager(jobContext);
        filterManager.init(getPreExportFiltersPropertyKey());
    }

}
