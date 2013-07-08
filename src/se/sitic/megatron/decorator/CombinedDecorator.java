package se.sitic.megatron.decorator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.StringUtil;


/**
 * Adds IP + ASN + country code + hostname.
 */
public class CombinedDecorator implements IDecorator {
    private static final Logger log = Logger.getLogger(CombinedDecorator.class);
    
    private static final String[] CLASS_NAMES = {
        IpAddressDecorator.class.getName(),
        AsnGeoIpDecorator.class.getName(),
        CountryCodeDecorator.class.getName(),
        HostnameDecorator.class.getName(),
    };
    
    private List<IDecorator> decorators;

    
    public void init(JobContext jobContext) throws MegatronException {
        decorators = new ArrayList<IDecorator>();

        StringBuilder classNamesString = new StringBuilder(128);
        
        TypedProperties props = jobContext.getProps();
        String[] classNames = props.getStringList(AppProperties.DECORATOR_COMBINED_DECORATOR_CLASS_NAMES_KEY, CLASS_NAMES);
        for (int i = 0; i < classNames.length; i++) {
            String className = classNames[i];
            try {
                Class<?> clazz = Class.forName(className);
                IDecorator decorator = (IDecorator)clazz.newInstance();
                decorator.init(jobContext);
                decorators.add(decorator);
                
                if (classNamesString.length() != 0) {
                    classNamesString.append(", ");
                }
                String shortClassName = StringUtil.removePrefix(className, clazz.getPackage().getName() + ".");
                classNamesString.append(shortClassName);
            } catch (Exception e) {
                // ClassNotFoundException, InstantiationException, IllegalAccessException, MegatronException
                String msg = "Cannot instantiate or initialize decorator class: " + className;
                throw new MegatronException(msg, e);
            }
        }
        log.info("Using combined decorator: " + classNamesString.toString());
    }


    public void execute(LogEntry logEntry) throws MegatronException {
        for (Iterator<IDecorator> iterator = decorators.iterator(); iterator.hasNext(); ) {
            iterator.next().execute(logEntry);
        }
    }


    public void close() throws MegatronException {
        for (Iterator<IDecorator> iterator = decorators.iterator(); iterator.hasNext(); ) {
            IDecorator decorator = iterator.next();
            try {
                decorator.close();
            } catch (Exception e) {
                log.error("Cannot close decorator.", e);
            }
        }
    }

}
