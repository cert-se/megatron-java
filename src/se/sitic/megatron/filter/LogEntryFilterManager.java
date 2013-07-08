package se.sitic.megatron.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;


/**
 * Handles a list of ILogEntryFilters and its lifecycle:<ul>
 * <li>Creation</li>
 * <li>Execution</li>
 * <li>Cleanup</li>
 * </ul> 
 */
public class LogEntryFilterManager {
    private static final Logger log = Logger.getLogger(LogEntryFilterManager.class);
    
    private JobContext jobContext;
    private List<ILogEntryFilter> filters;

    
    public LogEntryFilterManager(JobContext jobContext) {
        this.jobContext = jobContext;
    }    

    
    public void init(String propKey) throws MegatronException {
        filters = createLogEntryFilters(propKey);
    }

    
    public void init(String[] classNames) throws MegatronException {
        filters = createLogEntryFilters(classNames);
    }

    
    public boolean executeFilters(LogEntry logEntry) throws MegatronException {
        if (filters.isEmpty()) {
            return true;
        }
        
        boolean result = true;
        for (Iterator<ILogEntryFilter> iterator = filters.iterator(); result && iterator.hasNext(); ) {
            result &= iterator.next().accept(logEntry);
        }
        return result;
    }

    
    public void closeFilters() {
        if (filters == null) {
            return;
        }
        for (Iterator<ILogEntryFilter> iterator = filters.iterator(); iterator.hasNext(); ) {
            try {
                iterator.next().close();
            } catch (Exception e) {
                log.error("Cannot close log entry filter.", e);
            }
        }
    }

    
    private List<ILogEntryFilter> createLogEntryFilters(String propKey) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        String[] classNames = props.getStringList(propKey, new String[0]);
        return createLogEntryFilters(classNames);
    }

    
    private List<ILogEntryFilter> createLogEntryFilters(String[] classNames) throws MegatronException {
        List<ILogEntryFilter> result = new ArrayList<ILogEntryFilter>();
        if (classNames.length > 0) {
            log.debug("Using log entry filters: " + Arrays.asList(classNames));
        }
        for (int i = 0; i < classNames.length; i++) {
            String className = classNames[i];
            if (className.trim().length() == 0) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(className);
                ILogEntryFilter filter = (ILogEntryFilter)clazz.newInstance();
                filter.init(jobContext);
                result.add(filter);
            } catch (MegatronException e) {
                String msg = "Cannot initialize filter class: " + className;
                throw new MegatronException(msg, e);
            } catch (Exception e) {
                // ClassNotFoundException, InstantiationException, IllegalAccessException
                String msg = "Cannot instantiate filter class: " + className;
                throw new MegatronException(msg, e);
            }
        }
        return result;
    }
    
}
