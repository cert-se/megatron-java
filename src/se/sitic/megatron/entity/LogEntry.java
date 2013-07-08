package se.sitic.megatron.entity;

import se.sitic.megatron.entity.base.BaseLogEntry;



public class LogEntry extends BaseLogEntry {
	private static final long serialVersionUID = 1L;
	
	
/*[CONSTRUCTOR MARKER BEGIN]*/
	public LogEntry () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public LogEntry (java.lang.Long id) {
		super(id);
	}

	/**
	 * Constructor for required fields
	 */
	public LogEntry (
		java.lang.Long id,
		java.lang.Long created,
		java.lang.Long logTimestamp) {

		super (
			id,
			created,
			logTimestamp);
	}

/*[CONSTRUCTOR MARKER END]*/
	
	public int compareTo(Object obj) {

        int result = 0;
        
        if (this.getId() == 0 || ((LogEntry)obj).getId() == 0) {
            // Assert that only objects that have been persited are used. 
            throw new java.lang.AssertionError("LogEntry ID is undefined (0)");
        }
        		
		if (this.getId() == ((LogEntry)obj).getId()) {		
			result = 0;
		}
		else if (this.getId() < ((LogEntry)obj).getId()) {
			result = -1;
		}
		else {
			return 1;
		}
		return result;
	}
	
}
