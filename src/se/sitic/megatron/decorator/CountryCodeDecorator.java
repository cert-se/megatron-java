package se.sitic.megatron.decorator;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.geoip.GeoIpCountryManager;
import se.sitic.megatron.util.AppUtil;


/**
 * Adds country code if missing and ip-address exists.
 */
public class CountryCodeDecorator implements IDecorator {
    private static final Logger log = Logger.getLogger(CountryCodeDecorator.class);    

    private GeoIpCountryManager geoIpManager;
    private long noOfLookups;
    private long noOfLookups2;

    
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        geoIpManager = GeoIpCountryManager.getInstance(props);
    }

    
    public void execute(LogEntry logEntry) throws MegatronException {
        List<Long> ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
        Iterator<Long> iterator = (ipAddresses != null) ? ipAddresses.iterator() : null;
        while ((logEntry.getCountryCode() == null) && (iterator != null) && iterator.hasNext()) {
            String countryCode = geoIpManager.getCountry(iterator.next(), false);
            logEntry.setCountryCode(countryCode);
            ++noOfLookups;
        }
        
        if ((logEntry.getCountryCode2() == null) && (logEntry.getIpAddress2() != null)) {
            String countryCode = geoIpManager.getCountry(logEntry.getIpAddress2(), false);
            logEntry.setCountryCode2(countryCode);
            ++noOfLookups2;
        }
    }

    
    public void close() {
        long noOfTotalLookups = noOfLookups + noOfLookups2;
        log.info("No. of lookups (ip --> country code): " + noOfTotalLookups + " (" + noOfLookups + "+" + noOfLookups2 + ")."); 
        geoIpManager.close();
    }

}
