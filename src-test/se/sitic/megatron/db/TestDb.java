package se.sitic.megatron.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.ASNumber;
import se.sitic.megatron.entity.DomainName;
import se.sitic.megatron.entity.EntryType;
import se.sitic.megatron.entity.IpRange;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.JobType;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.MailJob;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.entity.OriginalLogEntry;
import se.sitic.megatron.entity.Priority;
import se.sitic.megatron.util.SqlUtil;

public class TestDb {

    private static Session session = null;
    private static Transaction tx = null;
    private static DbManager dBm = null;
    private static Logger log = null;
    private static boolean TRUNCATE = true;
    @SuppressWarnings("unused")
    private static boolean DONT_TRUNCATE = !TRUNCATE;
    

    private static void setUpDb() {
        
        SessionFactory sessionFactory = new Configuration().configure()
        .buildSessionFactory();
        session = sessionFactory.openSession();

        log = Logger.getLogger(TestDb.class);
        
        TypedProperties props = new TypedProperties(
                new HashMap<String, String>(), null);
        try {
            dBm = DbManager.createDbManager(props);
        } catch (DbException e) {
            e.printStackTrace();
        }
        
    }
    
    private static void setUpTables(boolean truncate) {
       

        try {
            

            if (truncate == TRUNCATE) {

                tx = session.beginTransaction();
                truncateAllTables();

                // Set up priorities
                Priority prio1 = setUpPriorities();

                setUpOrganisation(prio1);

                // Setting up entry type

                EntryType entryType = setUpEntryTypes();

                setUpJobTypes(entryType);

                tx.commit();
                session.close(); 

            }

        } catch (DbException dbe) {
            System.out.println("DB exception");
            dbe.printStackTrace();
        }

    }

    private static void setUpJobTypes(EntryType entryType) {
        // Setting up jobType
        JobType jobType = new JobType();
        jobType.setComment("First job type");
        jobType.setEnabled(true);
        jobType.setEntryType(entryType);
        jobType.setName("MyJobType");
        jobType.setSourceDescription("Job type source");

        session.save(jobType);
    }

    private static EntryType setUpEntryTypes() {
        EntryType entryType = new EntryType();
        entryType.setName("Test_entry_type_1");
        session.save(entryType);
        EntryType entryType2 = new EntryType();
        entryType2.setName("Test_entry_type_2");
        session.save(entryType2);
        return entryType;
    }

    private static void setUpOrganisation(Priority prio1) throws DbException {
        // Set up IP-ranges
        IpRange ipRange1 = new IpRange();
        ipRange1.setStartAddress(1000L);
        ipRange1.setEndAddress(2000L);
        ipRange1.setNetName("Net-ett");
        
        IpRange ipRange2 = new IpRange();
        ipRange2.setStartAddress(3000L);
        ipRange2.setEndAddress(4000L);
        ipRange2.setNetName("Net-tva");

        // Set up organization
        Organization org = testAddOrganization(prio1, "First org");

        org.addToIpRanges(ipRange1);
        // System.out.println("ORG id = " + ipRange.getOrganizationId());
        org.addToIpRanges(ipRange2);

        DomainName dName1 = new DomainName("www.test.se");
        DomainName dName2 = new DomainName("     www.sitic.se     ");
        DomainName dName3 = new DomainName("www.pts.se");

        /*
         * dName1.setName("www.test.se"); dName2.setName("www.sitic.se");
         * dName3.setName("www.pts.se");
         */
        org.addToDomainNames(dName1);
        org.addToDomainNames(dName2);
        org.addToDomainNames(dName3);

        ASNumber as1 = new ASNumber(111111L);
        ASNumber as2 = new ASNumber(222222L);
        ASNumber as3 = new ASNumber(333333L);

        org.addToASNumbers(as1);
        org.addToASNumbers(as2);
        org.addToASNumbers(as3);

        session.update(org);
    }

    private static Priority setUpPriorities() {
        Priority prio1 = new Priority();
        Priority prio2 = new Priority();
        Priority prio3 = new Priority();
        prio1.setName("High");
        prio1.setPrio(1);
        prio2.setName("Meduim");
        prio2.setPrio(2);
        prio3.setName("Low");
        prio3.setPrio(3);

        session.save(prio1);
        session.save(prio2);
        session.save(prio3);
        return prio1;
    }

