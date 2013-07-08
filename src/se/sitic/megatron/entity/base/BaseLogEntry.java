package se.sitic.megatron.entity.base;

import java.io.Serializable;


/**
 * This is an object that contains data related to the log_entry table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="log_entry"
 */

public abstract class BaseLogEntry  implements Serializable, Comparable<Object> {

	private static final long serialVersionUID = 1L;
	public static String REF = "LogEntry";
	public static String PROP_ORGANIZATION = "Organization";
	public static String PROP_IP_ADDRESS = "IpAddress";
	public static String PROP_ASN = "Asn";
	public static String PROP_HOSTNAME2 = "Hostname2";
	public static String PROP_JOB = "Job";
	public static String PROP_ORGANIZATION2 = "Organization2";
	public static String PROP_ORIGINAL_LOG_ENTRY = "OriginalLogEntry";
	public static String PROP_PORT = "Port";
	public static String PROP_CREATED = "Created";
	public static String PROP_IP_ADDRESS2 = "IpAddress2";
	public static String PROP_URL = "Url";
	public static String PROP_COUNTRY_CODE2 = "CountryCode2";
	public static String PROP_PORT2 = "Port2";
	public static String PROP_ASN2 = "Asn2";
	public static String PROP_ID = "Id";
	public static String PROP_LOG_TIMESTAMP = "LogTimestamp";
	public static String PROP_COUNTRY_CODE = "CountryCode";
	public static String PROP_HOSTNAME = "Hostname";
	public static String PROP_IP_RANGE_START = "IpRangeStart";
	public static String PROP_IP_RANGE_END = "IpRangeEnd";


	// constructors
	public BaseLogEntry () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BaseLogEntry (Long id) {
		this.setId(id);
		initialize();
	}

