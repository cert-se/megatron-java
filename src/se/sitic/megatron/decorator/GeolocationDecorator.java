package se.sitic.megatron.decorator;

import java.util.Arrays;
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
import se.sitic.megatron.geoip.GeoIpCityManager;
import se.sitic.megatron.geoip.Geolocation;
import se.sitic.megatron.util.AppUtil;


/**
 * Adds geolocation data (latitude, longitude, city etc.) as additional items.
 * <p>  
 * Using MaxMind's free or commercial City database.
 */
public class GeolocationDecorator implements IDecorator {
    private static final Logger log = Logger.getLogger(GeolocationDecorator.class);
    
    private static final String[] FIELDS_DEFAULT = { "latitude", "longitude", "city" }; 
    
    private GeoIpCityManager geoIpCityManager;
    private List<String> fields;
    private boolean lookupIpAddress;
    private boolean lookupIpAddress2;
    private long noOfLookups;
    private long noOfLookups2;
    
    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        geoIpCityManager = GeoIpCityManager.getInstance(props);
        String[] fieldArray = props.getStringListFromCommaSeparatedValue(AppProperties.DECORATOR_GEOLOCATION_FIELDS_TO_ADD_KEY, FIELDS_DEFAULT, true);
        fields = Arrays.asList(fieldArray); 
        
        lookupIpAddress = false;
        lookupIpAddress2 = false;
        for (int i = 0; i < fieldArray.length; i++) {
            if (fieldArray[i].endsWith("2")) {
                lookupIpAddress2 = true;
            } else {
                lookupIpAddress = true;
            }
        }
    }

    
    @Override
    public void execute(LogEntry logEntry) throws MegatronException {
        // Note: We must ensure that all specified fields are added to additional items,
        // even if lookup returns null. Otherwise the export might fail.
        
        boolean additionalItemsAdded = false;
        if (lookupIpAddress) {
            List<Long> ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
            Iterator<Long> iterator = (ipAddresses != null) ? ipAddresses.iterator() : null;
            Geolocation geolocation = null;
            while ((geolocation == null) && (iterator != null) && iterator.hasNext()) {
                geolocation = geoIpCityManager.getGeolocation(iterator.next());
                if (geolocation != null) {
                    addAdditionalItems(logEntry, geolocation, true);
                    additionalItemsAdded = true;
                }
                ++noOfLookups;
            }
        }
        if (!additionalItemsAdded) {
            addAdditionalItems(logEntry, Geolocation.EMPTY_GEOLOCATION, true);
        }

        additionalItemsAdded = false;
        if (lookupIpAddress2 && (logEntry.getIpAddress2() != null)) {
            Geolocation geolocation = geoIpCityManager.getGeolocation(logEntry.getIpAddress2());
            if (geolocation != null) {
                addAdditionalItems(logEntry, geolocation, false);
                additionalItemsAdded = true;
            }
            ++noOfLookups2;
        }
        if (!additionalItemsAdded) {
            addAdditionalItems(logEntry, Geolocation.EMPTY_GEOLOCATION, false);
        }
    }
    
    
    @Override
    public void close() throws MegatronException {
        long noOfTotalLookups = noOfLookups + noOfLookups2;
        log.info("No. of lookups (ip --> geolocation): " + noOfTotalLookups + " (" + noOfLookups + "+" + noOfLookups2 + ")."); 
        geoIpCityManager.close();
    }
    
    
    private void addAdditionalItems(LogEntry logEntry, Geolocation geolocation, boolean primaryIp) throws MegatronException {
        for (Iterator<String> iterator = fields.iterator(); iterator.hasNext(); ) {
            String field = iterator.next();
            if ((primaryIp && field.endsWith("2")) || (!primaryIp && !field.endsWith("2"))) {
                continue;
            }
            if ("latitude".equals(field) || "latitude2".equals(field)) {
                addAdditionalItem(logEntry, field, geolocation.getLatitude() + "");
            } else if ("longitude".equals(field) || "longitude2".equals(field)) {
                addAdditionalItem(logEntry, field, geolocation.getLongitude() + "");
            } else if ("city".equals(field) || "city2".equals(field)) {
                addAdditionalItem(logEntry, field, geolocation.getCity());
            } else if ("countryCode".equals(field) || "countryCode2".equals(field)) {
                addAdditionalItem(logEntry, field, geolocation.getCountryCode());
            } else if ("countryName".equals(field) || "countryName2".equals(field)) {
                addAdditionalItem(logEntry, field, geolocation.getCountryName());
            } else if ("region".equals(field) || "region2".equals(field)) {
                addAdditionalItem(logEntry, field, geolocation.getRegion());
            } else if ("postalCode".equals(field) || "postalCode2".equals(field)) {
                addAdditionalItem(logEntry, field, geolocation.getPostalCode());
            } else if ("areaCode".equals(field) || "areaCode2".equals(field)) {
                addAdditionalItem(logEntry, field, geolocation.getAreaCode() + "");
            } else if ("metroCode".equals(field) || "metroCode2".equals(field)) {
                addAdditionalItem(logEntry, field, geolocation.getMetroCode() + "");
            } else {
                String msg = "Invalid field in 'decorator.geolocationDecorator.fieldsToAdd'. Unknown field: " + field;
                throw new MegatronException(msg);
            }
        }
    }

    
    private void addAdditionalItem(LogEntry logEntry, String key, String value) {
        Map<String, String> map = logEntry.getAdditionalItems();
        if (map == null) {
            map = new HashMap<String, String>();
        }
        map.put(key, value);
    }
    
}
