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

    
    @Override
    public void addLogEntry(LogEntry logEntry) 
    throws DbException {                
        

    }

    
    @Override
    public void updateLogEntry(LogEntry logEntry) 
    throws DbException {                
        

    }
    
    
    @Override
    public void addOrganization(Organization org, String modifiedBy) 
    throws DbException {

        
    }

    
    @Override
    public void updateOrganization(Organization org, String modifiedBy) 
    throws DbException {

        
    }


    @Override
    public void addMailJob(MailJob mailJob) 
    throws DbException {
        

    }
    

    @Override
    @Deprecated
    public void updateMailJob(MailJob mailJob) 
    throws DbException {
        

    }


    @Override
    public void finishMailJob(MailJob mailJob, String errorMsg) 
    throws DbException {
        

    }

    
    @Override
    public long deleteMailJob(MailJob mailJob) 
    throws DbException {
        return 0;
    
    }        
    

    @Override
    public void addLogJob(Job job) 
    throws DbException {    
        
    }

    
    @Override
    public void finishLogJob(Job job, String errorMsg) 
    throws DbException {
        

    }

    
    @Override
    public long deleteLogJob(Job job) 
    throws DbException {
        return 0;
    
    }
    
    
}
