package se.sitic.megatron.db;

	/*   
    Read-only version of DbManager
    */


import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.MailJob;
import se.sitic.megatron.entity.Organization;


public class ReadOnlyDbManager extends  DbManager {

	
	public ReadOnlyDbManager() 
	throws DbException {

		super();
	}

	
	public void addLogEntry(LogEntry logEntry) 
	throws DbException {				
		

	}

	
	public void updateLogEntry(LogEntry logEntry) 
	throws DbException {				
		

	}
	
	
	public void addOrganization(Organization org) 
	throws DbException {

		
	}

	
	public void updateOrganization(Organization org, String modifiedBy) 
	throws DbException {

		
	}


	public void addMailJob(MailJob mailJob) 
	throws DbException {
		

	}
	

	@Deprecated
	public void updateMailJob(MailJob mailJob) 
	throws DbException {
		

	}


	public void finishMailJob(MailJob mailJob, String errorMsg) 
	throws DbException {
		

	}

	
	public long deleteMailJob(MailJob mailJob) 
	throws DbException {
        return 0;
	
	}		
	

	public void addLogJob(Job job) 
	throws DbException {	
	    
	}

	
	public void finishLogJob(Job job, String errorMsg) 
	throws DbException {
		

	}

	
	public long deleteLogJob(Job job) 
	throws DbException {
		return 0;
	
	}
	
	
}
