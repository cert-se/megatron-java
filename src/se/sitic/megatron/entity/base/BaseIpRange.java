package se.sitic.megatron.entity.base;

import java.io.Serializable;


/**
 * This is an object that contains data related to the ip_range table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="ip_range"
 */

public abstract class BaseIpRange  implements Serializable, Comparable<Object> {

	private static final long serialVersionUID = 1L;
	public static String REF = "IpRange";
	public static String PROP_END_ADDRESS = "EndAddress";
	public static String PROP_START_ADDRESS = "StartAddress";
	public static String PROP_ID = "Id";
	public static String PROP_NET_NAME = "NetName";
	public static String PROP_ORGANIZATION_ID = "OrganizationId";


	// constructors
	public BaseIpRange () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BaseIpRange (java.lang.Integer id) {
		this.setId(id);
		initialize();
	}

	/**
	 * Constructor for required fields
	 */
	public BaseIpRange (
		java.lang.Integer id,
		java.lang.Integer organizationId,
		java.lang.Long startAddress,
		java.lang.Long endAddress) {

		this.setId(id);
		this.setOrganizationId(organizationId);
		this.setStartAddress(startAddress);
		this.setEndAddress(endAddress);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private java.lang.Integer id;

	// fields
	private java.lang.Integer organizationId;
	private java.lang.Long startAddress;
	private java.lang.Long endAddress;
	private java.lang.String netName;



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
	 * Return the value associated with the column: start_address
	 */
	public java.lang.Long getStartAddress () {
		return startAddress;
	}

	/**
	 * Set the value related to the column: start_address
	 * @param startAddress the start_address value
	 */
	public void setStartAddress (java.lang.Long startAddress) {
		this.startAddress = startAddress;
	}



	/**
	 * Return the value associated with the column: end_address
	 */
	public java.lang.Long getEndAddress () {
		return endAddress;
	}

	/**
	 * Set the value related to the column: end_address
	 * @param endAddress the end_address value
	 */
	public void setEndAddress (java.lang.Long endAddress) {
		this.endAddress = endAddress;
	}



	/**
	 * Return the value associated with the column: net_name
	 */
	public java.lang.String getNetName () {
		return netName;
	}

	/**
	 * Set the value related to the column: net_name
	 * @param netName the net_name value
	 */
	public void setNetName (java.lang.String netName) {
		this.netName = netName;
	}




	public boolean equals (Object obj) {
		if (null == obj) return false;
		if (!(obj instanceof se.sitic.megatron.entity.IpRange)) return false;
		else {
			se.sitic.megatron.entity.IpRange ipRange = (se.sitic.megatron.entity.IpRange) obj;
			if (null == this.getId() || null == ipRange.getId()) return false;
			else return (this.getId().equals(ipRange.getId()));
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