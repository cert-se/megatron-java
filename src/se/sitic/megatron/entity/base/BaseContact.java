package se.sitic.megatron.entity.base;

import java.io.Serializable;

/**
 * This is an object that contains data related to the contact table. Do not
 * modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 * 
 * @hibernate.class table="contact"
 */

public abstract class BaseContact implements Serializable, Comparable<Object> {

    private static final long serialVersionUID = 1L;
    public static String REF = "Contact";
    public static String PROP_ENABLED = "Enabled";
    public static String PROP_MODIFIED_BY = "ModifiedBy";
    public static String PROP_COMMENT = "Comment";
    public static String PROP_FIRST_NAME = "FirstName";
    public static String PROP_LAST_NAME = "LastName";
    public static String PROP_EMAIL_ADDRESS = "EmailAddress";
    public static String PROP_EMAIL_TYPE = "EmailType";
    public static String PROP_PHONE_NUMBER = "PhoneNumber";
    public static String PROP_ROLE = "Role";
    public static String PROP_CREATED = "Created";
    public static String PROP_ID = "Id";
    public static String PROP_LAST_MODIFIED = "LastModified";
    public static String PROP_AUTO_UPDATE_EMAIL = "AutoUpdateEmail";
    public static String PROP_EXTERNAL_REFERENCE = "ExternalReference";
    public static String PROP_ORGANIZATION_ID = "OrganizationId";

    // constructors
    public BaseContact() {
        initialize();
    }

    /**
     * Constructor for primary key
     */
    public BaseContact(java.lang.Integer id) {
        this.setId(id);
        initialize();
    }

    /**
     * Constructor for required fields
     */
    public BaseContact(java.lang.Integer id, java.lang.String firstName,
            java.lang.String lastName, java.lang.String comment,
            java.lang.String emailAddress, java.lang.String emailType,
            java.lang.String phoneNumber, java.lang.String role,
            java.lang.String externalReference,
            java.lang.Integer organizationId, boolean enabled,
            java.lang.Long created, java.lang.Long lastModified,
            java.lang.String modifiedBy, boolean autoUpdateEmail) {

        this.setId(id);
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.setComment(comment);
        this.setEmailAddress(emailAddress);
        this.setEmailType(emailType);
        this.setPhoneNumber(phoneNumber);
        this.setRole(role);
        this.setExternalReference(externalReference);
        this.setOrganizationId(organizationId);
        this.setEnabled(enabled);
        this.setCreated(created);
        this.setLastModified(lastModified);
        this.setModifiedBy(modifiedBy);
        this.setAutoUpdateEmail(autoUpdateEmail);
        initialize();
    }

    protected void initialize() {
    }

    private int hashCode = Integer.MIN_VALUE;

    // primary key
    private java.lang.Integer id;

    // fields
    private java.lang.String firstName;
    private java.lang.String lastName;
    private java.lang.String comment;
    private java.lang.String emailAddress;
    private java.lang.String emailType;
    private java.lang.String phoneNumber;
    private java.lang.String role;
    private java.lang.String externalReference;
    private java.lang.Integer organizationId;
    private boolean enabled;
    private java.lang.Long created;
    private java.lang.Long lastModified;
    private java.lang.String modifiedBy;
    private boolean autoUpdateEmail;

    /**
     * Return the unique identifier of this class
     * 
     * @hibernate.id generator-class="native" column="id"
     */
    public java.lang.Integer getId() {
        return id;
    }

    /**
     * Set the unique identifier of this class
     * 
     * @param id
     *            the new ID
     */
    public void setId(java.lang.Integer id) {
        this.id = id;
        this.hashCode = Integer.MIN_VALUE;
    }

    /**
     * Return the value associated with the column: org_id
     */
    public java.lang.Integer getOrganizationId() {
        return this.organizationId;
    }

    /**
     * Set the value related to the column: org_id
     * 
     * @param organizationId
     *            the org_id value
     */
    public void setOrganizationId(java.lang.Integer organizationId) {
        this.organizationId = organizationId;
    }

    /**
     * Return the value associated with the column: first_name
     */
    public java.lang.String getFirstName() {
        return firstName;
    }

    /**
     * Set the value related to the column: first_name
     * 
     * @param name
     *            the name value
     */
    public void setFirstName(java.lang.String firstName) {
        this.firstName = firstName;
    }

