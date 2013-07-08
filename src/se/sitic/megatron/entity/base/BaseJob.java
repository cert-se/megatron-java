package se.sitic.megatron.entity.base;

import java.io.Serializable;


/**
 * This is an object that contains data related to the job table.
 * Do not modify this class because it will be overwritten if the configuration file
 * related to this class is modified.
 *
 * @hibernate.class
 *  table="job"
 */

public abstract class BaseJob  implements Serializable {

	private static final long serialVersionUID = 1L;
	public static String REF = "Job";
	public static String PROP_FILE_SIZE = "FileSize";
	public static String PROP_FILE_HASH = "FileHash";
	public static String PROP_NAME = "Name";
	public static String PROP_JOB_TYPE = "JobType";
	public static String PROP_STARTED = "Started";
	public static String PROP_COMMENT = "Comment";
	public static String PROP_ID = "Id";
	public static String PROP_ERROR_MSG = "ErrorMsg";
	public static String PROP_FINISHED = "Finished";
	public static String PROP_FILENAME = "Filename";
	public static String PROP_PROCESSED_LINES = "ProcessedLines";

	// constructors
	public BaseJob () {
		initialize();
	}

	/**
	 * Constructor for primary key
	 */
	public BaseJob (java.lang.Long id) {
		this.setId(id);
		initialize();
	}

	/**
	 * Constructor for required fields
	 */
	public BaseJob (
		java.lang.Long id,
		java.lang.String name,
		java.lang.String filename,
		java.lang.String fileHash,
		java.lang.Long fileSize,
		java.lang.Long started) {

		this.setId(id);
		this.setName(name);
		this.setFilename(filename);
		this.setFileHash(fileHash);
		this.setFileSize(fileSize);
		this.setStarted(started);
		initialize();
	}

	protected void initialize () {}



	private int hashCode = Integer.MIN_VALUE;

	// primary key
	private java.lang.Long id;

	// fields
	private java.lang.String name;
	private java.lang.String filename;
	private java.lang.String fileHash;
	private java.lang.Long fileSize;
	private java.lang.Long started;
	private java.lang.Long finished;
	private java.lang.String errorMsg;
	private java.lang.String comment;
	private java.lang.Long processedLines;

	// many to one
	private se.sitic.megatron.entity.JobType jobType;

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
	 * Return the value associated with the column: filename
	 */
	public java.lang.String getFilename () {
		return filename;
	}

	/**
	 * Set the value related to the column: filename
	 * @param filename the filename value
	 */
	public void setFilename (java.lang.String filename) {
		this.filename = filename;
	}



	/**
	 * Return the value associated with the column: file_hash
	 */
	public java.lang.String getFileHash () {
		return fileHash;
	}

	/**
	 * Set the value related to the column: file_hash
	 * @param fileHash the file_hash value
	 */
	public void setFileHash (java.lang.String fileHash) {
		this.fileHash = fileHash;
	}



	/**
	 * Return the value associated with the column: file_size
	 */
	public java.lang.Long getFileSize () {
		return fileSize;
	}

	/**
	 * Set the value related to the column: file_size
	 * @param fileSize the file_size value
	 */
	public void setFileSize (java.lang.Long fileSize) {
		this.fileSize = fileSize;
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
	 * Return the value associated with the column: processed_lines
	 */
	public java.lang.Long getProcessedLines () {
		return processedLines;
	}

	/**
	 * Set the value related to the column: processed_lines
	 * @param processedLines the processed_lines value
	 */
	public void setProcessedLines (java.lang.Long processedLines) {
		this.processedLines = processedLines;
	}



	/**
	 * Return the value associated with the column: job_type_id
	 */
	public se.sitic.megatron.entity.JobType getJobType () {
		return jobType;
	}

	/**
	 * Set the value related to the column: job_type_id
	 * @param jobType the job_type_id value
	 */
	public void setJobType (se.sitic.megatron.entity.JobType jobType) {
		this.jobType = jobType;
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
		if (!(obj instanceof se.sitic.megatron.entity.Job)) return false;
		else {
			se.sitic.megatron.entity.Job job = (se.sitic.megatron.entity.Job) obj;
			if (null == this.getId() || null == job.getId()) return false;
			else return (this.getId().equals(job.getId()));
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