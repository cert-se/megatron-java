package se.sitic.megatron.entity.base;

import java.io.Serializable;


/**
 * This is an object that contains data related to the prio table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="prio"
 */

public abstract class BasePriority  implements Serializable {

	
	private static final long serialVersionUID = 1L;
	public static String REF = "Priority";
	public static String PROP_NAME = "Name";
	public static String PROP_PRIO = "Prio";
	public static String PROP_ID = "Id";


	// constructors
	public BasePriority () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BasePriority (java.lang.Integer id) {
		this.setId(id);
		initialize();
	}

	/**
	 * Constructor for required fields
	 */
	public BasePriority (
		java.lang.Integer id,
		java.lang.String name,
		java.lang.Integer prio) {

		this.setId(id);
		this.setName(name);
		this.setPrio(prio);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private java.lang.Integer id;

	// fields
	private java.lang.String name;
	private java.lang.Integer prio;



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
	 * Return the value associated with the column: prio
	 */
	public java.lang.Integer getPrio () {
		return prio;
	}

	/**
	 * Set the value related to the column: prio
	 * @param prio the prio value
	 */
	public void setPrio (java.lang.Integer prio) {
		this.prio = prio;
	}




	public boolean equals (Object obj) {
		if (null == obj) return false;
		if (!(obj instanceof se.sitic.megatron.entity.Priority)) return false;
		else {
			se.sitic.megatron.entity.Priority priority = (se.sitic.megatron.entity.Priority) obj;
			if (null == this.getId() || null == priority.getId()) return false;
			else return (this.getId().equals(priority.getId()));
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