	/**
	 * Constructor for required fields
	 */
	public BaseLogEntry (
		java.lang.Long  id,
		java.lang.Long created,
		java.lang.Long logTimestamp) {

		this.setId(id);
		this.setCreated(created);
		this.setLogTimestamp(logTimestamp);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private long id;

	// fields
	private java.lang.Long created;
	private java.lang.Long logTimestamp;
	private java.lang.Long ipAddress;
	private java.lang.String hostname;
	private java.lang.Integer port;
	private java.lang.Long asn;
	private java.lang.String countryCode;
	private java.lang.Long ipAddress2;
	private java.lang.String hostname2;
	private java.lang.Integer port2;
	private java.lang.Long asn2;
	private java.lang.String countryCode2;
	private java.lang.String url;
	private java.lang.Long ipRangeStart;
	private java.lang.Long ipRangeEnd;

	// many to one
	private se.sitic.megatron.entity.OriginalLogEntry originalLogEntry;
	private se.sitic.megatron.entity.Organization organization;
	private se.sitic.megatron.entity.Organization organization2;
	private se.sitic.megatron.entity.Job job;

	// collections
	private java.util.Map<String, String> additionalItems;
	private java.util.List<String> freeTexts;



	/**
	 * Return the unique identifier of this class
     * @hibernate.id
     *  generator-class="native"
     *  column="id"
     */
	public Long getId () {
		return id;
	}

	/**
	 * Set the unique identifier of this class
	 * @param id the new ID
	 */
	public void setId (Long id) {
		this.id = id;
		this.hashCode = Integer.MIN_VALUE;
	}




	/**
	 * Return the value associated with the column: created
	 */
	public java.lang.Long getCreated () {
		return created;
	}

	/**
	 * Set the value related to the column: created
	 * @param created the created value
	 */
	public void setCreated (java.lang.Long created) {
		this.created = created;
	}



	/**
	 * Return the value associated with the column: log_timestamp
	 */
	public java.lang.Long getLogTimestamp () {
		return logTimestamp;
	}

	/**
	 * Set the value related to the column: log_timestamp
	 * @param logTimestamp the log_timestamp value
	 */
	public void setLogTimestamp (java.lang.Long logTimestamp) {
		this.logTimestamp = logTimestamp;
	}



	/**
	 * Return the value associated with the column: ip_address
	 */
	public java.lang.Long getIpAddress () {
		return ipAddress;
	}

	/**
	 * Set the value related to the column: ip_address
	 * @param ipAddress the ip_address value
	 */
	public void setIpAddress (java.lang.Long ipAddress) {
		this.ipAddress = ipAddress;
	}



	/**
	 * Return the value associated with the column: hostname
	 */
	public java.lang.String getHostname () {
		return hostname;
	}

	/**
	 * Set the value related to the column: hostname
	 * @param hostname the hostname value
	 */
	public void setHostname (java.lang.String hostname) {
		this.hostname = hostname;
	}



	/**
	 * Return the value associated with the column: port
	 */
	public java.lang.Integer getPort () {
		return port;
	}

	/**
	 * Set the value related to the column: port
	 * @param port the port value
	 */
	public void setPort (java.lang.Integer port) {
		this.port = port;
	}



	/**
	 * Return the value associated with the column: asn
	 */
	public java.lang.Long getAsn () {
		return asn;
	}

	/**
	 * Set the value related to the column: asn
	 * @param asn the asn value
	 */
	public void setAsn (java.lang.Long asn) {
		this.asn = asn;
	}



	/**
	 * Return the value associated with the column: country_code
	 */
	public java.lang.String getCountryCode () {
		return countryCode;
	}

	/**
	 * Set the value related to the column: country_code
	 * @param countryCode the country_code value
	 */
	public void setCountryCode (java.lang.String countryCode) {
		this.countryCode = countryCode;
	}



	/**
	 * Return the value associated with the column: ip_address2
	 */
	public java.lang.Long getIpAddress2 () {
		return ipAddress2;
	}

	/**
	 * Set the value related to the column: ip_address2
	 * @param ipAddress2 the ip_address2 value
	 */
	public void setIpAddress2 (java.lang.Long ipAddress2) {
		this.ipAddress2 = ipAddress2;
	}



	/**
	 * Return the value associated with the column: hostname2
	 */
	public java.lang.String getHostname2 () {
		return hostname2;
	}

	/**
	 * Set the value related to the column: hostname2
	 * @param hostname2 the hostname2 value
	 */
	public void setHostname2 (java.lang.String hostname2) {
		this.hostname2 = hostname2;
	}



	/**
	 * Return the value associated with the column: port2
	 */
	public java.lang.Integer getPort2 () {
		return port2;
	}

	/**
	 * Set the value related to the column: port2
	 * @param port2 the port2 value
	 */
	public void setPort2 (java.lang.Integer port2) {
		this.port2 = port2;
	}



	/**
	 * Return the value associated with the column: asn2
	 */
	public java.lang.Long getAsn2 () {
		return asn2;
	}

	/**
	 * Set the value related to the column: asn2
	 * @param asn2 the asn2 value
	 */
	public void setAsn2 (java.lang.Long asn2) {
		this.asn2 = asn2;
	}



	/**
	 * Return the value associated with the column: country_code2
	 */
	public java.lang.String getCountryCode2 () {
		return countryCode2;
	}

	/**
	 * Set the value related to the column: country_code2
	 * @param countryCode2 the country_code2 value
	 */
	public void setCountryCode2 (java.lang.String countryCode2) {
		this.countryCode2 = countryCode2;
	}



	/**
	 * Return the value associated with the column: url
	 */
	public java.lang.String getUrl () {
		return url;
	}

	/**
	 * Set the value related to the column: url
	 * @param url the url value
	 */
	public void setUrl (java.lang.String url) {
		this.url = url;
	}



	/**
	 * Return the value associated with the column: original_log_entry_id
	 */
	public se.sitic.megatron.entity.OriginalLogEntry getOriginalLogEntry () {
		return originalLogEntry;
	}

	/**
	 * Set the value related to the column: original_log_entry_id
	 * @param originalLogEntry the original_log_entry_id value
	 */
	public void setOriginalLogEntry (se.sitic.megatron.entity.OriginalLogEntry originalLogEntry) {
		this.originalLogEntry = originalLogEntry;
	}



	/**
	 * Return the value associated with the column: org_id
	 */
	public se.sitic.megatron.entity.Organization getOrganization () {
		return organization;
	}

	/**
	 * Set the value related to the column: org_id
	 * @param organization the org_id value
	 */
	public void setOrganization (se.sitic.megatron.entity.Organization organization) {
		this.organization = organization;
	}



	/**
	 * Return the value associated with the column: org_id2
	 */
	public se.sitic.megatron.entity.Organization getOrganization2 () {
		return organization2;
	}

	/**
	 * Set the value related to the column: org_id2
	 * @param organization2 the org_id2 value
	 */
	public void setOrganization2 (se.sitic.megatron.entity.Organization organization2) {
		this.organization2 = organization2;
	}



	/**
	 * Return the value associated with the column: job_id
	 */
	public se.sitic.megatron.entity.Job getJob () {
		return job;
	}

	/**
	 * Set the value related to the column: job_id
	 * @param job the job_id value
	 */
	public void setJob (se.sitic.megatron.entity.Job job) {
		this.job = job;
	}



	/**
	 * Return the value associated with the column: AdditionalItems
	 */
	public java.util.Map<String, String> getAdditionalItems () {
		return additionalItems;
	}

	/**
	 * Set the value related to the column: AdditionalItems
	 * @param additionalItems the AdditionalItems value
	 */
	public void setAdditionalItems (java.util.Map<String, String> additionalItems) {
		this.additionalItems = additionalItems;
	}

	

	/**
	 * Return the value associated with the column: FreeTexts
	 */
	public java.util.List<String> getFreeTexts () {
		return freeTexts;
	}

	
	
	/**
	 * Set the value related to the column: FreeTexts
	 * @param freeTexts the FreeTexts value
	 */
	public void setFreeTexts (java.util.List<String> freeTexts) {
		this.freeTexts = freeTexts;
	}


	
	/**
	 * Return the value associated with the column: ip_range_start
	 */
	public java.lang.Long getIpRangeStart () {
		return ipRangeStart;
	}

	
	
	/**
	 * Set the value related to the column: ip_range_start
	 * @param ipRangeStart the ip_range_start value
	 */
	public void setIpRangeStart (java.lang.Long ipRangeStart) {
		this.ipRangeStart = ipRangeStart;
	}
	
	
	
	/**
	 * Return the value associated with the column: ip_range_end
	 */
	public java.lang.Long getIpRangeEnd () {
		return ipRangeEnd;
	}

	
	
	/**
	 * Set the value related to the column: ip_range_end
	 * @param ipRangeEnd the ip_range_end value
	 */
	public void setIpRangeEnd (java.lang.Long ipRangeEnd) {
		this.ipRangeEnd = ipRangeEnd;
	}


	
	public boolean equals (Object obj) {
		if (null == obj) return false;
		if (!(obj instanceof se.sitic.megatron.entity.LogEntry)) return false;
		else {
			se.sitic.megatron.entity.LogEntry logEntry = (se.sitic.megatron.entity.LogEntry) obj;
			return (this.getId() == logEntry.getId());
		}
	}

	public int hashCode () {
		if (Integer.MIN_VALUE == this.hashCode) {
			return (int) this.getId().intValue();
		}
		return this.hashCode;
	}


	public String toString () {
		return super.toString();
	}


}