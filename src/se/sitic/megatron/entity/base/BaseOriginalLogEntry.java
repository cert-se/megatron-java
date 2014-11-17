package se.sitic.megatron.entity.base;

import java.io.Serializable;


/**
 * This is an object that contains data related to the original_log_entry table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="original_log_entry"
 */

public abstract class BaseOriginalLogEntry  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static String REF = "OriginalLogEntry";
	public static String PROP_ENTRY = "Entry";
	public static String PROP_CREATED = "Created";
	public static String PROP_ID = "Id";
	public static String PROP_LOG_ENTRY = "LogEntry";


	// constructors
	public BaseOriginalLogEntry () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BaseOriginalLogEntry (java.lang.Long id) {
		this.setId(id);
		initialize();
	}

	/**
	 * Constructor for required fields
	 */
	public BaseOriginalLogEntry (
		java.lang.Long id,
		java.lang.Long created,
		java.lang.String entry) {

		this.setId(id);
		this.setCreated(created);
		this.setEntry(entry);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private java.lang.Long id;

	// fields
	private java.lang.Long created;
	private java.lang.String entry;

	// one to one
	private se.sitic.megatron.entity.LogEntry logEntry;



	/**
	 * Return the unique identifier of this class
     * @hibernate.id
     *  generator-class="native"
     *  column="id"
     */
	public java.lang.Long getId () {
		return id;
	}

	/**
	 * Set the unique identifier of this class
	 * @param id the new ID
	 */
	public void setId (java.lang.Long id) {
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
	 * Return the value associated with the column: entry
	 */
	public java.lang.String getEntry () {
	    // Special try/catch statement for the case when no entry exists
	    try {
	        return entry;
	    }
	    catch (org.hibernate.ObjectNotFoundException e){	        
	        return null;
	    }		
	}

	/**
	 * Set the value related to the column: entry
	 * @param entry the entry value
	 */
	public void setEntry (java.lang.String entry) {
		this.entry = entry;
	}



	/**
	 * Return the value associated with the column: LogEntry
	 */
	public se.sitic.megatron.entity.LogEntry getLogEntry () {
		return logEntry;
	}

	/**
	 * Set the value related to the column: LogEntry
	 * @param logEntry the LogEntry value
	 */
	public void setLogEntry (se.sitic.megatron.entity.LogEntry logEntry) {
		this.logEntry = logEntry;
	}




	public boolean equals (Object obj) {
		if (null == obj) return false;
		if (!(obj instanceof se.sitic.megatron.entity.OriginalLogEntry)) return false;
		else {
			se.sitic.megatron.entity.OriginalLogEntry originalLogEntry = (se.sitic.megatron.entity.OriginalLogEntry) obj;
			if (null == this.getId() || null == originalLogEntry.getId()) return false;
			else return (this.getId().equals(originalLogEntry.getId()));
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