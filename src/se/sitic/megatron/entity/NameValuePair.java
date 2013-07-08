package se.sitic.megatron.entity;


/**
 * Entity class (data holder) for a name-value pair.
 */
public class NameValuePair {
    private String name;
    private String value;


    /**
     * Constructor.
     */
    public NameValuePair(String name, String value) {
        this.name = name;
        this.value = value;
    }


    public String getName() {
        return name;
    }


    public String getValue() {
        return value;
    }


    @Override
    public String toString() {
        int len = ((name != null) ? name.length() : 0) + ((value != null) ? value.length() : 0);
        len += Math.max(len, 15) + 1;
        StringBuilder result = new StringBuilder(len);
        result.append(name).append('=').append(value);
        return result.toString();
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof NameValuePair))
            return false;
        final NameValuePair other = (NameValuePair) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
