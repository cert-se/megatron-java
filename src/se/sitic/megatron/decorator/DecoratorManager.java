package se.sitic.megatron.decorator;

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
 * Handles a list of IDecorators and its lifecycle:<ul>
 * <li>Creation</li>
 * <li>Execution</li>
 * <li>Cleanup</li>
 * </ul> 
 */
public class DecoratorManager {
    private static final Logger log = Logger.getLogger(DecoratorManager.class);
    
    private JobContext jobContext;
    private List<IDecorator> decorators;
    private OrganizationMatcherDecorator organizationMatcher;

    
    public DecoratorManager(JobContext jobContext) {
        this.jobContext = jobContext;
    }    

    
    public void init(String propKey, boolean useOrganizationMatcher) throws MegatronException {
        decorators = createDecorators(propKey, useOrganizationMatcher);
    }

    
    public void init(String[] classNames) throws MegatronException {
        decorators = createDecorators(classNames, false);
    }

    
    public void executeDecorators(LogEntry logEntry) throws MegatronException {
        if (decorators.isEmpty()) {
            return;
        }

        for (Iterator<IDecorator> iterator = decorators.iterator(); iterator.hasNext(); ) {
            iterator.next().execute(logEntry);
        }
    }

    
    public OrganizationMatcherDecorator getOrganizationMatcher() {
        return organizationMatcher;
    }
    
    
    public void closeDecorators() {
        if (decorators == null) {
            return;
        }
        for (Iterator<IDecorator> iterator = decorators.iterator(); iterator.hasNext(); ) {
            try {
                iterator.next().close();
            } catch (Exception e) {
                log.error("Cannot close decorator.", e);
            }
        }
    }

    
    private List<IDecorator> createDecorators(String propKey, boolean useOrganizationMatcher) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        String[] classNames = props.getStringList(propKey, new String[0]);
        return createDecorators(classNames, useOrganizationMatcher);
    }
    
    
    private List<IDecorator> createDecorators(String[] classNames, boolean useOrganizationMatcher) throws MegatronException {
        List<IDecorator> result = new ArrayList<IDecorator>();
        if (classNames.length > 0) {
            log.debug("Using decorators: " + Arrays.asList(classNames));
        }
        for (int i = 0; i < classNames.length; i++) {
            String className =  classNames[i];
            if (className.trim().length() == 0) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(className);
                IDecorator decorator = (IDecorator)clazz.newInstance();
                decorator.init(jobContext);
                result.add(decorator);
            } catch (MegatronException e) {
                String msg = "Cannot initialize decorator class: " + className;
                throw new MegatronException(msg, e);
            } catch (Exception e) {
                // ClassNotFoundException, InstantiationException, IllegalAccessException
                String msg = "Cannot instantiate decorator class: " + className;
                throw new MegatronException(msg, e);
            }
        }

        // Add OrganizationMatcherDecorator
        if (useOrganizationMatcher) {
            organizationMatcher = new OrganizationMatcherDecorator();
            organizationMatcher.init(jobContext);
            result.add(organizationMatcher);
        }
        
        return result;
    }
    
}