    /**
     * Return the value associated with the column: last_name
     */
    public java.lang.String getLastName() {
        return lastName;
    }

    /**
     * Set the value related to the column: last_name
     * 
     * @param name
     *            the name value
     */
    public void setLastName(java.lang.String lastName) {
        this.lastName = lastName;
    }

    /**
     * Return the value associated with the column: comment
     */
    public java.lang.String getComment() {
        return comment;
    }

    /**
     * Set the value related to the column: last_name
     * 
     * @param name
     *            the name value
     */
    public void setComment(java.lang.String comment) {
        this.comment = comment;
    }

    /**
     * Return the value associated with the column: enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the value related to the column: enabled
     * 
     * @param enabled
     *            the enabled value
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the value associated with the column: email_address
     */
    public java.lang.String getEmailAddress() {
        return emailAddress;
    }

    /**
     * Set the value related to the column: email_address
     * 
     * @param emailAddress
     *            the email_address value
     */
    public void setEmailAddress(java.lang.String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * Return the value associated with the column: email_type
     */
    public java.lang.String getEmailType() {
        return emailType;
    }

    /**
     * Set the value related to the column: email_type
     * 
     * @param emailType
     *            the email_type value
     */
    public void setEmailType(java.lang.String emailType) {
        this.emailType = emailType;
    }

    /**
     * Return the value associated with the column: phone_number
     */
    public java.lang.String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Set the value related to the column: phone_number
     * 
     * @param phoneNumber
     *            the phone_number value
     */
    public void setPhoneNumber(java.lang.String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Return the value associated with the column: role
     */
    public java.lang.String getRole() {
        return role;
    }

    /**
     * Set the value related to the column: role
     * 
     * @param role
     *            the role value
     */
    public void setRole(java.lang.String role) {
        this.role = role;
    }

    /**
     * Return the value associated with the column: external_reference
     */
    public java.lang.String getExternalReference() {
        return externalReference;
    }

    /**
     * Set the value related to the column: external_reference
     * 
     * @param emailAddress
     *            the external_reference value
     */
    public void setExternalReference(java.lang.String externalReference) {
        this.externalReference = externalReference;
    }

    /**
     * Return the value associated with the column: created
     */
    public java.lang.Long getCreated() {
        return created;
    }

    /**
     * Set the value related to the column: created
     * 
     * @param created
     *            the created value
     */
    public void setCreated(java.lang.Long created) {
        this.created = created;
    }

    /**
     * Return the value associated with the column: last_modified
     */
    public java.lang.Long getLastModified() {
        return lastModified;
    }

    /**
     * Set the value related to the column: last_modified
     * 
     * @param lastModified
     *            the last_modified value
     */
    public void setLastModified(java.lang.Long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Return the value associated with the column: modified_by
     */
    public java.lang.String getModifiedBy() {
        return modifiedBy;
    }

    /**
     * Set the value related to the column: modified_by
     * 
     * @param modifiedBy
     *            the modified_by value
     */
    public void setModifiedBy(java.lang.String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    /**
     * Return the value associated with the column: auto_update_email
     */
    public boolean isAutoUpdateEmail() {
        return autoUpdateEmail;
    }

    /**
     * Set the value related to the column: auto_update_email
     * 
     * @param autoUpdateEmail
     *            the auto_update_email value
     */
    public void setAutoUpdateEmail(boolean autoUpdateEmail) {
        this.autoUpdateEmail = autoUpdateEmail;
    }

    public boolean equals(Object obj) {
        if (null == obj)
            return false;
        if (!(obj instanceof se.sitic.megatron.entity.Contact))
            return false;
        else {
            se.sitic.megatron.entity.Contact contact = (se.sitic.megatron.entity.Contact) obj;
            if (null == this.getId() || null == contact.getId())
                return false;
            else
                return (this.getId().equals(contact.getId()));
        }
    }

    public int hashCode() {
        if (Integer.MIN_VALUE == this.hashCode) {
            if (null == this.getId())
                return super.hashCode();
            else {
                String hashStr = this.getClass().getName() + ":"
                        + this.getId().hashCode();
                this.hashCode = hashStr.hashCode();
            }
        }
        return this.hashCode;
    }

    public String toString() {
        return super.toString();
    }

}