package se.sitic.megatron.core;

import se.sitic.megatron.entity.MailJob;


/**
 * Context for a mail job. 
 */
public class MailJobContext extends JobContext {
    private MailJob mailJob;
    
    /** No. of sent mails. 0 if --dry-run. */
    private long noOfSentMails = 0L;

    /** No. of sent log entries. 0 if --dry-run. */
    private long noOfSentLogEntries = 0L;
    
    /** No. of log entries that have been ignored because recipient already have been notified. */
    private int noOfQuarantineLogEntries;

    /** No. of mails to be sent. Used when --dry-run is specified. */
    private long noOfMailsTobeSent = 0L;

    
    public MailJobContext(TypedProperties props, MailJob mailJob) {
        super(props, mailJob.getJob());
        this.mailJob = mailJob;
    }

    
    public MailJob getMailJob() {
        return mailJob;
    }

    
    public long getNoOfSentMails() {
        return noOfSentMails;
    }

    
    public void incNoOfSentMails(int incValue) {
        this.noOfSentMails += incValue;
    }


    public long getNoOfSentLogEntries() {
        return noOfSentLogEntries;
    }
    
    
    public void incNoOfSentLogEntries(int incValue) {
        this.noOfSentLogEntries += incValue;
    }


    public int getNoOfQuarantineLogEntries() {
        return noOfQuarantineLogEntries;
    }

    
    public void incNoOfQuarantineLogEntries(int incValue) {
        this.noOfQuarantineLogEntries += incValue;
    }
    
    
    public long getNoOfMailsTobeSent() {
        return noOfMailsTobeSent;
    }

    
    public void incNoOfMailsTobeSent(int incValue) {
        this.noOfMailsTobeSent += incValue;
    }

}
