package se.sitic.megatron.geoip;

import java.io.IOException;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.StringUtil;

import com.maxmind.geoip.LookupService;


/**
 * Returns AS number and AS name for an IP address using GeoIP.
 */
public class GeoIpAsnManager {
    private static final Logger log = Logger.getLogger(GeoIpAsnManager.class);

    private static final String GEO_IP_ASN_DATABASE_FILE = "conf/geoip-db/GeoIPASNum.dat";

    private static GeoIpAsnManager singleton;
    
    private LookupService geoIpLookup;

    
    /**
     * Constructor.
     */
    private GeoIpAsnManager(TypedProperties props) throws MegatronException {
        init(props);
    }

    
    /**
     * Returns singleton. Must be a singleton; MaxMind's database is copied to memory
     * and multiple instance may cause OutOfMemoryError. 
     */
    public synchronized static GeoIpAsnManager getInstance(TypedProperties props) throws MegatronException {
        if (singleton == null) {
            singleton = new GeoIpAsnManager(props);
        }
        return singleton;
    }

    
    public As getAs(long ipAddress) {
        As result = null;
        
        try {
            String asStr = geoIpLookup.getOrg(ipAddress);
            if (!StringUtil.isNullOrEmpty(asStr)) {
                log.debug("AS for ip-address " + ipAddress + ": " + asStr);
                result = parseAs(asStr);
            }
        } catch (RuntimeException e) {
            // in some rare cases com.maxmind.geoip.LookupService.getOrg throws ArrayIndexOutOfBoundsException
            log.error("Cannot lookup AS from ip-adress: " + ipAddress, e);
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
      String dbFile = props.getString(AppProperties.GEO_IP_ASN_DATABASE_FILE_KEY, GEO_IP_ASN_DATABASE_FILE);
      try {
          geoIpLookup = new LookupService(dbFile, LookupService.GEOIP_MEMORY_CACHE);
      } catch (IOException e) {
          String msg = "Cannot initialize GeoIP-service. Database need to be downloaded (see conf/geoip-db/readme.txt) Database file: " + dbFile;
          throw new MegatronException(msg, e);
      }
    }
    
    
    private As parseAs(String asStr) {
        long asNumber = 0;
        String asName = null;
        // Format: ASXXX Name
        String[] numName = StringUtil.splitHeadTail(asStr, " ", false);
        if ((numName[0].length() > 2) && numName[0].startsWith("AS")) {
            String asNumberStr = numName[0].substring(2);
            try {
                asNumber = Long.parseLong(asNumberStr);
            } catch (NumberFormatException e) {
                log.error("Cannot parse AS number: " + asNumberStr, e);
            }
        
            asName = numName[1];
        }
        return new As(asNumber, asName);
    }

}
