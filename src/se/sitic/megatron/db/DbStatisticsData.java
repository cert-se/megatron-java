package se.sitic.megatron.db;

import java.util.Date;


/**
 * Data holder class that contains statistics for a period, e.g. number of log
 * entries, sent mails, etc. 
 */
public class DbStatisticsData {
    /** Period start. */
    private Date startDate;
    
    /** Period end. */
    private Date endDate;
    
    /** No. of successful log jobs (excluding mail jobs). */
    private long noOfSuccessfulJobs;
    
    /** No. of failed log jobs (excluding mail jobs). */
    private long noOfFailedJobs;
    
    /** No. of processed lines (after possible split or merge). */
    private long noOfProcessedLines;
    
    /** No. of log entries. */
    private long noOfLogEntries;

    /** No. of log entries that have been matched to an organisation. */
    private long noOfMatchedLogEntries;
    
    /** No. of sent emails in all mail jobs. */
    private long noOfSentEmails;
    
    /** No. of created organizations. */
    private long noOfNewOrganizations;

    /** No. of modified organizations. */
    private long noOfModifiedOrganizations;

    
    public DbStatisticsData(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    
    public void inc(DbStatisticsData statsData) {
        this.noOfSuccessfulJobs += statsData.getNoOfSuccessfulJobs();
        this.noOfFailedJobs += statsData.getNoOfFailedJobs();
        this.noOfProcessedLines += statsData.getNoOfProcessedLines();
        this.noOfLogEntries += statsData.getNoOfLogEntries();
        this.noOfMatchedLogEntries += statsData.getNoOfMatchedLogEntries(); 
        this.noOfSentEmails += statsData.getNoOfSentEmails();
        this.noOfNewOrganizations += statsData.getNoOfNewOrganizations();
        this.noOfModifiedOrganizations += statsData.getNoOfModifiedOrganizations();
    }


    public long getNoOfSuccessfulJobs() {
        return noOfSuccessfulJobs;
    }


    public void setNoOfSuccessfulJobs(long noOfSuccessfulJobs) {
        this.noOfSuccessfulJobs = noOfSuccessfulJobs;
    }

    
    public long getNoOfFailedJobs() {
        return noOfFailedJobs;
    }


    public void setNoOfFailedJobs(long noOfFailedJobs) {
        this.noOfFailedJobs = noOfFailedJobs;
    }

    
    public long getNoOfProcessedLines() {
        return noOfProcessedLines;
    }


    public void setNoOfProcessedLines(long noOfProcessedLines) {
        this.noOfProcessedLines = noOfProcessedLines;
    }


    public long getNoOfLogEntries() {
        return noOfLogEntries;
    }


    public void setNoOfLogEntries(long noOfLogEntries) {
        this.noOfLogEntries = noOfLogEntries;
    }


    public long getNoOfMatchedLogEntries() {
        return noOfMatchedLogEntries;
    }


    public void setNoOfMatchedLogEntries(long noOfMatchedLogEntries) {
        this.noOfMatchedLogEntries = noOfMatchedLogEntries;
    }


    public long getNoOfSentEmails() {
        return noOfSentEmails;
    }


    public void setNoOfSentEmails(long noOfSentEmails) {
        this.noOfSentEmails = noOfSentEmails;
    }


    public long getNoOfNewOrganizations() {
        return noOfNewOrganizations;
    }


    public void setNoOfNewOrganizations(long noOfNewOrganizations) {
        this.noOfNewOrganizations = noOfNewOrganizations;
    }


    public long getNoOfModifiedOrganizations() {
        return noOfModifiedOrganizations;
    }


    public void setNoOfModifiedOrganizations(long noOfModifiedOrganizations) {
        this.noOfModifiedOrganizations = noOfModifiedOrganizations;
    }


    public Date getStartDate() {
        return startDate;
    }


    public Date getEndDate() {
        return endDate;
    }
    
}
