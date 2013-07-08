package se.sitic.megatron.geoip;

import java.io.IOException;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;


/**
 * Returns geolocation (latitude/longitude, country, city etc.) for an IP address using GeoIP.
 */
public class GeoIpCityManager {
    private static final Logger log = Logger.getLogger(GeoIpCityManager.class);

    private static final String GEO_IP_CITY_DATABASE_FILE = "conf/geoip-db/GeoLiteCity.dat";

    private static GeoIpCityManager singleton;
    
    private LookupService geoIpLookup;
    
    
    /**
     * Constructor.
     */
    private GeoIpCityManager(TypedProperties props) throws MegatronException {
        init(props);
    }
    
    
    /**
     * Returns singleton. Must be a singleton; MaxMind's database is copied to memory
     * and multiple instance may cause OutOfMemoryError. 
     */
    public synchronized static GeoIpCityManager getInstance(TypedProperties props) throws MegatronException {
        if (singleton == null) {
            singleton = new GeoIpCityManager(props);
        }
        return singleton;
    }

    
    public Geolocation getGeolocation(long ipAddress) {
        Geolocation result = null;
        
        Location location = geoIpLookup.getLocation(ipAddress);
        if (location != null) {
            if (log.isDebugEnabled()) {
                log.debug("Geolocation for ip-address " + ipAddress + ": " + location.latitude + "/" + location.longitude + " (" + location.countryCode + ")");
            }
            result = new Geolocation(location);
        }
        
        return result;
    }

    
    public Country getCountry(long ipAddress) {
        // The following line does not work; throws ArrayIndexOutOfBoundsException  
        // return geoIpLookup.getCountry(ipAddress);

        Country result = null;
        Location location = geoIpLookup.getLocation(ipAddress);
        if ((location != null) && (location.countryCode != null) && !location.countryCode.equals("--")) {
            result = new Country(location.countryCode, location.countryName);
        }        
        return result;
    }
    
    
    public void close() {
// Cannot close due to singleton
//        try {
//            geoIpLookup.close();
//        } catch (Exception ignored) {
//            // empty
//        }
    }


    private void init(TypedProperties props) throws MegatronException {
      String dbFile = props.getString(AppProperties.GEO_IP_CITY_DATABASE_FILE_KEY, GEO_IP_CITY_DATABASE_FILE);
      try {
          geoIpLookup = new LookupService(dbFile, LookupService.GEOIP_MEMORY_CACHE);
      } catch (IOException e) {
          String msg = "Cannot initialize GeoIP-service. Database need to be downloaded (see conf/geoip-db/readme.txt) Database file: " + dbFile;
          throw new MegatronException(msg, e);
      }
    }
    
}