    private static void truncateAllTables() {
        Query q = session.createSQLQuery("truncate ip_range");
        q.executeUpdate();
        q = session.createSQLQuery("truncate ip_range");
        q.executeUpdate();
        q = session.createSQLQuery("truncate additional_item");
        q.executeUpdate();
        q = session.createSQLQuery("truncate asn");
        q.executeUpdate();
        q = session.createSQLQuery("truncate domain_name");
        q.executeUpdate();
        q = session.createSQLQuery("truncate entry_type");
        q.executeUpdate();
        q = session.createSQLQuery("truncate free_text");
        q.executeUpdate();
        q = session.createSQLQuery("truncate ip_range");
        q.executeUpdate();
        q = session.createSQLQuery("truncate job");
        q.executeUpdate();
        q = session.createSQLQuery("truncate job_type");
        q.executeUpdate();
        q = session.createSQLQuery("truncate log_entry");
        q.executeUpdate();
        q = session.createSQLQuery("truncate mail_job");
        q.executeUpdate();
        q = session.createSQLQuery("truncate mail_job_log_entry_mapping");
        q.executeUpdate();
        q = session.createSQLQuery("truncate organization");
        q.executeUpdate();
        q = session.createSQLQuery("truncate original_log_entry");
        q.executeUpdate();
        q = session.createSQLQuery("truncate prio");
        q.executeUpdate();
    }

    private static void logMethod(String method) {

        log.debug("========================== " + method
                + "==========================");

    }

    private static Organization testAddOrganization(Priority prio1, String name)
            throws DbException {
        Organization org = new Organization();

        org.setComment("My organization");
        org.setCountryCode("se");
        org.setDescription("The organisation");
        org.setEmailAddresses("apa@bepa.se, bepa@apa.se");
        org.setEnabled(false);
        org.setLanguageCode("sv");
        org.setModifiedBy("gope");
        org.setName(name);
        org.setRegistrationNo("202100-4359");
        org.setPriority(prio1);
        java.util.Date date = new java.util.Date();
        long time = SqlUtil.convertTimestamp(date);
        org.setCreated(time);
        org.setLastModified(time);
        dBm.addOrganization(org);
        return org;
    }

    private static void tearDown() throws DbException {

        if (tx.isActive()) {
            tx.commit();
        }
        if (session.isOpen()) {
            session.close();
        }

        dBm.close();

    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        try {
            
            setUpDb();           
            
            setUpTables(TRUNCATE);
            
            java.util.Date date = new java.util.Date();

            long time = SqlUtil.convertTimestamp(date);
            
            //Job theJob = (Job)dBm.genericLoadObject("Job", "Id", 1);
            
            //dBm.deleteLogJob(theJob);
                                             

            
            
            testSeachEntryType();

            JobType jobType = testSearchJobType();
            String name = "Job1";
            Job job1 = testAddLogJob(time, jobType, name);
            name = "Job2";
            
            @SuppressWarnings("unused")
            Job job2 = testAddLogJob(time, jobType, name);

            job1 = testSearchLogJob(job1);

            testSearchOrganization();

            LogEntry entry1 = testAddLogEntry(createOriginalLogEntry(1), new Date(1000000),
                    job1);
            LogEntry entry2 = testAddLogEntry(createOriginalLogEntry(2), new Date(2000000),
                    job1);
            LogEntry entry3 = testAddLogEntry(createOriginalLogEntry(3), new Date(3000000),
                    job1);
                                            
            testUpdateLogEntry(entry1);

            testListLogEntries(job1.getId());

            testUpdateOrganization();

            testSearchOrganizations();

            // testSearchOrganizationsNoResult();

            testListOrganizations();

            testGetOrganization();

            testSearchOrgForDomain();
            
            testFinishLogJob(job1);
            
            testFinishLogJob(job2);
            
            testFetchNoOfProcessedLinesPerJobType(new Date(0), new Date());
            
            testFefetchNoOfLogEntriesPerJobType(new Date(0), new Date(), false);
            
            testFefetchNoOfLogEntriesPerJobType(new Date(0), new Date(), true);
            
            
            // Test delete of huge logJob
            //testDeleteHugeLogJob(job2, 20);
            
            

            MailJob mailJob = testAddMailJob(job1);

            mailJob.setJob(job1);

            mailJob.addToLogEntries(entry1);

            mailJob.setComment("Test comment");

            testUpdateMailJob(mailJob);

            mailJob.setComment("Test comment again");

            mailJob.addToLogEntries(entry2);
            testUpdateMailJob(mailJob);
            mailJob.addToLogEntries(entry3);
            testUpdateMailJob(mailJob);

            testFinishMailJob(mailJob, null);

            testGetAllIpRanges();

            testGetAllDomainNames();

            testGetAllASNumbers();

            testExistsMailForIp();

            testStats(new Date(0), new Date());
            
            //testDeleteMailJob(mailJob);

            //log.debug("number of entries in job " + job1.getId() + " is " + job1.getLogEntries().size());
            
            //testDeleteJob(job1);

            tearDown();

        } catch (HibernateException he) {
            System.out.println("HibernateException ");
            he.printStackTrace();
        } catch (DbException dbe) {
            System.out.println("DbException ");
            dbe.printStackTrace();
        }

    }

