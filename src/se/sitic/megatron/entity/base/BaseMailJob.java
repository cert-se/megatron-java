package se.sitic.megatron.entity.base;

import java.io.Serializable;


/**
 * This is an object that contains data related to the mail_job table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="mail_job"
 */

public abstract class BaseMailJob  implements Serializable {

    private static final long serialVersionUID = 1L;
    public static String REF = "MailJob";
	public static String PROP_JOB = "Job";
	public static String PROP_STARTED = "Started";
	public static String PROP_COMMENT = "Comment";
	public static String PROP_ID = "Id";
	public static String PROP_ERROR_MSG = "ErrorMsg";
	public static String PROP_FINISHED = "Finished";
	public static String PROP_USE_PRIMARY_ORG = "UsePrimaryOrg";


	// constructors
	public BaseMailJob () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BaseMailJob (java.lang.Long id) {
		this.setId(id);
		initialize();
	}

	/**
	 * Constructor for required fields
	 */
	public BaseMailJob (
		java.lang.Long id,
		boolean usePrimaryOrg,
		java.lang.Long started) {

		this.setId(id);
		this.setUsePrimaryOrg(usePrimaryOrg);
		this.setStarted(started);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private java.lang.Long id;

	// fields
	private boolean usePrimaryOrg;
	private java.lang.Long started;
	private java.lang.Long finished;
	private java.lang.String errorMsg;
	private java.lang.String comment;

	// many to one
	private se.sitic.megatron.entity.Job job;

	// collections
	private java.util.Set<se.sitic.megatron.entity.LogEntry> logEntries;



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
	 * Return the value associated with the column: use_primary_org
	 */
	public boolean isUsePrimaryOrg () {
		return usePrimaryOrg;
	}

	/**
	 * Set the value related to the column: use_primary_org
	 * @param usePrimaryOrg the use_primary_org value
	 */
	public void setUsePrimaryOrg (boolean usePrimaryOrg) {
		this.usePrimaryOrg = usePrimaryOrg;
	}



	/**
	 * Return the value associated with the column: started
	 */
	public java.lang.Long getStarted () {
		return started;
	}

	/**
	 * Set the value related to the column: started
	 * @param started the started value
	 */
	public void setStarted (java.lang.Long started) {
		this.started = started;
	}



	/**
	 * Return the value associated with the column: finished
	 */
	public java.lang.Long getFinished () {
		return finished;
	}

	/**
	 * Set the value related to the column: finished
	 * @param finished the finished value
	 */
	public void setFinished (java.lang.Long finished) {
		this.finished = finished;
	}



	/**
	 * Return the value associated with the column: error_msg
	 */
	public java.lang.String getErrorMsg () {
		return errorMsg;
	}

	/**
	 * Set the value related to the column: error_msg
	 * @param errorMsg the error_msg value
	 */
	public void setErrorMsg (java.lang.String errorMsg) {
		this.errorMsg = errorMsg;
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
	 * Return the value associated with the column: LogEntries
	 */
	public java.util.Set<se.sitic.megatron.entity.LogEntry> getLogEntries () {
		return logEntries;
	}

	/**
	 * Set the value related to the column: LogEntries
	 * @param logEntries the LogEntries value
	 */
	public void setLogEntries (java.util.Set<se.sitic.megatron.entity.LogEntry> logEntries) {
		this.logEntries = logEntries;
	}

	public void addToLogEntries (se.sitic.megatron.entity.LogEntry logEntry) {
		if (null == getLogEntries()) setLogEntries(new java.util.TreeSet<se.sitic.megatron.entity.LogEntry>());
		getLogEntries().add(logEntry);
	}




	public boolean equals (Object obj) {
		if (null == obj) return false;
		if (!(obj instanceof se.sitic.megatron.entity.MailJob)) return false;
		else {
			se.sitic.megatron.entity.MailJob mailJob = (se.sitic.megatron.entity.MailJob) obj;
			if (null == this.getId() || null == mailJob.getId()) return false;
			else return (this.getId().equals(mailJob.getId()));
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