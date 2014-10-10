package se.sitic.megatron.db;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.ASNumber;
import se.sitic.megatron.entity.Contact;
import se.sitic.megatron.entity.DomainName;
import se.sitic.megatron.entity.EntryType;
import se.sitic.megatron.entity.IpRange;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.JobType;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.MailJob;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.entity.Priority;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;

public class DbManager {

    protected Session session = null;    
    protected Logger log = null;    
    protected DbManager()

            throws DbException { 

        this.log = Logger.getLogger(this.getClass());

        try {
            // This step will read hibernate.cfg.xml             
            //    and prepare hibernate for use
            SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();            
            session = sessionFactory.openSession();
        }
        catch (HibernateException he) {                    
            he.printStackTrace();
            throw new DbException("Failed to initialize DbManager", he);
        }              
    }


    public static DbManager createDbManager(TypedProperties props) 
            throws DbException { 

        DbManager dbm = null;

        if (props.isNoDb() || props.isMailDryRun() ||  props.isMailDryRun2()) {
            dbm = new ReadOnlyDbManager();            
        }    
        else {
            dbm = new DbManager();
        }
        return dbm;
    }


    protected Object loadLongObject (Class<?> clazz,  long objId) 
            throws DbException { 

        Object obj = session.get(clazz, objId);

        if (obj == null) {
            String msg = ("Invalid object reference: " + objId + ".");
            log.error(msg);
            throw new DbException(msg);
        }
        return obj;
    }

    protected Object loadIntegerObject (Class<?> clazz,  int objId) 
            throws DbException { 

        Object obj = session.get(clazz, objId);

        if (obj == null) {
            String msg = ("Invalid object reference: " + objId + ".");
            log.error(msg);
            throw new DbException(msg);
        }
        return obj;
    }


    public Object genericLoadObject(String className, String[] searchFields, Object[] values)
            throws DbException { 

        if (searchFields.length != values.length) {
            throw new DbException("Number of search fields and number of values are not maching");
        }
        if (searchFields.length < 1) {
            throw new DbException("Search fields are empty");
        }

        Object obj = null;

        String hql = "from " + className + " where ";                

        try {

            for (int i=0; i<searchFields.length; i++) {            
                hql = hql + ( i == 0 ? searchFields[i] + "= ?" :  " AND " +  searchFields[i]  + "= ?");
            }
            Query query = session.createQuery(hql);

            for (int i=0; i<searchFields.length; i++) {

                String typeName = values[i].getClass().getSimpleName();

                Method method = null;
                if (typeName.equals("Integer")) {
                    query.setInteger(i, ((Integer)values[i]).intValue());
                }
                else if (typeName.equals("Long")) {
                    query.setLong(i, ((Long)values[i]).longValue());
                }
                else {
                    method = query.getClass().getMethod("set" + typeName, Integer.TYPE, values[i].getClass());
                    Object[] args = {i, values[i]};
                    method.invoke(query, args);
                }                
            }                       
            obj = query.uniqueResult();

        } catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in genericLoadObject", e);
        }