    private static OriginalLogEntry createOriginalLogEntry(int index) {
        OriginalLogEntry oLogEntry = new OriginalLogEntry();
        java.util.Date date = new java.util.Date(111111110 + index);        
        oLogEntry.setCreated(date.getTime());
        oLogEntry.setEntry("The log row_" + index);
        oLogEntry.setEntry("The log entry text_" + index);
        return oLogEntry;
    }
    
    private static void testFinishMailJob(MailJob mailJob, String errorMsg)
            throws DbException {
        dBm.finishMailJob(mailJob, errorMsg);
    }

    private static Job testSearchLogJob(Job job) throws DbException {
        job = dBm.searchLogJob(job.getName());
        return job;
    }

    private static EntryType testSeachEntryType() throws DbException {
        EntryType eType = dBm.searchEntryType("Test_entry_type_1");
        return eType;
    }

    private static void testListOrganizations() throws DbException {
        ArrayList<Organization> orgs = (ArrayList<Organization>) dBm
                .listOrganizations(0, 10);
        if (orgs.size() != 3) {
            throw new DbException(
                    "Error wrong number of orgs found, should be three");
        }
        orgs = (ArrayList<Organization>) dBm.listOrganizations(0, 2);
        if (orgs.size() != 2) {
            throw new DbException(
                    "Error wrong number of orgs found, should be two");
        }
        orgs = (ArrayList<Organization>) dBm.listOrganizations(1, 10);
        if (orgs.size() != 2) {
            throw new DbException(
                    "Error wrong number of orgs found, should be two");
        }

        orgs = (ArrayList<Organization>) dBm.listOrganizations(0, 10);

        java.util.Iterator<Organization> i = orgs.iterator();
        while (i.hasNext()) {
            System.out.println("Org: " + i.next().getName() + "found");
        }

    }

    private static void testSearchOrganizations() throws DbException {
        Priority prio2 = dBm.getPriority(2);
        Priority prio3 = dBm.getPriority(3);
        testAddOrganization(prio2, "Second org");
        testAddOrganization(prio3, "Third org");

        ArrayList<Organization> orgs = (ArrayList<Organization>) dBm
                .searchOrganizations("org", 0, 10);
        if (orgs.size() != 3) {
            throw new DbException("Error wrong number of orgs found");
        }
    }

    @SuppressWarnings("unused")
    private static void testSearchOrganizationsNoResult() throws DbException {

        ArrayList<Organization> orgs = (ArrayList<Organization>) dBm
                .searchOrganizations("not there", 0, 10);
        if (orgs == null) {
            throw new DbException("List is null");
        }
        if (orgs.size() == 0) {
            throw new DbException("List is empty");
        }
    }

    private static void testUpdateOrganization() throws DbException {
        Priority prio = dBm.getPriority(2);
        Organization org = dBm.searchOrganization("First org");
        org.setPriority(prio);
        dBm.updateOrganization(org, "TestDb");
    }

    private static Organization testSearchOrganization() throws DbException {
        Organization org = dBm.searchOrganization("First org");
        return org;
    }

    private static Organization testGetOrganization() throws DbException {
        Organization org1 = dBm.searchOrganization("First org");
        Organization org2 = dBm.getOrganization(org1.getId().intValue());

        if (org1.equals(org2) == false) {
            System.out.println("Error Organization not found!");
        }
        return org2;
    }

    private static void testUpdateLogEntry(LogEntry entry) throws DbException {
        entry.getFreeTexts().add("Text4 text text text");
        entry.getAdditionalItems().put("other item", "other value");
        dBm.updateLogEntry(entry);
    }

    private static void testListLogEntries(long jobId) throws DbException {

        ArrayList<LogEntry> logEntries = (ArrayList<LogEntry>) dBm
                .listLogEntries(jobId, 0, 100);

        java.util.Iterator<LogEntry> i = logEntries.iterator();
        while (i.hasNext()) {
            System.out.println("log entry id " + i.next().getId() + " found");
        }

        logEntries = (ArrayList<LogEntry>) dBm.listLogEntries(jobId, true, 0,
                100);

        i = logEntries.iterator();
        while (i.hasNext()) {
            System.out.println("log entry id " + i.next().getId() + " found");
        }

    }

