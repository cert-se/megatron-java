package se.sitic.megatron.geoip;


/**
 * Entity class for an AS. Contains AS number and AS name.
 */
public class As {
    private long asNumber;
    private String asName;

    
    public As(long asNumber, String asName) {
        this.asNumber = asNumber;
        this.asName = asName;
    }


    public long getAsNumber() {
        return asNumber;
    }


    public String getAsName() {
        return asName;
    }

    
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer(64);
        
        result.append("AS");
        result.append(asNumber);
        result.append(" (");
        result.append(asName);
        result.append(")");
        return result.toString();
    }
    
}