        return obj;
    }



    public Object genericLoadObject(String className, String searchField, Object value)
            throws DbException { 

        Object obj = null;

        Query query = session.createQuery("from " + className + " where " + searchField + "= ?");

        String typeName = value.getClass().getSimpleName();

        try {

            Method method = null;
            if (typeName.equals("Integer")) {
                query.setInteger(0, ((Integer)value).intValue());
            }
            else if (typeName.equals("Long")) {
                query.setLong(0, ((Long)value).longValue());
            } 
            else {
                method = query.getClass().getMethod("set" + typeName, Integer.TYPE, value.getClass());
                Object[] args = {0, value};
                method.invoke(query, args);
            }            

            obj = query.uniqueResult();

        } catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in genericLoadObject", e);
        }

        return obj;

    }

    public void addPriority(Priority prio) 
            throws DbException { 

        try {                
            session.saveOrUpdate(prio);
            session.flush();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in addPriority", e);
        }
    }


    public void addEntryType(EntryType entryType) 
            throws DbException { 
        try {                    
            session.saveOrUpdate(entryType);
            session.flush();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in addEntryType", e);
        }
    }


    public void addLogEntry(LogEntry logEntry) 
            throws DbException { 
        try {                
            session.save(logEntry);
            session.flush();
            session.evict(logEntry);
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in addLogEntry", e);
        }
    }


    public void updateLogEntry(LogEntry logEntry) 
            throws DbException { 
        try {                
            session.update(logEntry);
            session.flush();
            session.evict(logEntry);
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in updateLogEntry", e);
        }
    }


    @SuppressWarnings("unchecked")
    public List<LogEntry> listLogEntries(long logJobId, int startIndex,
            int noOfRecords) 
                    throws DbException { 
        try {
            Query query = session.createQuery("from LogEntry where Job.Id = ? order by Id asc");
            query.setLong(0, logJobId);
            query.setMaxResults(noOfRecords);
            query.setFirstResult(startIndex);
            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in listLogEntries", e);
        }
    }


    @SuppressWarnings("unchecked")
    public List<LogEntry> listLogEntries(long logJobId, boolean usePrimaryOrg, int startIndex,
            int noOfRecords) 
                    throws DbException { 

        try {
            Query query = null;

            if (usePrimaryOrg) {
                query = session.createQuery("from LogEntry where Job.Id = ? order by Organization, Id asc");
            }
            else {
                query = session.createQuery("from LogEntry where Job.Id = ? order by Organization2, Id asc");
            }
            query.setLong(0, logJobId);
            query.setMaxResults(noOfRecords);
            query.setFirstResult(startIndex);

            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in listLogEntries", e);
        }
    }


    /**
     * Returns high priority entries for specified job. Both primary and 
     * secondary organization is checked (org_id and org_id2).
     * 
     * Note, this method contains MySQL specific query parameters.
     * Must be checked if another database is used.
     * 
     * @param prio log entries linked to an organization with a priority equals
     *     or above this value will be included in the result. 
     * 
     * @return High priority entries, sorted by prio.
     */

    @SuppressWarnings({ "deprecation" })
    public List<LogEntry> listLogEntries(long logJobId, int prio, int startIndex, int noOfRecords) throws DbException {

        try {            
            ArrayList<LogEntry> entries = new ArrayList<LogEntry>();

            String sql = "select distinct l.id from log_entry l, organization o, prio p where l.job_id = ? and (l.org_id = o.id or l.org_id2 = o.id) and o.prio_id = p.id and p.prio >= ? order by prio desc limit ?, ?;";    
            PreparedStatement ps = session.connection().prepareStatement(sql);
            ps.setLong(1, logJobId);
            ps.setInt(2, prio);
            ps.setInt(3, startIndex);
            ps.setInt(4, startIndex + noOfRecords);

            boolean result = ps.execute();
            if (result) {
                while (ps.getResultSet().next()) {                        
                    long id = ps.getResultSet().getLong("id");
                    LogEntry entry = (LogEntry)this.loadLongObject(LogEntry.class, id);
                    entries.add(entry);                        
                }                             
            }
            ps.close();     

            return entries;
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in listLogEntries", e);
        }                               
    }


    /**
     * Returns the log entry with the latest "created" timestamp, where 
     * ipAddress is equal to specified IP. Null is returned if not found.
     * Only ipAddress is checked, not ipAddress2 or ipRange.
     * 
     * @param ip ip-address for the log entry.
     * @param jobId log entries for this job will be ignored. 
     */
    @SuppressWarnings("deprecation")
    public LogEntry getLastSeenLogEntry(long ip, long jobId) throws DbException {

        LogEntry entry = null;

        try {
            String sql = "select id from log_entry where ip_address = ? and job_id != ? order by created desc";    
            PreparedStatement ps = session.connection().prepareStatement(sql);
            ps.setLong(1, ip);            
            ps.setLong(2, jobId);
            boolean result = ps.execute();
            if (result) {
                if (ps.getResultSet().first()) {
                    long id = ps.getResultSet().getLong("id");
                    entry = (LogEntry)this.loadLongObject(LogEntry.class, id);
                }                
            }
            ps.close();     
        }        
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getLastSeenLogEntry", e);
        }
        return entry;
    }


    /**
     * Returns the mail job with the latest "started" timestamp, which have
     * mailed specified IP. Null is returned if not found.
     * Only ipAddress is checked, not ipAddress2 or ipRange.
     */
    @SuppressWarnings("deprecation")
    public MailJob getLastSeenMailJob(long ip) throws DbException {

        MailJob mailJob = null;

        try {
            String sql = "select mj.id from mail_job mj, mail_job_log_entry_mapping mjlem, log_entry le " + 
                    "where mjlem.log_entry_id = le.id and le.ip_address = ? and mjlem.mail_job_id = mj.id order by mj.started desc";

            PreparedStatement ps = session.connection().prepareStatement(sql);
            ps.setLong(1, ip);                   
            boolean result = ps.execute();
            if (result) {
                if (ps.getResultSet().first()) {
                    long id = ps.getResultSet().getLong("id");
                    mailJob = (MailJob)this.loadLongObject(MailJob.class, id);
                }                
            }
            ps.close();     
        }       
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getLastSeenMailJob", e);
        }
        return mailJob;
    }


    public void addOrganization(Organization org, String modifiedBy) 
            throws DbException { 
        try {
            java.util.Date date = new java.util.Date();
            long time = SqlUtil.convertTimestamp(date);        
            org.setLastModified(time);
            org.setModifiedBy(modifiedBy);
            if (org.getCreated() == null) {
                org.setCreated(time);            
            }      
            session.save(org);
            session.flush();
        } 

        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " not possible to add organiation", e);
        }
    }



    public void updateOrganization(Organization org, String modifiedBy) 
            throws DbException { 
        try {
            java.util.Date date = new java.util.Date();
            long time = SqlUtil.convertTimestamp(date);        
            org.setLastModified(time);
            org.setModifiedBy(modifiedBy);
            session.update(org);
            session.flush();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in updateOrganization", e);
        }
    }


    @SuppressWarnings("unchecked")
    public List<Organization> searchOrganizations(String name, int startIndex,
            int noOfRecords) 
                    throws DbException { 
        try {
            Query query = session.createQuery("from Organization where name like ?");
            query.setString(0, "%" + name +"%");

            query.setMaxResults(noOfRecords);
            query.setFirstResult(startIndex);

            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchOrganizations", e);
        }
    }


    public Organization searchOrganization(String name) 
            throws DbException { 
        try {
            Query query = session.createQuery("from Organization where name = ?");
            query.setString(0, name);

            Organization org = (Organization)query.uniqueResult();

            return org;
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchOrganization", e);
        }

    }


    @SuppressWarnings("unchecked")
    public List<Organization> listOrganizations(int startIndex, int noOfRecords) 
            throws DbException { 

        try {
            Query query = session.createQuery("from Organization order by Name asc");        
            query.setMaxResults(noOfRecords);
            query.setFirstResult(startIndex);                

            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in listOrganizations", e);
        }

    }


    public Organization getOrganization(int orgId) 
            throws DbException { 

        try {
            Organization org = (Organization)loadIntegerObject(Organization.class, orgId);

            return org;
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getOrganization", e);
        }

    }


    public void addMailJob(MailJob mailJob) 
            throws DbException { 

        try {
            mailJob.setStarted(SqlUtil.convertTimestamp(new Date()));
            session.saveOrUpdate(mailJob);
            session.flush();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in addMailJob", e);
        }
    }


    /*        
        The updateMailJob is deprecated due to a problem when persisting the MailJob object after
        a change in the LogEntry collection. The reason could be due to some missconfiguration in the
        Hibernate object mapping for MailJob or a bug in Hibernate. 
        Use finishMailJob to persist the MailJob once it has been completed.      
     */
    @Deprecated
    public void updateMailJob(MailJob mailJob) 
            throws DbException { 

        try {
            session.update(mailJob);        
            session.flush();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in updateMailJob", e);
        }
    }


    public void finishMailJob(MailJob mailJob, String errorMsg) 
            throws DbException { 

        try {
            mailJob.setErrorMsg(errorMsg);
            mailJob.setFinished(SqlUtil.convertTimestamp(new Date()));
            session.update(mailJob);            
            session.flush();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in finishMailJob", e);
        }
    }


    public long deleteMailJob(MailJob mailJob) 
            throws DbException { 

        try {
            Query query = session.createSQLQuery(
                    "SELECT COUNT(*) FROM mail_job_log_entry_mapping WHERE mail_job_id = ?");
            query.setLong(0, mailJob.getId());
            long noLogEntriesInJob = ((BigInteger)query.list().get(0)).longValue();

            session.delete(mailJob);
            session.flush();

            long entriesLeft = ((BigInteger)query.list().get(0)).longValue();

            return noLogEntriesInJob - entriesLeft;
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in deleteMailJob", e);
        }
    }


    // Search MailJob by Job entity
    @SuppressWarnings("unchecked")
    public List<MailJob> searchMailJobs(Date startTime,
            Date endTime, int startIndex, int noOfRecords) 
                    throws DbException { 
        try {
            Query query = session.createQuery("from MailJob where started >= ? and started <= ? order by Id asc");
            query.setLong(0, SqlUtil.convertTimestamp(startTime));
            query.setLong(1, SqlUtil.convertTimestamp(endTime));
            query.setMaxResults(noOfRecords);
            query.setFirstResult(startIndex);

            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchMailJobs", e);
        }
    }


    // Search MailJob by Job entity
    @SuppressWarnings("unchecked")
    public List<MailJob> searchMailJobs(Job job) 
            throws DbException { 

        try {
            Query query = session.createQuery("from MailJob where Job = ?");
            query.setEntity(0, job);

            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchMailJobs", e);
        }
    }


    public void addJobType(JobType jobType) 
            throws DbException { 

        try {                
            session.saveOrUpdate(jobType);
            session.flush();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in addJobType", e);
        }
    }

    public void addLogJob(Job job) 
            throws DbException {

        try {
            session.save(job);
            session.flush();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in addLogJob", e);
        }
    }

    public boolean existsFileHash(String hash)
            throws DbException {

        try {
            Query query = session.createQuery("from Job where FileHash = ?");
            query.setString(0, hash);

            return query.list().size() > 0;
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in existsFileHash", e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Job> searchLogJobs(Date startTime, Date endTime, int startIndex,
            int noOfRecords) 
                    throws DbException { 

        try {

            Query query = session.createQuery("from Job where started >= ? and started <= ? order by Id asc");
            query.setLong(0, SqlUtil.convertTimestamp(startTime));
            query.setLong(1, SqlUtil.convertTimestamp(endTime));
            query.setMaxResults(noOfRecords);
            query.setFirstResult(startIndex);

            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchLogJobs", e);
        }

    }                           


    public Job searchLogJob(String name)
            throws DbException { 

        try {
            Query query = session.createQuery("from Job where name = ?");
            query.setString(0, name);

            return (Job)query.uniqueResult();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchLogJob", e);
        }

    } 


    public void finishLogJob(Job job, String errorMsg) 
            throws DbException { 

        try {
            job.setFinished(SqlUtil.convertTimestamp(new Date()));
            job.setErrorMsg(errorMsg);
            session.update(job);
            session.flush();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in finishLogJob", e);
        }

    }


    @SuppressWarnings("deprecation")
    public long deleteLogJob(Job job) 
            throws DbException {

        long jobId = job.getId();        


        // Use JDBC connection to delete additional_item, free_text & original_log_entry
        try {

            // additional_item
            String sqlDelete = "delete from additional_item where log_entry_id in (select id from log_entry where job_id = ?)";       
            PreparedStatement ps = session.connection().prepareStatement(sqlDelete);
            ps.setLong(1, jobId);
            ps.execute();
            ps.close();        

            // free_text
            sqlDelete = "delete from free_text where log_entry_id in (select id from log_entry where job_id = ?)";
            ps = session.connection().prepareStatement(sqlDelete);
            ps.setLong(1, jobId);
            ps.execute();
            ps.close();

            // original_log_entry
            sqlDelete = "delete from original_log_entry where id in (select id from log_entry where job_id = ?)";
            ps = session.connection().prepareStatement(sqlDelete);
            ps.setLong(1, jobId);
            ps.execute();
            ps.close();

        }
        catch(SQLException sqle) {
            throw handleException("SQL exception in deleteLogJob", sqle);
        }


        try {
            // Delete LogEntry using hql
            String hqlDelete = "delete LogEntry where Job.Id = ?";
            long  numberOfDeletedLogEntries = session.createQuery( hqlDelete ).setLong(0, job.getId()).executeUpdate();

            session.evict(job);
            Job realJob = (Job)loadLongObject(job.getClass(), jobId);

            // Delete Job
            session.delete(realJob);        
            session.flush();


            return numberOfDeletedLogEntries;
        }

        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in deleteLogJob", e);
        }

    }


    public JobType searchJobType(String name) 
            throws DbException { 

        try {
            Query query = session.createQuery("from JobType where name = ?");
            query.setString(0, name);

            return (JobType)query.uniqueResult();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchJobType", e);
        }

    }


    public EntryType searchEntryType(String name) 
            throws DbException { 

        try {

            Query query = session.createQuery("from EntryType where name = ?");
            query.setString(0, name);

            return (EntryType)query.uniqueResult();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchEntryType", e);
        }

    }


    public Priority getPriority(int prio)
            throws DbException { 
        try {
            Query query = session.createQuery("from Priority where prio = ?");
            query.setInteger(0, prio);

            return (Priority)query.uniqueResult();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getPriority", e);
        }

    }


    @SuppressWarnings("unchecked")
    public List<Priority> getAllPriorities()
            throws DbException { 

        try {
            Query query = session.createQuery("from Priority order by Prio desc");


            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getAllPriorities", e);
        }

    }


    @SuppressWarnings("unchecked")
    public List<IpRange> getAllIpRanges(boolean includeDisabledOrgs)
            throws DbException { 

        try {

            Query query = null;
            if (includeDisabledOrgs) {
                query = session.createQuery("from IpRange");
            }
            else {
                query = session.createQuery("from IpRange where OrganizationId in (from Organization where Enabled = ?)");
                query.setBoolean(0, true);
            }

            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getAllIpRanges", e);
        }

    }


    @SuppressWarnings("unchecked")
    public List<ASNumber> getAllASNumbers(boolean includeDisabledOrgs)
            throws DbException { 

        try {

            Query query = null;
            if (includeDisabledOrgs) {
                query = session.createQuery("from ASNumber");
            }
            else {
                query = session.createQuery("from ASNumber where OrganizationId in (from Organization where Enabled = ?)");
                query.setBoolean(0, true);
            }

            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getAllASNumbers", e);
        }

    }


    @SuppressWarnings("unchecked")
    public List<DomainName> getAllDomainNames(boolean includeDisabledOrgs)
            throws DbException { 

        try {

            Query query = null;
            if (includeDisabledOrgs) {
                query = session.createQuery("from DomainName");
            }
            else {
                query = session.createQuery("from DomainName where OrganizationId in (from Organization where Enabled = ?)");
                query.setBoolean(0, true);
            }
            return query.list();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getAllDomainNames", e);
        }

    }

    // Gets the organization with the given domain name
    public Organization getOrgByDomainName(String domainName)
            throws DbException { 

        try {

            Organization org = null;
            // Ugly to step search...
            Query query = session.createQuery("from DomainName where Name = ?");
            query.setString(0, domainName);

            DomainName dName = (DomainName) query.uniqueResult();

            if (dName != null) {
                org = (Organization)loadIntegerObject(Organization.class, dName.getOrganizationId());               
            }
            return org;

        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchOrgByDomainName", e);
        }

    }

    // Searches for organizations with domain names like the input name
    public List<Organization> searchOrgForDomainName(String domainName)
            throws DbException { 

        try {                       
            //Query query = session.createQuery("OrganizationId from DomainName where Name like '%?%'");
            //query.setString(0, domainName);

            String sql = "SELECT DISTINCT org_id FROM domain_name WHERE domain_name LIKE ? ORDER BY org_id ASC";            
            Query query = session.createSQLQuery(sql);
            query.setString(0, "%" + domainName + "%");
            List<Object> orgIDs = query.list();
            List<Organization> orgs = new ArrayList<Organization>();    

            Iterator<Object> itr = orgIDs.iterator();           
            while (itr.hasNext()) { 
                Organization org = getOrganization(((Integer)itr.next()).intValue());
                if (org != null) {
                    orgs.add(org);
                }               
            }           
            return orgs;            
            
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchOrgForDomainName", e);
        }        
    }

    public Organization searchOrganizationByASNumber(int asn)
            throws DbException { 

        try {

            Organization org = null;
            // Ugly two step search...
            Query query = session.createQuery("from ASNumber where Number = ?");
            query.setInteger(0, asn);

            ASNumber asnum = (ASNumber) query.uniqueResult();

            if (asnum != null) {
                org = (Organization)loadIntegerObject(Organization.class, asnum.getOrganizationId());               
            }
            return org;

        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchOrgByASNumber", e);
        }

    }


    @SuppressWarnings("unchecked")
    public List<Organization> searchOrganizationsByIPrange(long start, long end) 
            throws DbException { 
        try {
            String sql = "SELECT DISTINCT org_id FROM ip_range WHERE (end_address >= ? AND end_address <= ?) OR (start_address <=  ? AND end_address >= ?) OR  (start_address >= ? AND start_address <= ?) OR (start_address >= ? AND end_address <= ?) ORDER BY org_id ASC";

            Query query = session.createSQLQuery(sql);
            query.setLong(0, start);   
            query.setLong(1, end);
            query.setLong(2, start);
            query.setLong(3, end);
            query.setLong(4, start);
            query.setLong(5, end);
            query.setLong(6, start);
            query.setLong(7, end);


            List<Object> orgIDs = query.list();
            List<Organization> orgs = new ArrayList<Organization>();    

            Iterator<Object> itr = orgIDs.iterator();           
            while (itr.hasNext()) { 
                Organization org = getOrganization(((Integer)itr.next()).intValue());
                if (org != null) {
                    orgs.add(org);
                }               
            }           
            return orgs;
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchOrgsByIPrange", e);
        }           
    }   


    @SuppressWarnings("unchecked")
    public List<LogEntry> searchLogEntriesByIPrange(long start, long end) 
            throws DbException { 
        try {
            //String sql = "SELECT id FROM log_entry where (ip_address >= ? AND ip_address <= ?) OR (ip_address2 >= ? AND ip_address2 <= ?)";
            Query query = session.createQuery("from LogEntry where (IpAddress >= ? AND IpAddress <= ?) OR (IpAddress2 >= ? AND IpAddress2 <= ?)");

            query.setLong(0, start);   
            query.setLong(1, end);
            query.setLong(2, start);
            query.setLong(3, end);


            /*List<Object> entryIDs = query.list();
                List<LogEntry> entries = new ArrayList<LogEntry>(); 

                Iterator<Object> itr = entryIDs.iterator();         
                while (itr.hasNext()) {                                 
                    LogEntry entry = (LogEntry)loadLongObject(LogEntry.class, ((Long)itr.next()).longValue());                                  

                    if (entry != null) {
                        entries.add(entry);
                    }               
                }           */
            return query.list();
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchLogEntriesByIPrange", e);
        }           
    }   

    @SuppressWarnings("unchecked")
    public List<LogEntry> searchLogEntriesByOrgId(int orgId)            
            throws DbException { 
        try {
            Query query = session.createQuery("from LogEntry where Organization = ?");
            query.setInteger(0, orgId);

            return query.list();
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchLogEntriesByOrgId", e);
        }       
    }

    @SuppressWarnings("unchecked")
    public List<LogEntry> searchLogEntriesByASNumber(long asn) 
            throws DbException { 
        try {

            Query query = session.createQuery("from LogEntry where Asn = ? or Asn2 = ?");           
            query.setLong(0, asn);   
            query.setLong(1, asn);

            return query.list();
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchLogEntriesByASNumber", e);
        }           
    }   


    @SuppressWarnings("unchecked")
    public List<Organization> searchOrganizationByEmailAddress(String address) 
            throws DbException {

        List<Organization> orgs = new ArrayList<Organization>();

        try {
            Query query = session.createQuery("from Contact where EmailAddress like ?");
            query.setString(0, "%" + address +"%");
            List<Contact> contacts = query.list();
            for (Contact contact: contacts) {   
                Organization org = (Organization)loadIntegerObject(Organization.class, contact.getOrganizationId());                
                if (orgs.contains(org) == false) {              
                    orgs.add(org);
                }
            }       
            return orgs;
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in searchOrganizationByEmailAddress", e);
        }

    }


    public long getNumberOfLogEntries()
            throws DbException { 
        try {

            Query query = session.createSQLQuery(
                    "SELECT COUNT(*) FROM log_entry");

            return ((BigInteger)query.list().get(0)).longValue();
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getNumberOfLogEntries", e);
        }

    }


    public boolean existsMailForIp(long ip, Organization organization, long periodInSecs, boolean usePrimaryOrg)
            throws DbException { 

        try {        
            String orgIdColName = "org_id";
            String ipAddrColName = "ip_address";
            if (usePrimaryOrg != true) {
                orgIdColName = "org_id2";
                ipAddrColName = "ip_address2";
            }

            String select = "SELECT COUNT(*) FROM log_entry le, mail_job_log_entry_mapping map, mail_job mj where mj.started >= ? AND mj.finished <= ? AND mj.id = map.mail_job_id AND map.log_entry_id = le.id AND le." + orgIdColName + " = ? AND le." + ipAddrColName + " = ?";                  
            Query query = session.createSQLQuery(select); 

            long endTime = SqlUtil.convertTimestamp(new Date());        
            long startTime = endTime - periodInSecs;

            query.setLong(0, startTime);
            query.setLong(1, endTime);    
            query.setInteger(2, organization.getId());
            query.setLong(3, ip);

            return ((BigInteger)query.list().get(0)).intValue() > 0 ? true:false;
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in existsMailForIp", e);
        }

    }


    public DbStatisticsData fetchStatistics(Date startDate, Date endDate)
            throws DbException { 
        try {

            DbStatisticsData dbStats = new DbStatisticsData(startDate, endDate);

            dbStats.setNoOfSuccessfulJobs(getNumberOfSuccessfulLogJobs(startDate, endDate));
            dbStats.setNoOfFailedJobs(getNumberOfFailedLogJobs(startDate, endDate));
            dbStats.setNoOfProcessedLines(getNumberOfProcessedLines(startDate, endDate));
            dbStats.setNoOfLogEntries(getNumberOfLogEntries(startDate, endDate));
            dbStats.setNoOfMatchedLogEntries(getNumberOfMatchedLogEntries(startDate, endDate));
            dbStats.setNoOfSentEmails(getNumberOfSentEmails(startDate, endDate));        
            dbStats.setNoOfNewOrganizations(getNumberOfNewOrganizations(startDate, endDate));
            dbStats.setNoOfModifiedOrganizations(getNumberOfModifiedOrganizations(startDate, endDate));

            return dbStats;

        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in fetchStatistics", e);
        }

    }

    /**
     * Fetches number of processed lines for successful jobs per job type.
     * 
     * @return Map with result, where key is job type and value is no. 
     *     of processed lines.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Long> fetchNoOfProcessedLinesPerEntryType(Date startDate, Date endDate) 
            throws DbException { 
        try {

            Map<String, Long> result = new HashMap<String, Long>();

            String select = "SELECT et.name, sum(j.processed_lines) FROM job j, job_type jt, entry_type et WHERE job_type_id = jt.id AND jt.entry_type_id = et.id AND finished >= ? AND finished <= ? group by et.name;";

            Query query = session.createSQLQuery(select);        
            query.setLong(0, SqlUtil.convertTimestamp(startDate));
            query.setLong(1, SqlUtil.convertTimestamp(endDate));

            List<Object[]> resultSet = query.list();

            if (resultSet != null) {
                for (Object[] obj: resultSet) {                
                    result.put(new String(obj[0].toString()), new Long(((java.math.BigDecimal)obj[1]).longValue()));                
                }
            }    

            return result;
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in fetchNoOfProcessedLinesPerJobType", e);
        }
    }


    /**
     * Fetches number of log entries for successful jobs per job type.
     * 
     * @param onlyMatchedEntries If true, count only log entries where organization and 
     *     organization2 is not null. If false, count all log entries. 
     * 
     * @return Map with result, where key is job type and value is no. 
     *     of processed lines.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Long> fetchNoOfLogEntriesPerEntryType(Date startDate, Date endDate, boolean onlyMatchedEntries) 
            throws DbException { 

        try {

            Map<String, Long> result = new HashMap<String, Long>();

            String select = "SELECT et.name, count(le.id) FROM log_entry le, job j, job_type jt, entry_type et WHERE le.job_id = j.id AND j.job_type_id = jt.id AND jt.entry_type_id = et.id AND j.finished >= ? AND j.finished <= ?";

            if (onlyMatchedEntries) {
                select = select + " AND (le.org_id IS NOT null OR le.org_id2 IS NOT null)";
            }
            select = select + " GROUP BY jt.entry_type_id";

            Query query = session.createSQLQuery(select);        
            query.setLong(0, SqlUtil.convertTimestamp(startDate));
            query.setLong(1, SqlUtil.convertTimestamp(endDate));

            List<Object[]> resultSet = query.list();

            if (resultSet != null) {
                for (Object[] obj: resultSet) {                
                    result.put(new String(obj[0].toString()), new Long(((BigInteger)obj[1]).longValue()));                
                }
            }    

            return result;
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in fetchNoOfLogEntriesPerJobType", e);
        }

    }


    /**
     * Fetches log entries for use in GeolocationXmlReportGenerator. Hibernate 
     * is not used due to memory constraints and complexity of the query.  
     * 
     * @param jobTypeKillList list if job types to exclude (the db-field "job_type.name").
     */
    @SuppressWarnings("deprecation")
    public List<LogEntry> fetchLogEntriesForGeolocation(Date startDate, Date endDate, List<String> jobTypeKillList) throws DbException {
        List<LogEntry> result = new ArrayList<LogEntry>(2048);
        PreparedStatement ps = null;
        try {
            // -- build query
            String queryStr = 
                    "select log_entry.id, log_timestamp, log_entry.created, ip_address, ip_range_start, port, hostname, asn, prio.name, additional_item.name, additional_item.value " +
                            "from (log_entry, job, job_type) " +
                            "left join additional_item on log_entry.id = additional_item.log_entry_id " +
                            "left join organization on log_entry.org_id = organization.id " +
                            "left join prio on organization.prio_id = prio.id " + 
                            "where " +
                            "log_entry.log_timestamp >= ? and log_entry.log_timestamp <= ? and " +
                            "log_entry.job_id = job.id and " +
                            "job.job_type_id = job_type.id and " +
                            "job_type.name not in (@inList@) " + 
                            "order by log_entry.log_timestamp, log_entry.id asc;";
            if ((jobTypeKillList == null) || jobTypeKillList.isEmpty()) {
                jobTypeKillList = new ArrayList<String>();
                jobTypeKillList.add("");
            }
            String inList = StringUtil.toSingleQuotedString(jobTypeKillList);
            queryStr = StringUtil.replace(queryStr, "@inList@", inList);

            // -- execute query
            log.debug("Executing query: " + queryStr);
            long t1 = System.currentTimeMillis();
            ps = session.connection().prepareStatement(queryStr);
            ps.setLong(1, SqlUtil.convertTimestamp(startDate));
            ps.setLong(2, SqlUtil.convertTimestamp(endDate));
            boolean successful = ps.execute();
            String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - t1);
            log.debug("Query executed. Time: " + durationStr);

            // -- read result set; only one LogEntry per ip-address.
            final String dbIdKey = "dbId";
            final String timesSeenKey = "timesSeen";
            final String lastSeenKey = "lastSeen";
            final String prioNameKey = "prioName";
            final String infectionKey = "infection";
            if (successful) {
                long id = 0L;
                Map<Long, LogEntry> ipAddressMap = new HashMap<Long, LogEntry>();
                ResultSet resultSet = ps.getResultSet(); 
                while (resultSet.next()) {
                    Long ipAddress = resultSet.getLong(4);
                    if (ipAddress == null) {
                        ipAddress = resultSet.getLong(5);
                        if (ipAddress == null) {
                            continue;
                        }
                    }
                    LogEntry logEntry = ipAddressMap.get(ipAddress);
                    if (logEntry != null) {
                        int timesSeen = Integer.parseInt(logEntry.getAdditionalItems().get(timesSeenKey)); 
                        logEntry.getAdditionalItems().put(timesSeenKey, Integer.toString(++timesSeen));
                        long lastSeen = Long.parseLong(logEntry.getAdditionalItems().get(lastSeenKey));
                        long maxLastSeen = Math.max(lastSeen, resultSet.getLong(2));
                        logEntry.getAdditionalItems().put(lastSeenKey, Long.toString(maxLastSeen));
                    } 
                    else {
                        logEntry = new LogEntry(id++);
                        ipAddressMap.put(ipAddress, logEntry);
                        result.add(logEntry);
                        if (logEntry.getAdditionalItems() == null) {
                            logEntry.setAdditionalItems(new HashMap<String, String>());
                        }
                        logEntry.setIpAddress(ipAddress);
                        // logTimestamp == firstSeen
                        logEntry.setLogTimestamp(resultSet.getLong(2));
                        logEntry.setCreated(resultSet.getLong(3));
                        logEntry.setPort(resultSet.getInt(6));
                        logEntry.setHostname(resultSet.getString(7));
                        logEntry.setAsn(resultSet.getLong(8));
                        logEntry.getAdditionalItems().put(dbIdKey, Long.toString(resultSet.getLong(1))); 
                        logEntry.getAdditionalItems().put(timesSeenKey, "1");
                        logEntry.getAdditionalItems().put(lastSeenKey, logEntry.getLogTimestamp().toString());
                        logEntry.getAdditionalItems().put(prioNameKey, StringUtil.getNotNull(resultSet.getString(9)));
                    }
                    // add infection if missing
                    String infection = logEntry.getAdditionalItems().get(infectionKey);
                    if (StringUtil.isNullOrEmpty(infection)) {
                        infection = null;
                        String name = resultSet.getString(10);
                        if ((name != null) && name.equals(infectionKey)) {
                            infection = resultSet.getString(11);
                            if ((infection != null) && (infection.equals("irc") || infection.equalsIgnoreCase("sinkhole") || infection.equalsIgnoreCase("other"))) {
                                infection = null;
                            }
                        }
                        logEntry.getAdditionalItems().put(infectionKey, StringUtil.getNotNull(infection));
                    }
                }
            }
        } catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in fetchLogEntriesForGeolocation", e);
        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException ignored) { }
        }

        return result;
    }


    /**
     * Fetches log entries for specified organization for use in OrganizationReportGenerator.
     * 
     * @param jobTypes list of job types (db-field "job_type.name") for log entries to be included in the result.
     */
    @SuppressWarnings("deprecation")
    public List<LogEntry> fetchLogEntriesForOrganizationReport(int orgId, Date startDate, Date endDate, List<String> jobTypes) 
            throws DbException {
        List<LogEntry> entries = new ArrayList<LogEntry>(2048);
        PreparedStatement ps = null;
        if ((jobTypes == null) || jobTypes.isEmpty()) {
            return entries;
        }

        try {
            String inList = StringUtil.toSingleQuotedString(jobTypes);
            String sql = 
                    "SELECT le.id from log_entry le, job j, job_type jt " +
                            "WHERE j.finished >= ? AND j.finished <= ? AND j.job_type_id = jt.id AND jt.name in (" + inList + ") AND le.job_id = j.id AND le.org_id = ?;";

            log.debug("Executing query: " + sql);
            ps = session.connection().prepareStatement(sql);            
            ps.setLong(1, SqlUtil.convertTimestamp(startDate));
            ps.setLong(2, SqlUtil.convertTimestamp(endDate));
            ps.setInt(3, orgId);
            boolean successful = ps.execute();

            if (successful) {
                ResultSet resultSet = ps.getResultSet(); 
                while (resultSet.next()) {
                    long entryID = resultSet.getLong(1);
                    LogEntry entry = (LogEntry)this.loadLongObject(LogEntry.class, entryID);
                    entries.add(entry);
                }
            }
        } catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in fetchLogEntriesForOrganizationReport", e);
        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException ignored) { }
        }

        return entries;
    }


    public long getNumberOfSuccessfulLogJobs(Date startDate, Date endDate) 
            throws DbException {
        try {
            /** No. of successful log jobs (excluding mail jobs). */
            String sql = "SELECT COUNT(*) FROM job WHERE finished >= ? AND finished <= ? AND error_msg IS NULL";
            Query query = session.createSQLQuery(sql);

            query.setLong(0, SqlUtil.convertTimestamp(startDate));
            query.setLong(1, SqlUtil.convertTimestamp(endDate));

            return (query.list() != null) ? ((BigInteger)query.list().get(0)).longValue() : 0;
        } 
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getNumberOfSuccessfulLogJobs", e);
        }

    }


    public long getNumberOfFailedLogJobs(Date startDate, Date endDate) 
            throws DbException {
        try {    
            /** No. of failed log jobs (excluding mail jobs). */
            String sql = "SELECT COUNT(*) FROM job WHERE started >= ? AND started <= ? AND finished is null";
            Query query = session.createSQLQuery(sql);
            query.setLong(0, SqlUtil.convertTimestamp(startDate));
            query.setLong(1, SqlUtil.convertTimestamp(endDate));

            return (query.list() != null) ? ((BigInteger)query.list().get(0)).longValue() : 0;
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getNumberOfFailedLogJobs", e);
        }   
    }


    public long getNumberOfProcessedLines(Date startDate, Date endDate) 
            throws DbException { 
        try {
            /** No. of processed lines (after possible split or merge). */
            String sql = "SELECT SUM(processed_lines) FROM job WHERE finished >= ? AND finished <= ?";
            Query query = session.createSQLQuery(sql);
            query.setLong(0, SqlUtil.convertTimestamp(startDate));
            query.setLong(1, SqlUtil.convertTimestamp(endDate));

            return ((query.list() != null) && (query.list().get(0) != null)) ? ((java.math.BigDecimal)query.list().get(0)).longValue() : 0;
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getNumberOfProcessedLines", e);
        }
    }


    public long getNumberOfLogEntries(Date startDate, Date endDate) 
            throws DbException { 
        try {

            /** No. of processed lines (after possible split or merge). */
            String sql = "SELECT COUNT(*) FROM job j, log_entry l WHERE j.finished >= ? AND j.finished <= ? AND j.id = l.job_id" ;
            Query query = session.createSQLQuery(sql);
            query.setLong(0, SqlUtil.convertTimestamp(startDate));
            query.setLong(1, SqlUtil.convertTimestamp(endDate));

            return (query.list() != null) ? ((BigInteger)query.list().get(0)).longValue() : 0;
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getNumberOfLogEntries", e);
        }
    }


    public long getNumberOfMatchedLogEntries(Date startDate, Date endDate) 
            throws DbException { 
        try {

            /** No. of log entries that have been matched to an organisation. */
            String sql = "SELECT COUNT(*) FROM job j, log_entry l WHERE j.finished >= ? AND j.finished <= ? AND j.id = l.job_id AND (org_id IS NOT NULL or org_id2 IS NOT NULL)" ;
            Query query = session.createSQLQuery(sql);
            query.setLong(0, SqlUtil.convertTimestamp(startDate));
            query.setLong(1, SqlUtil.convertTimestamp(endDate));

            return (query.list() != null) ? ((BigInteger)query.list().get(0)).longValue() : 0;
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getNumberOfMatchedLogEntries", e);
        }
    }


    public long getNumberOfSentEmails(Date startDate, Date endDate) 
            throws DbException { 
        try {

            /** No. of sent emails in all mail jobs. */
            String sql = "SELECT DISTINCT le.org_id FROM log_entry le, mail_job mj, mail_job_log_entry_mapping mjle WHERE le.id = mjle.log_entry_id AND mj.id = mjle.mail_job_id AND mj.started >= ? AND mj.finished <= ?;";

            Query query = session.createSQLQuery(sql);
            query.setLong(0, SqlUtil.convertTimestamp(startDate));
            query.setLong(1, SqlUtil.convertTimestamp(endDate));

            List<?> rs = query.list();

            return rs != null ? (long)rs.size() : 0;
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getNumberOfSentEmails", e);
        }
    }


    public long getNumberOfNewOrganizations(Date startDate, Date endDate) 
            throws DbException { 
        try {
            /** No. of created organizations. */        
            String sql = "SELECT COUNT(*) FROM organization WHERE created >= ? AND created <= ?";
            Query query = session.createSQLQuery(sql);
            query.setLong(0, SqlUtil.convertTimestamp(startDate));
            query.setLong(1, SqlUtil.convertTimestamp(endDate));

            return (query.list() != null) ? ((BigInteger)query.list().get(0)).longValue() : 0;
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getNumberOfNewOrganizations", e);
        }
    }


    public long getNumberOfModifiedOrganizations(Date startDate, Date endDate) 
            throws DbException { 
        try {    
            /** No. of modified organizations. */        
            String sql = "SELECT COUNT(*) FROM organization WHERE last_modified >= ? AND last_modified <= ? AND created <  last_modified";
            Query query = session.createSQLQuery(sql);
            query.setLong(0, SqlUtil.convertTimestamp(startDate));
            query.setLong(1, SqlUtil.convertTimestamp(endDate));

            return (query.list() != null) ? ((BigInteger)query.list().get(0)).longValue() : 0;
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getNumberOfModifiedOrganizations", e);
        }   
    }


    public boolean isRangeOverlapping(long start, long end) 
            throws DbException { 
        try {
            String sql = "SELECT COUNT(*) FROM ip_range WHERE (end_address >= ? AND end_address <= ?) OR (start_address <=  ? AND end_address >= ?) OR  (start_address >= ? AND start_address <= ?) OR (start_address >= ? AND end_address <= ?)";

            Query query = session.createSQLQuery(sql);
            query.setLong(0, start);   
            query.setLong(1, end);
            query.setLong(2, start);
            query.setLong(3, end);
            query.setLong(4, start);
            query.setLong(5, end);
            query.setLong(6, start);
            query.setLong(7, end);

            long result = ((BigInteger)query.list().get(0)).longValue();

            return query.list() != null && result > 0;
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in isRangeOverlapping", e);
        }           
    }


    @SuppressWarnings("unchecked")
    public List<BigInteger> isRangeOverlappingOrgId(long start, long end)
            throws DbException {

        try {
            String sql = "SELECT ORG_ID FROM ip_range WHERE (end_address >= ? AND end_address <= ?) OR (start_address <=  ? AND end_address >= ?) OR  (start_address >= ? AND start_address <= ?) OR (start_address >= ? AND end_address <= ?)";

            Query query = session.createSQLQuery(sql);
            query.setLong(0, start);
            query.setLong(1, end);
            query.setLong(2, start);
            query.setLong(3, end);
            query.setLong(4, start);
            query.setLong(5, end);
            query.setLong(6, start);
            query.setLong(7, end);

            return query.list();

        } catch (Exception e) {
            throw handleException(e.getClass().getSimpleName()
                    + " exception in isRangeOverlappingOrgId", e);
        }
    }


    @SuppressWarnings("unchecked")
    public List<Contact> searchContactsByOrgIDsAndRoles(String[] orgIDs, String roles) 
            throws DbException {

        /*
         * Searches for contacts by orgId (if present) and roles (if present).
         * If no criterias are given, all contacts are returned.
         */
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        Query query = null;


        if (orgIDs != null && orgIDs.length > 0) {
            for (String orgID : orgIDs) {           
                if (roles != null && roles.isEmpty() == false) {
                    // Search by orgId and roles
                    query = session.createQuery("from Contact where OrganizationId = ? and Role in (:roles)");              
                    query.setParameterList("roles", roles.split(","));

                    //System.out.println("1" + query.getQueryString() + " roles: " + roles);
                }   
                else {
                    // Search by orgId only
                    query = session.createQuery("from Contact where OrganizationId = ?");       
                    //System.out.println("2" + query.getQueryString());

                }
                query.setInteger(0, Integer.parseInt(orgID));
                //System.out.println("3" + query.getQueryString());
                //System.out.println("Found " + query.list().size() + " number of contacts.");
                if (query.list().isEmpty() == false) {              
                    contacts.addAll((List<Contact>)query.list());
                }
            }
        }
        else {

            if (roles != null && roles.isEmpty() == false) {    
                // Search by roles only
                query = session.createQuery("from Contact where Role in (:roles)");             
                query.setParameterList("roles", roles.split(","));
            }
            else {
                // Search all contacts
                query = session.createQuery("from Contact");
            }

            contacts.addAll((List<Contact>)query.list());
        }
        return contacts;

    }


    @SuppressWarnings("unchecked")
    public List<Contact>  searcContactsByPrioAndRoles(String prioHigh, String prioLow, String searchRoles)
            throws DbException {

        ArrayList<Contact> contacts = new ArrayList<Contact>();

        Query query = null;
        query = session.createQuery("from Organization where Priority.Prio <= ? and Priority.Prio >= ?");
        query.setString(0, prioHigh);
        query.setString(1, prioLow);

        if (query.list() != null) {         
            List<Organization> orgs = query.list();         
            Iterator<Organization> itr = orgs.iterator();           
            while (itr.hasNext()) {                     
                Set<Contact> orgContacts = ((Organization)itr.next()).getContacts();

                if (orgContacts != null && orgContacts.isEmpty() == false) {
                    if (searchRoles != null && searchRoles.isEmpty() == false) {
                        for (Contact contact : orgContacts) {
                            if (contact != null && contact.getRole() != null && searchRoles.contains(contact.getRole())) {

                                contacts.add(contact);                              
                            }
                        }
                    }
                    else {
                        contacts.addAll(orgContacts);
                    }
                }                               
            }                       
        }       

        return contacts;


    }

    @SuppressWarnings("unchecked")
    public List<String> getAllContactRoleNames() 
            throws DbException {
        try {
            String sql = "SELECT DISTINCT role FROM contact where role is not null ORDER BY role";
            Query query = session.createSQLQuery(sql);

            return (List<String>)query.list();

        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in getAllContactRoleNames", e);
        }

    }


    public void genericDelete(Object obj) 
            throws DbException {
        try {
            session.delete(obj);
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in genericDelete", e);
        }
    }


    public void flushSession()
            throws DbException {
        try {    
            session.flush();
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in flushSession", e);
        }
    }    

    public void close() 
            throws DbException {

        try {
            session.close();
        }
        catch (Exception e) {
            throw handleException(e.getClass().getSimpleName() + " exception in close", e);
        }
    }

    private DbException handleException (String msg, Exception e) {

        return new DbException(msg, e);

    }

}
