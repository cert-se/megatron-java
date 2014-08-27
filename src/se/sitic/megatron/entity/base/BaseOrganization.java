package se.sitic.megatron.entity.base;

import java.io.Serializable;


/**
 * This is an object that contains data related to the organization table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="organization"
 */

public abstract class BaseOrganization  implements Serializable {

    private static final long serialVersionUID = 1L;
    public static String REF = "Organization";
	public static String PROP_ENABLED = "Enabled";
	public static String PROP_DESCRIPTION = "Description";
	public static String PROP_EMAIL_ADDRESSES = "EmailAddresses";
	public static String PROP_MODIFIED_BY = "ModifiedBy";
	public static String PROP_COMMENT = "Comment";
	public static String PROP_REGISTRATION_NO = "RegistrationNo";
	public static String PROP_PRIORITY = "Priority";
	public static String PROP_NAME = "Name";
	public static String PROP_CREATED = "Created";
	public static String PROP_LANGUAGE_CODE = "LanguageCode";
	public static String PROP_ID = "Id";
	public static String PROP_COUNTRY_CODE = "CountryCode";
	public static String PROP_LAST_MODIFIED = "LastModified";
	public static String PROP_AUTO_UPDATE_MATCH_FIELDS = "AutoUpdateMatchFields";
	public static String PROP_AUTO_UPDATE_EMAIL = "AutoUpdateEmail";


	// constructors
	public BaseOrganization () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BaseOrganization (java.lang.Integer id) {
		this.setId(id);
		initialize();
	}

