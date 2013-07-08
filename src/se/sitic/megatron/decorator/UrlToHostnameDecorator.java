package se.sitic.megatron.decorator;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.AppUtil;


/**
 * Extracts hostname from the URL-field and assigns it to the hostname-field,
 * or hostname2-field depending on configuration. 
 */
public class UrlToHostnameDecorator implements IDecorator {
    private static final Logger log = Logger.getLogger(UrlToHostnameDecorator.class);
    
    private int noOfAssignedHostnames;
    private boolean usePrimaryOrg;
    private boolean warningLogged = false;
    

    public UrlToHostnameDecorator() {
        // empty
    }

    
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        usePrimaryOrg = props.getBoolean(AppProperties.DECORATOR_URL_TO_HOSTNAME_USE_PRIMARY_ORG_KEY, true);
        String hostnameField = usePrimaryOrg ? "hostname" : "hostname2";  
        log.info("Using UrlToHostnameDecorator. Hostname in URL will be assigned to the " + hostnameField + " field.");
    }    


    public void execute(LogEntry logEntry) throws MegatronException {
        if ((usePrimaryOrg && (logEntry.getHostname() != null)) || (!usePrimaryOrg && (logEntry.getHostname2() != null))) {
            if (!warningLogged) {
                log.error("Job type configuration is incorrect. UrlToHostnameDecorator may overwrite hostname or hostname2.");
                warningLogged = true;
            }
        }
        
        String hostname = AppUtil.extractHostnameFromUrl(logEntry.getUrl());
        if (hostname != null) {
            if (usePrimaryOrg) {
                logEntry.setHostname(hostname);
            } else {
                logEntry.setHostname2(hostname);
            }
            ++noOfAssignedHostnames;
        }
    }

    
    public void close() throws MegatronException {
        log.info("No. of assigned hostnames: " + noOfAssignedHostnames); 
    }

}