    private static MailJob testAddMailJob(Job job) throws DbException {
        MailJob mailJob = new MailJob();
        mailJob.setJob(job);
        // mailJob.

        dBm.addMailJob(mailJob);

        return mailJob;
    }

    private static void testUpdateMailJob(MailJob mailJob) throws DbException {

        logMethod("testUpdateMailJob start");
        logMethod("mailJob comment " + mailJob.getComment());
        dBm.updateMailJob(mailJob);
        logMethod("testUpdateMailJob end");

    }

    private static void testFinishLogJob(Job job) throws DbException {
        
        job.setProcessedLines(12345678L);        
        dBm.finishLogJob(job, null);
    }

    private static Job testAddLogJob(long time, JobType jobType, String name) throws DbException {
        Job job = new Job();
        job.setComment("Test job");
        job.setFileHash("ABC123DEF");
        job.setFilename("test.log");
        job.setFileSize(12345L);
        job.setName(name);
        job.setStarted(time);
        job.setJobType(jobType);

        dBm.addLogJob(job);
        return job;
    }

    private static JobType testSearchJobType() throws DbException {
        JobType jobType = dBm.searchJobType("MyJobType");
        return jobType;
    }

    private static LogEntry testAddLogEntry(OriginalLogEntry oLogEntry,
            Date time, Job job) throws DbException {

        Organization org = testSearchOrganization();

        LogEntry logEntry = new LogEntry();
        logEntry.setAsn(2311L);
        logEntry.setAsn2(2893L);
        logEntry.setCountryCode("se");
        logEntry.setCountryCode2("no");
        logEntry.setCreated(SqlUtil.convertTimestamp(new Date()));
        logEntry.setHostname("apa.se");
        logEntry.setHostname2("bepa.no");
        logEntry.setIpAddress(1234567L);
        logEntry.setIpAddress2(1234567L);
        logEntry.setLogTimestamp(SqlUtil.convertTimestamp(time));
        logEntry.setOrganization(org);
        logEntry.setOrganization2(org);
        logEntry.setJob(job);
        logEntry.setUrl("Test URL");
        logEntry.setPort(80);
        logEntry.setPort2(443);
        logEntry.setOriginalLogEntry(oLogEntry);
        Map<String, String> items = new HashMap<String, String>();
        items.put("key1", "value");
        items.put("key2", "value2");
        items.put("key3", "value3");
        logEntry.setAdditionalItems(items);
        ArrayList<String> fTexts = new ArrayList<String>();
        fTexts.add("Text text text text");
        fTexts.add("Text2 text text text");
        fTexts.add("Text3 text text text");
        logEntry.setFreeTexts(fTexts);
        logEntry.setIpRangeStart(54321L);
        logEntry.setIpRangeEnd(55321L);

        dBm.addLogEntry(logEntry);
        
       
        
        return logEntry;

    }

    public static void testGetAllIpRanges() throws DbException {

        boolean includeDisabled = true;
        
        logMethod("testGetAllIpRanges() start");
        
        log.debug("Testing for all orgs");
        
        ArrayList<IpRange> ipr = (ArrayList<IpRange>) dBm.getAllIpRanges(includeDisabled);

        java.util.Iterator<IpRange> i = ipr.iterator();
        while (i.hasNext()) {
           log.debug("Found IpRange " + i.next().getId());
        }

        log.debug("Testing for enabled orgs");
        
        
        ipr = (ArrayList<IpRange>) dBm.getAllIpRanges(!includeDisabled);

        i = ipr.iterator();
        if (i.hasNext()) {
           log.error("Found ip_range " + i.next().getId());
        }
        else {
            log.debug("Passed, no IpRanges found");
        }

        
        logMethod("testGetAllIpRanges() end");
    }

    
    public static void testGetAllDomainNames() throws DbException {

        boolean includeDisabled = true;
        
        logMethod("testGetAllDomainNames() start");
        
        log.debug("Testing for all orgs");
        
        ArrayList<DomainName> ipr = (ArrayList<DomainName>) dBm.getAllDomainNames(includeDisabled);

        java.util.Iterator<DomainName> i = ipr.iterator();
        while (i.hasNext()) {
           log.debug("Found DomainName " + i.next().getId());
        }

        log.debug("Testing for enabled orgs");
        
        
        ipr = (ArrayList<DomainName>) dBm.getAllDomainNames(!includeDisabled);

        i = ipr.iterator();
        if (i.hasNext()) {
           log.error("Found DomainName " + i.next().getId());
        }
        else {
            log.debug("Passed, no DomainNames found");
        }

        
        logMethod("testGetAllDomainNames() end");
    }
            

