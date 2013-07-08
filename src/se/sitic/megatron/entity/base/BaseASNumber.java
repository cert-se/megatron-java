package se.sitic.megatron.entity.base;

import java.io.Serializable;


/**
 * This is an object that contains data related to the asn table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="asn"
 */

public abstract class BaseASNumber  implements Serializable, Comparable<Object>{

	private static final long serialVersionUID = 1L;
	public static String REF = "ASNumber";
	public static String PROP_NUMBER = "Number";
	public static String PROP_ID = "Id";
	public static String PROP_ORGANIZATION_ID = "OrganizationId";


	// constructors
	public BaseASNumber () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BaseASNumber (java.lang.Integer id) {
		this.setId(id);
		initialize();
	}

	/**
	 * Constructor for required fields
	 */
	public BaseASNumber (
		java.lang.Integer id,
		java.lang.Integer organizationId,
		java.lang.Long number) {

		this.setId(id);
		this.setOrganizationId(organizationId);
		this.setNumber(number);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private java.lang.Integer id;

	// fields
	private java.lang.Integer organizationId;
	private java.lang.Long number;



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



	/**
	 * Return the value associated with the column: asn
	 */
	public java.lang.Long getNumber () {
		return number;
	}

	/**
	 * Set the value related to the column: asn
	 * @param number the asn value
	 */
	public void setNumber (java.lang.Long number) {
		this.number = number;
	}




	public boolean equals (Object obj) {
		if (null == obj) return false;
		if (!(obj instanceof se.sitic.megatron.entity.ASNumber)) return false;
		else {
			se.sitic.megatron.entity.ASNumber aSNumber = (se.sitic.megatron.entity.ASNumber) obj;
			if (null == this.getId() || null == aSNumber.getId()) return false;
			else return (this.getId().equals(aSNumber.getId()));
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