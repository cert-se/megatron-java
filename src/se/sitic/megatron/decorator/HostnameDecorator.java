package se.sitic.megatron.decorator;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.AppUtil;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.IpAddressUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Adds hostname if missing and ip-address exists. 
 */
public class HostnameDecorator implements IDecorator {
    private static final Logger log = Logger.getLogger(HostnameDecorator.class);    

    private long noOfLookups;
    private long noOfLookups2;
    private long noOfFailedLookups;
    private long noOfFailedLookups2;
    private long totalDuration;
    private long totalDuration2;    

    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        // empty
    }


    @Override
    public void execute(LogEntry logEntry) throws MegatronException {
        List<Long> ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
        Iterator<Long> iterator = (ipAddresses != null) ? ipAddresses.iterator() : null;
        while ((logEntry.getHostname() == null) && (iterator != null) && iterator.hasNext()) {
            long t1 = System.currentTimeMillis();
            String hostname = IpAddressUtil.reverseDnsLookup(iterator.next());
            long t2 = System.currentTimeMillis();
            totalDuration += t2-t1;
            if (!StringUtil.isNullOrEmpty(hostname)) {
                logEntry.setHostname(hostname);    
            } else {
                ++noOfFailedLookups;
            }
            ++noOfLookups;
        }
        
        if ((logEntry.getHostname2() == null) && (logEntry.getIpAddress2() != null)) {
            long t1 = System.currentTimeMillis();
            String hostname = IpAddressUtil.reverseDnsLookup(logEntry.getIpAddress2());
            long t2 = System.currentTimeMillis();
            totalDuration2 += t2-t1;
            if (!StringUtil.isNullOrEmpty(hostname)) {
                logEntry.setHostname2(hostname);    
            } else {
                ++noOfFailedLookups2;
            }
            ++noOfLookups2;
        }
    }


    @Override
    public void close() throws MegatronException {
        long noOfTotalLookups = noOfLookups + noOfLookups2;
        log.info("No. of lookups (ip --> hostname): " + noOfTotalLookups + " (" + noOfLookups + "+" + noOfLookups2 + ")."); 
        long noOfTotalFailedLookups = noOfFailedLookups + noOfFailedLookups2;
        log.info("No. of lookups that did not resolve (ip --> hostname): " + noOfTotalFailedLookups + " (" + noOfFailedLookups + "+" + noOfFailedLookups2 + ")."); 
        
        String totalDurationStr = DateUtil.formatDuration(totalDuration + totalDuration2);
        String durationStr = DateUtil.formatDuration(totalDuration);
        String durationStr2 = DateUtil.formatDuration(totalDuration2);
        log.info("Time for lookups (ip --> hostname): " + totalDurationStr + " (" + durationStr + "+" + durationStr2 + ").");
    }

}
