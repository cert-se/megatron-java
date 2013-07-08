package se.sitic.megatron.entity.base;

import java.io.Serializable;


/**
 * This is an object that contains data related to the domain_name table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="domain_name"
 */

public abstract class BaseDomainName  implements Serializable, Comparable<Object> {

	private static final long serialVersionUID = 1L;
	public static String REF = "DomainName";
	public static String PROP_NAME = "Name";
	public static String PROP_ID = "Id";
	public static String PROP_ORGANIZATION_ID = "OrganizationId";


	// constructors
	public BaseDomainName () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BaseDomainName (java.lang.Integer id) {
		this.setId(id);
		initialize();
	}

	/**
	 * Constructor for required fields
	 */
	public BaseDomainName (
		java.lang.Integer id,
		java.lang.Integer organizationId,
		java.lang.String name) {

		this.setId(id);
		this.setOrganizationId(organizationId);
		this.setName(name);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private java.lang.Integer id;

	// fields
	private java.lang.String name;
	private java.lang.Integer organizationId;



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
	 * Return the value associated with the column: domain_name
	 */
	public java.lang.String getName () {
		return name;
	}

	/**
	 * Set the value related to the column: domain_name
	 * @param name the domain_name value
	 */
	public void setName (java.lang.String name) {
		this.name = name;
	}



	/**
	 * Return the value associated with the column: org_id
	 */
	public java.lang.Integer getOrganizationId () {
		return organizationId;
	}

	/**
	 * Set the value related to the column: org_id
	 * @param organizationId the org_id value
	 */
	public void setOrganizationId (java.lang.Integer organizationId) {
		this.organizationId = organizationId;
	}




	public boolean equals (Object obj) {
		if (null == obj) return false;
		if (!(obj instanceof se.sitic.megatron.entity.DomainName)) return false;
		else {
			se.sitic.megatron.entity.DomainName domainName = (se.sitic.megatron.entity.DomainName) obj;
			if (null == this.getId() || null == domainName.getId()) return false;
			else return (this.getId().equals(domainName.getId()));
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