    public static void testGetAllASNumbers() throws DbException {
        
        boolean includeDisabled = true;
        
        logMethod("testGetAllASNumbers() start");
        
        log.debug("Testing for all orgs");
        
        ArrayList<ASNumber> ipr = (ArrayList<ASNumber>) dBm.getAllASNumbers(includeDisabled);

        java.util.Iterator<ASNumber> i = ipr.iterator();        
        while (i.hasNext()) {
           log.debug("Found ASNumber " + i.next().getId());
        }

        log.debug("Testing for enabled orgs");
        
        ipr = (ArrayList<ASNumber>) dBm.getAllASNumbers(!includeDisabled);

        i = ipr.iterator();
        if (i.hasNext()) {
           log.error("Found ASNumber " + i.next().getId());
        }
        else {
            log.debug("Passed, no ASNumbers found");
        }

        
        logMethod("testGetAllASNumbers() end");
    }

    
    public static void testStats(Date startDate, Date endDate) throws DbException {
        
        
        log.debug("NumberOfFailedLogJobs 0: " + dBm.getNumberOfFailedLogJobs(startDate, endDate));
        log.debug("NumberOfLogEntries 3: " + dBm.getNumberOfLogEntries());
        log.debug("NumberOfModifiedOrganizations 1: " + dBm.getNumberOfModifiedOrganizations(startDate, endDate));
        log.debug("NumberOfNewOrganizations 3: " + dBm.getNumberOfNewOrganizations(startDate, endDate));
        log.debug("NumberOfProcessedLines 12345678: " + dBm.getNumberOfProcessedLines(startDate, endDate));
        log.debug("NumberOfSentEmails 1: " + dBm.getNumberOfSentEmails(startDate, endDate));
        log.debug("NumberOfSuccessfulLogJobs 1: " + dBm.getNumberOfSuccessfulLogJobs(startDate, endDate));
        log.debug("NumberOfMatchedLogEntries 3: " + dBm.getNumberOfMatchedLogEntries(startDate, endDate));
        log.debug("NumberOfLogEntries 3: " + dBm.getNumberOfLogEntries(startDate, endDate));
        
        
    }
    
    public static void testExistsMailForIp() throws DbException {

        Organization org = testSearchOrganization();

        if (dBm.existsMailForIp(1234567, org, 3600, false)) {
            System.out.println("Yes!");
        } else {
            System.out.println("No!");
        }
    }
    
    public static void testDeleteHugeLogJob(Job job, int numberOfEntries) {
        
        try {
            for (int i = 0; i< numberOfEntries; i++) {




                testAddLogEntry(createOriginalLogEntry(i), new Date(), job);



            }
            log.debug("Deleting huge job");

            testDeleteJob(job);

        } catch (DbException e) {
            e.printStackTrace();
        }
        
        
    }

    public static void testDeleteMailJob(MailJob mj) throws DbException {

        log.debug("Number of logEntries in mailJob = " + dBm.deleteMailJob(mj));
    }

    public static void testDeleteJob(Job job) throws DbException {

        dBm.deleteLogJob(job);
    }
    
    public static void testFetchNoOfProcessedLinesPerJobType(Date startDate, Date endDate)
    throws DbException {
        
        
        Map<String, Long> result = dBm.fetchNoOfProcessedLinesPerEntryType(startDate, endDate);
        
        String[] keys = {} ;        
        keys = result.keySet().toArray(keys);
        
        for (String key : keys) {
            log.debug("JobType: " + key + ", processed lines : " + result.get(key));                        
        }
    }
    
    
    public static void testFefetchNoOfLogEntriesPerJobType(Date startDate, Date endDate, boolean matchedEntries)
    throws DbException {
        
        
        Map<String, Long> result = dBm.fetchNoOfLogEntriesPerEntryType(startDate, endDate, matchedEntries);
        
        String[] keys = {} ;        
        keys = result.keySet().toArray(keys);
        
        for (String key : keys) {
            log.debug("JobType: " + key + ", number of " + (matchedEntries ? "matched" : "") + " entries: " + result.get(key));                        
        }
    }
    
    public static void testSearchOrgForDomain() 
    throws DbException {
        
        String name = "www.test.se";
        
        Organization org = dBm.searchOrgForDomain(name);                
        
        if (org != null) {
            log.debug("Found organization id " + org.getId() + " with matching domainName " + name);
        }
        else {
            log.error("No organization with matching domainName " + name + " was found" );
        }
        
    }
    
    
}
