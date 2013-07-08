package se.sitic.megatron.entity.base;

import java.io.Serializable;


/**
 * This is an object that contains data related to the job_type table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="job_type"
 */

public abstract class BaseJobType  implements Serializable {


	private static final long serialVersionUID = 1L;
	public static String REF = "JobType";
	public static String PROP_NAME = "Name";
	public static String PROP_ENTRY_TYPE = "EntryType";
	public static String PROP_ENABLED = "Enabled";
	public static String PROP_SOURCE_DESCRIPTION = "SourceDescription";
	public static String PROP_COMMENT = "Comment";
	public static String PROP_ID = "Id";


	// constructors
	public BaseJobType () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BaseJobType (java.lang.Integer id) {
		this.setId(id);
		initialize();
	}

	/**
	 * Constructor for required fields
	 */
	public BaseJobType (
		java.lang.Integer id,
		java.lang.String name,
		boolean enabled) {

		this.setId(id);
		this.setName(name);
		this.setEnabled(enabled);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private java.lang.Integer id;

	// fields
	private java.lang.String name;
	private boolean enabled;
	private java.lang.String sourceDescription;
	private java.lang.String comment;

	// many to one
	private se.sitic.megatron.entity.EntryType entryType;



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
	 * Return the value associated with the column: source_description
	 */
	public java.lang.String getSourceDescription () {
		return sourceDescription;
	}

	/**
	 * Set the value related to the column: source_description
	 * @param sourceDescription the source_description value
	 */
	public void setSourceDescription (java.lang.String sourceDescription) {
		this.sourceDescription = sourceDescription;
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
	 * Return the value associated with the column: entry_type_id
	 */
	public se.sitic.megatron.entity.EntryType getEntryType () {
		return entryType;
	}

	/**
	 * Set the value related to the column: entry_type_id
	 * @param entryType the entry_type_id value
	 */
	public void setEntryType (se.sitic.megatron.entity.EntryType entryType) {
		this.entryType = entryType;
	}




	public boolean equals (Object obj) {
		if (null == obj) return false;
		if (!(obj instanceof se.sitic.megatron.entity.JobType)) return false;
		else {
			se.sitic.megatron.entity.JobType jobType = (se.sitic.megatron.entity.JobType) obj;
			if (null == this.getId() || null == jobType.getId()) return false;
			else return (this.getId().equals(jobType.getId()));
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