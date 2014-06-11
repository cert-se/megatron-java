package se.sitic.megatron.decorator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.geoip.As;
import se.sitic.megatron.geoip.GeoIpAsnManager;
import se.sitic.megatron.util.AppUtil;


/**
 * Adds AS number and name if missing and ip-address exists.
 * <p>
 * Using MaxMind's free ASN database.
 * <p>
 * One alternative is the AsnDecorator, which uses imported data from a BPG table.
 * AsnDecorator is more precise but needs BPG data. AsnGeoIpDecorator will return
 * AS name in addition to AS number.
 */
public class AsnGeoIpDecorator implements IDecorator {
    private static final Logger log = Logger.getLogger(AsnGeoIpDecorator.class);

    public static final String AS_NUMBER = "asn";
    public static final String AS_NUMBER2 = "asn2";
    public static final String AS_NAME = "asName";
    public static final String AS_NAME2 = "asName2";
    
    private GeoIpAsnManager geoIpAsnManager;
    private boolean useAsnInLogEntry;
    private boolean addAsName;
    private long noOfLookups;
    private long noOfLookups2;

    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        geoIpAsnManager = GeoIpAsnManager.getInstance(props);
        useAsnInLogEntry = props.getBoolean(AppProperties.DECORATOR_USE_ASN_IN_LOG_ENTRY_KEY, true);
        addAsName = props.getBoolean(AppProperties.DECORATOR_ADD_AS_NAME_KEY, false);
    }


    @Override
    public void execute(LogEntry logEntry) throws MegatronException {
        List<Long> ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
        Iterator<Long> iterator = (ipAddresses != null) ? ipAddresses.iterator() : null;
        while (!existsAsn(logEntry, true) && (iterator != null) && iterator.hasNext()) {
            As as = geoIpAsnManager.getAs(iterator.next()); 
            if (as != null) {
                if (useAsnInLogEntry) {
                    logEntry.setAsn(new Long(as.getAsNumber()));    
                } else {
                    addAdditionalItem(logEntry, AS_NUMBER, as.getAsNumber() + "");
                }
                if (addAsName) {
                    addAdditionalItem(logEntry, AS_NAME, as.getAsName());
                }
            }
            ++noOfLookups;
        }
        ensureAdditionalItems(logEntry, true);

        if (!existsAsn(logEntry, false) && (logEntry.getIpAddress2() != null)) {
            As as = geoIpAsnManager.getAs(logEntry.getIpAddress2().longValue());
            if (as != null) {
                if (useAsnInLogEntry) {
                    logEntry.setAsn2(new Long(as.getAsNumber()));    
                } else {
                    addAdditionalItem(logEntry, AS_NUMBER2, as.getAsNumber() + "");
                }
                if (addAsName) {
                    addAdditionalItem(logEntry, AS_NAME2, as.getAsName());
                }
            }
            ++noOfLookups2;
        }
        ensureAdditionalItems(logEntry, false);
    }

    
    @Override
    public void close() throws MegatronException {
        long noOfTotalLookups = noOfLookups + noOfLookups2;
        log.info("No. of lookups by AsnGeoIpDecorator (ip --> asn): " + noOfTotalLookups + " (" + noOfLookups + "+" + noOfLookups2 + ")."); 

        geoIpAsnManager.close();
    }
    
    
    private boolean existsAsn(LogEntry logEntry, boolean primaryAsn) {
        boolean result = true;
        if (useAsnInLogEntry) {
            Long asn = primaryAsn ? logEntry.getAsn() : logEntry.getAsn2();
            result = (asn != null);
        } else {
            Map<String, String> map = logEntry.getAdditionalItems();
            if (map != null) {
                String key = primaryAsn ? AS_NUMBER : AS_NUMBER2;
                result = (map.get(key) != null);
            } else {
                result = false;
            }
        }
        return result;
    }
    
    
    private void addAdditionalItem(LogEntry logEntry, String key, String value) {
        Map<String, String> map = logEntry.getAdditionalItems();
        if (map == null) {
            map = new HashMap<String, String>();
        }
        map.put(key, value);
    }

    
    /**
     * Ensure that all log entries have additional items. If an additional 
     * item is missing for one log entry the export will fail (if variable
     * is present in template). 
     */
    private void ensureAdditionalItems(LogEntry logEntry, boolean primaryAsn) {
        Map<String, String> map = logEntry.getAdditionalItems();
        if (map == null) {
            map = Collections.emptyMap(); 
        }
        
        String key = primaryAsn ? AS_NUMBER : AS_NUMBER2; 
        if (!useAsnInLogEntry && (map.get(key) == null)) {
            addAdditionalItem(logEntry, key, "");
        }
     
        key = primaryAsn ? AS_NAME : AS_NAME2;
        if (addAsName && (map.get(key) == null)) {
            addAdditionalItem(logEntry, key, "");
        }
    }

}
