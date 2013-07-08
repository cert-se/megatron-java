package se.sitic.megatron.decorator;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.IpAddressUtil;


/**
 * Adds ip-address if missing and hostname exists. 
 */
public class IpAddressDecorator implements IDecorator {
    private static final Logger log = Logger.getLogger(IpAddressDecorator.class);    

    private long noOfLookups;
    private long noOfLookups2;
    private long noOfFailedLookups;
    private long noOfFailedLookups2;
    private long totalDuration;
    private long totalDuration2;    

    
    public void init(JobContext jobContext) throws MegatronException {
        // empty
    }


    public void execute(LogEntry logEntry) throws MegatronException {
        if ((logEntry.getIpAddress() == null) && (logEntry.getHostname() != null)) {
            long t1 = System.currentTimeMillis();
            long ipAddress = IpAddressUtil.dnsLookup(logEntry.getHostname());
            long t2 = System.currentTimeMillis();
            totalDuration += t2-t1;
            if (ipAddress != 0L) {
                logEntry.setIpAddress(ipAddress);    
            } else {
                ++noOfFailedLookups;
            }
            ++noOfLookups;
        }
        
        if ((logEntry.getIpAddress2() == null) && (logEntry.getHostname2() != null)) {
            long t1 = System.currentTimeMillis();
            long ipAddress = IpAddressUtil.dnsLookup(logEntry.getHostname2());
            long t2 = System.currentTimeMillis();
            totalDuration2 += t2-t1;
            if (ipAddress != 0L) {
                logEntry.setIpAddress2(ipAddress);    
            } else {
                ++noOfFailedLookups2;
            }
            ++noOfLookups2;
        }
    }


    public void close() throws MegatronException {
        long noOfTotalLookups = noOfLookups + noOfLookups2;
        log.info("No. of lookups (hostname --> ip): " + noOfTotalLookups + " (" + noOfLookups + "+" + noOfLookups2 + ").");
        long noOfTotalFailedLookups = noOfFailedLookups + noOfFailedLookups2;
        log.info("No. of lookups that did not resolve (hostname --> ip): " + noOfTotalFailedLookups + " (" + noOfFailedLookups + "+" + noOfFailedLookups2 + ")."); 
        
        String totalDurationStr = DateUtil.formatDuration(totalDuration + totalDuration2);
        String durationStr = DateUtil.formatDuration(totalDuration);
        String durationStr2 = DateUtil.formatDuration(totalDuration2);
        log.info("Time for lookups (hostname --> ip): " + totalDurationStr + " (" + durationStr + "+" + durationStr2 + ").");
    }

}
