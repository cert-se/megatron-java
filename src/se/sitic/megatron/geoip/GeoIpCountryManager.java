package se.sitic.megatron.geoip;

import java.io.IOException;
import java.util.Locale;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.StringUtil;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.LookupService;


/**
 * Returns country for an ip-address using GeoIp.
 */
public class GeoIpCountryManager {
    private static final Logger log = Logger.getLogger(GeoIpCountryManager.class);

    public static final String COUNTRY_CODE_UNKNOWN = "-";
    
    private static final String GEO_IP_DATABASE_FILE = "conf/GeoIP.dat";

    private static GeoIpCountryManager singleton;
    
    private LookupService geoIpLookup;
    private GeoIpCityManager geoIpCityManager;
    
    
    /**
     * Constructor.
     */
    private GeoIpCountryManager(TypedProperties props) throws MegatronException {
        init(props);
    }

    
    /**
     * Returns singleton. Must be a singleton; MaxMind's database is copied to memory
     * and multiple instance may cause OutOfMemoryError. 
     */
    public synchronized static GeoIpCountryManager getInstance(TypedProperties props) throws MegatronException {
        if (singleton == null) {
            singleton = new GeoIpCountryManager(props);
        }
        return singleton;
    }

    
    /**
     * Returns country code for specified ip-addrees, or "-" if lookup fails.
     * By using "-" as "unknown country code", these can be filtred out by
     * CountryCodeFilter.
     */
    public String getCountry(long ipAddress, boolean includeCountryName) {
        StringBuilder result = new StringBuilder(64);
        
        Country country = null;
        try {
            if (geoIpCityManager != null) {
                country = geoIpCityManager.getCountry(ipAddress);
            } else {
                country = geoIpLookup.getCountry(ipAddress); 
            }
        } catch (RuntimeException e) {
            // in some rare cases com.maxmind.geoip.LookupService.getCountry throws ArrayIndexOutOfBoundsException
            log.error("Cannot lookup country code from ip-adress: " + ipAddress, e);
        }

        if (country != null) {
            log.debug("Country for ip-address " + ipAddress + ": " + country.getCode());
            String countryCode = country.getCode();
            if (StringUtil.isNullOrEmpty(countryCode) || countryCode.equals("--")) {
                countryCode = COUNTRY_CODE_UNKNOWN;
            }
            result.append(countryCode);
            if ((includeCountryName) && !countryCode.equals(COUNTRY_CODE_UNKNOWN)) {
                Locale locale = Locale.US;
                String name = locale.getDisplayCountry();
                result.append(" (").append(name).append(")");
            }
        } else {
            result.append(COUNTRY_CODE_UNKNOWN);
        }
        
        return result.toString();
    }

    
    public void close() {
// Cannot close due to singleton        
//        try {
//            if (geoIpLookup != null) {
//                geoIpLookup.close();
//            }
//            if (geoIpCityManager != null) {
//                geoIpCityManager.close();
//            }
//        } catch (Exception ignored) {
//            // empty
//        }
    }

    
    private void init(TypedProperties props) throws MegatronException {
        boolean useCity = props.getBoolean(AppProperties.GEO_IP_USE_CITY_DATABASE_FOR_COUNTRY_LOOKUPS_KEY, false);
        if (useCity) {
            initCity(props);
        } else {
            initCountry(props);
        }
    }

    
    private void initCountry(TypedProperties props) throws MegatronException {
        String dbFile = props.getString(AppProperties.GEO_IP_COUNTRY_DATABASE_FILE_KEY, null);
        if (dbFile == null) {
            // Use the old deprecated property name
            dbFile = props.getString(AppProperties.GEO_IP_DATABASE_FILE_KEY, GEO_IP_DATABASE_FILE);
        }
        try {
            geoIpLookup = new LookupService(dbFile, LookupService.GEOIP_MEMORY_CACHE);
        } catch (IOException e) {
            String msg = "Cannot initialize GeoIP-service. Database file: " + dbFile;
            throw new MegatronException(msg, e);
        }
        log.info("Country lookups will use the MaxMind country database.");
    }
    
    
    private void initCity(TypedProperties props) throws MegatronException {
        this.geoIpCityManager = GeoIpCityManager.getInstance(props);
        log.info("Country lookups will use the MaxMind city database.");
    }

}
