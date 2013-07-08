package se.sitic.megatron.geoip;

import com.maxmind.geoip.Location;


/**
 * Wrapper for com.maxmind.geoip.Location.
 */
public class Geolocation {
    public static final Geolocation EMPTY_GEOLOCATION = new Geolocation();
    
    private Location location; 

    
    /**
     * Constructs an instance with empty values.
     */
    private Geolocation() {
        this.location = new Location();
    }

    
    /**
     * Constructs an instance with specified location.
     */
    public Geolocation(Location location) {
        this.location = location;
    }


    public String getCountryCode() {
        return (location.countryCode != null) ? location.countryCode : "";
    }

    
    public String getCountryName() {
        return (location.countryName != null) ? location.countryName : "";
    }

    
    public String getRegion() {
        return (location.region != null) ? location.region : "";
    }

    
    public String getCity() {
        return (location.city != null) ? location.city : "";
    }

    
    public String getPostalCode() {
        return (location.postalCode != null) ? location.postalCode : "";
    }

    
    public float getLatitude() {
        return location.latitude;
    }

    
    public float getLongitude() {
        return location.longitude;
    }

    
    public int getDmaCode() {
        return location.dma_code;
    }

    
    public int getAreaCode() {
        return location.area_code;
    }

    
    public int getMetroCode() {
        return location.metro_code;
    }
    
}