	/**
	 * Constructor for required fields
	 */
	public BaseOrganization (
		java.lang.Integer id,
		java.lang.String name,
		boolean enabled,
		java.lang.Long created,
		java.lang.Long lastModified,
		java.lang.String modifiedBy,		
		boolean autoUpdateMatchFields) {

		this.setId(id);
		this.setName(name);
		this.setEnabled(enabled);
		this.setCreated(created);
		this.setLastModified(lastModified);
		this.setModifiedBy(modifiedBy);
		this.setAutoUpdateMatchFields(autoUpdateMatchFields);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private java.lang.Integer id;

	// fields
	private java.lang.String name;
	private boolean enabled;
	private java.lang.String countryCode;
	private java.lang.String languageCode;
	private java.lang.String description;
	private java.lang.String comment;
	private java.lang.Long created;
	private java.lang.Long lastModified;
	private java.lang.String modifiedBy;
	private java.lang.String registrationNo;
	private boolean autoUpdateMatchFields;

	// many to one
	private se.sitic.megatron.entity.Priority priority;

	// collections
	private java.util.Set<se.sitic.megatron.entity.IpRange> ipRanges;
	private java.util.Set<se.sitic.megatron.entity.DomainName> domainNames;
	private java.util.Set<se.sitic.megatron.entity.ASNumber> aSNumbers;
	private java.util.Set<se.sitic.megatron.entity.Contact> contacts;



	/**
	 * Return the unique identifier of this class
     * @hibernate.id
     *  generator-class="native"
     *  column="id"
     */
	public java.lang.Integer getId () {
		return id;
	}

	/**
	 * Set the unique identifier of this class
	 * @param id the new ID
	 */
	public void setId (java.lang.Integer id) {
		this.id = id;
		this.hashCode = Integer.MIN_VALUE;
	}

	/**
	 * Return the value associated with the column: name
	 */
	public java.lang.String getName () {
		return name;
	}

	/**
	 * Set the value related to the column: name
	 * @param name the name value
	 */
	public void setName (java.lang.String name) {
		this.name = name;
	}

	/**
	 * Return the value associated with the column: enabled
	 */
	public boolean isEnabled () {
		return enabled;
	}

	/**
	 * Set the value related to the column: enabled
	 * @param enabled the enabled value
	 */
	public void setEnabled (boolean enabled) {
		this.enabled = enabled;
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
	 * Return the value associated with the column: language_code
	 */
	public java.lang.String getLanguageCode () {
		return languageCode;
	}

	/**
	 * Set the value related to the column: language_code
	 * @param languageCode the language_code value
	 */
	public void setLanguageCode (java.lang.String languageCode) {
		this.languageCode = languageCode;
	}

	/**
	 * Return the value associated with the column: description
	 */
	public java.lang.String getDescription () {
		return description;
	}

	/**
	 * Set the value related to the column: description
	 * @param description the description value
	 */
	public void setDescription (java.lang.String description) {
		this.description = description;
	}

	/**
	 * Return the value associated with the column: comment
	 */
	public java.lang.String getComment () {
		return comment;
	}

	/**
	 * Set the value related to the column: comment
	 * @param comment the comment value
	 */
	public void setComment (java.lang.String comment) {
		this.comment = comment;
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
	 * Return the value associated with the column: last_modified
	 */
	public java.lang.Long getLastModified () {
		return lastModified;
	}

	/**
	 * Set the value related to the column: last_modified
	 * @param lastModified the last_modified value
	 */
	public void setLastModified (java.lang.Long lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * Return the value associated with the column: modified_by
	 */
	public java.lang.String getModifiedBy () {
		return modifiedBy;
	}

	/**
	 * Set the value related to the column: modified_by
	 * @param modifiedBy the modified_by value
	 */
	public void setModifiedBy (java.lang.String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	/**
	 * Return the value associated with the column: registration_no
	 */
	public java.lang.String getRegistrationNo () {
		return registrationNo;
	}

	/**
	 * Set the value related to the column: registration_no
	 * @param registrationNo the registration_no value
	 */
	public void setRegistrationNo (java.lang.String registrationNo) {
		this.registrationNo = registrationNo;
	}

	/**
	 * Return the value associated with the column: auto_update_match_fields
	 */
	public boolean isAutoUpdateMatchFields () {
		return autoUpdateMatchFields;
	}

	/**
	 * Set the value related to the column: auto_update_match_fields
	 * @param autoUpdateMatchFields the auto_update_match_fields value
	 */
	public void setAutoUpdateMatchFields (boolean autoUpdateMatchFields) {
		this.autoUpdateMatchFields = autoUpdateMatchFields;
	}

	/**
	 * Return the value associated with the column: prio_id
	 */
	public se.sitic.megatron.entity.Priority getPriority () {
		return priority;
	}

	/**
	 * Set the value related to the column: prio_id
	 * @param priority the prio_id value
	 */
	public void setPriority (se.sitic.megatron.entity.Priority priority) {
		this.priority = priority;
	}

	/**
	 * Return the value associated with the column: IpRanges
	 */
	public java.util.Set<se.sitic.megatron.entity.IpRange> getIpRanges () {
		return ipRanges;
	}

	/**
	 * Set the value related to the column: IpRanges
	 * @param ipRanges the IpRanges value
	 */
	public void setIpRanges (java.util.Set<se.sitic.megatron.entity.IpRange> ipRanges) {
		this.ipRanges = ipRanges;
	}

	public void addToIpRanges (se.sitic.megatron.entity.IpRange ipRange) {
		if (null == getIpRanges()) setIpRanges(new java.util.TreeSet<se.sitic.megatron.entity.IpRange>());
		getIpRanges().add(ipRange);
	}

	/**
	 * Return the value associated with the column: DomainNames
	 */
	public java.util.Set<se.sitic.megatron.entity.DomainName> getDomainNames () {
		return domainNames;
	}

	/**
	 * Set the value related to the column: DomainNames
	 * @param domainNames the DomainNames value
	 */
	public void setDomainNames (java.util.Set<se.sitic.megatron.entity.DomainName> domainNames) {
		this.domainNames = domainNames;
	}

	public void addToDomainNames (se.sitic.megatron.entity.DomainName domainName) {
		if (null == getDomainNames()) setDomainNames(new java.util.TreeSet<se.sitic.megatron.entity.DomainName>());
		getDomainNames().add(domainName);
	}
	
	/**
	 * Return the value associated with the column: Contacts
	 */
	public java.util.Set<se.sitic.megatron.entity.Contact> getContacts () {
		return contacts;
	}
	
	// Temp method should be removed
	public  String getEmailAddresses() {
		return null;
	}

	// Temp method should be removed
	public  void setEmailAddresses(String tmp) {
		;
	}
	
	/**
	 * Set the value related to the column: Contacts
	 * @param contacts the Contacts value
	 */
	public void setContacts (java.util.Set<se.sitic.megatron.entity.Contact> contacts) {
		this.contacts = contacts;
	}

	public void addToContacts (se.sitic.megatron.entity.Contact contact) {
		if (null == getContacts()) setContacts(new java.util.TreeSet<se.sitic.megatron.entity.Contact>());
		getContacts().add(contact);
	}

	/**
	 * Return the value associated with the column: ASNumbers
	 */
	public java.util.Set<se.sitic.megatron.entity.ASNumber> getASNumbers () {
		return aSNumbers;
	}

	/**
	 * Set the value related to the column: ASNumbers
	 * @param aSNumbers the ASNumbers value
	 */
	public void setASNumbers (java.util.Set<se.sitic.megatron.entity.ASNumber> aSNumbers) {
		this.aSNumbers = aSNumbers;
	}

	public void addToASNumbers (se.sitic.megatron.entity.ASNumber aSNumber) {
		if (null == getASNumbers()) setASNumbers(new java.util.TreeSet<se.sitic.megatron.entity.ASNumber>());
		getASNumbers().add(aSNumber);
	}

	public boolean equals (Object obj) {
		if (null == obj) return false;
		if (!(obj instanceof se.sitic.megatron.entity.Organization)) return false;
		else {
			se.sitic.megatron.entity.Organization organization = (se.sitic.megatron.entity.Organization) obj;
			if (null == this.getId() || null == organization.getId()) return false;
			else return (this.getId().equals(organization.getId()));
		}
	}

	public int hashCode () {
		if (Integer.MIN_VALUE == this.hashCode) {
			if (null == this.getId()) return super.hashCode();
			else {
				String hashStr = this.getClass().getName() + ":" + this.getId().hashCode();
				this.hashCode = hashStr.hashCode();
			}
		}
		return this.hashCode;
	}

	public String toString () {
		return super.toString();
	}


}