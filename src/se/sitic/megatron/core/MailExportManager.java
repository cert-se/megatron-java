package se.sitic.megatron.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.MailJob;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.rss.JobRssFile;
import se.sitic.megatron.tickethandler.ITicketHandler;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Coordinates execution of a mail job. 
 */
public class MailExportManager extends AbstractExportManager {
    private final Logger log = Logger.getLogger(MailExportManager.class);
    private final int NO_OF_RECORDS = 1000;

    private MailJobContext mailJobContext;
    private MailJob mailJob;
    private MailExporter mailExporter;

    
    /**
     * Constructor.
     */
    public MailExportManager(TypedProperties props) {
        super(props);
    }

    
    /**
     * Creates a mail job for specified log job, and sends emails if not
     * --dry-runX is enabled.
     */
    public void execute(String jobName) throws MegatronException {
        try {
            this.jobName = jobName;
            log.info("Creating mail job and sending mail for job: " + jobName);
            init();

            dbManager.addMailJob(mailJob);
            
            // -- read and process log entries
            int startIndex = 0;
            long jobId = mailJob.getJob().getId();
            boolean usePrimaryOrg = mailJob.isUsePrimaryOrg();
            long prevOrgId = -1L;
            List<LogEntry> logEntriesWithSameOrgId = new ArrayList<LogEntry>();
            List<LogEntry> logEntries = dbManager.listLogEntries(jobId, usePrimaryOrg, startIndex, NO_OF_RECORDS);
            log.info("No. of log entries fetched: " + logEntries.size());
            while (!logEntries.isEmpty()) {
                for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
                    LogEntry logEntry = iterator.next();
                    Organization organization = getOrganization(logEntry);
                    if ((organization == null) || !organization.isEnabled()) {
                        mailJobContext.incNoOfFilteredLines(1);
                    } else if ((organization.getId() == prevOrgId) || (prevOrgId == -1L)) {
                        logEntriesWithSameOrgId.add(logEntry);
                        prevOrgId = organization.getId();
                    } else {
                        processLogEntries(logEntriesWithSameOrgId);
                        logEntriesWithSameOrgId.clear();
                        logEntriesWithSameOrgId.add(logEntry);
                        prevOrgId = organization.getId();
                    }
                }
                startIndex += NO_OF_RECORDS;
                logEntries = dbManager.listLogEntries(jobId, usePrimaryOrg, startIndex, NO_OF_RECORDS);
                log.info("No. of log entries fetched: " + logEntries.size());
            }
            if (logEntriesWithSameOrgId.size() > 0) {
                processLogEntries(logEntriesWithSameOrgId);
            }
            
            dbManager.finishMailJob(mailJob, null);
            
            mailExporter.sendSummaryMail();
        } catch (Exception e) {
            String mailJobId = null;
            if (mailJob != null) {
                if (mailJob.getId() != null) {
                    mailJobId = mailJob.getId() + "";
                } else {
                    mailJobId = "[mail job id is null]";
                }
            } else {
                mailJobId = "[mail job is null]";
            }
            String msg = "Mail job #" + mailJobId + " failed: " + e.getMessage();
            // stack-trace is logged elsewhere
            log.error(msg);
            MegatronException newException = (e instanceof MegatronException) ? (MegatronException)e : new MegatronException(msg, e);
            msg = "Job failed: " + e.getMessage();
            if ((mailJob != null) && (mailJob.getId() != null) && (mailJob.getId() > 0L)) {
                try {
                    dbManager.finishMailJob(mailJob, msg);
                } catch (MegatronException e2) {
                    String msg2 = "Cannot write error message to mail job; finishMailJob failed.";
                    log.error(msg2, e2);
                }
            }
            throw newException;
        } finally {
            decoratorManager.closeDecorators();
            filterManager.closeFilters();
            try { if (mailExporter != null) mailExporter.close(); } catch (Exception ignored) {}
            try { dbManager.close(); } catch (Exception ignored) {}
        }

        writeFinishedMessage();
        closeTickets();
    }
    
    
    @Override
    protected MailJobContext createJobContext() throws MegatronException {
        MailJob mailJob = new MailJob();
        mailJob.setJob(job);
        mailJob.setUsePrimaryOrg(!props.isUseOrg2());
        mailJob.setStarted(SqlUtil.convertTimestampToSec(System.currentTimeMillis()));

        MailJobContext result = new MailJobContext(props, mailJob); 
        result.setStartedTimestamp(System.currentTimeMillis());
        result.setDbManager(dbManager);

        return result;
    }

    
    @Override
    protected String getPreExportFiltersPropertyKey() {
        return AppProperties.FILTER_PRE_MAIL_KEY;
    }
    
    
    @Override
    protected String getPreExportDecoratorsPropertyKey() {
        return AppProperties.DECORATOR_PRE_MAIL_CLASS_NAMES_KEY;
    }

    
    @Override
    protected void init() throws MegatronException {
        super.init();
        
        mailJobContext = (MailJobContext)jobContext;
        mailJob = mailJobContext.getMailJob();
        mailExporter = new MailExporter(mailJobContext);
    }

    
    private void processLogEntries(List<LogEntry> logEntries) throws MegatronException {
        List<LogEntry> entriesToSend = new ArrayList<LogEntry>(); 
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            decoratorManager.executeDecorators(logEntry);
            if (filterManager.executeFilters(logEntry)) {
                entriesToSend.add(logEntry);
            } else {
                mailJobContext.incNoOfFilteredLines(1);
            }
        }

        if (entriesToSend.size() > 0) {
            mailExporter.sendMail(logEntries);
        } else {
            Organization organization = getOrganization(logEntries.get(0));
            String msg = "All log entries filtered; will not send email to organization#" +  organization.getId() + ": " + organization.getName();
            log.info(msg);
        }
    }

    
    private Organization getOrganization(LogEntry logEntry) {
        return mailJob.isUsePrimaryOrg() ? logEntry.getOrganization() : logEntry.getOrganization2();
    }
    
    
    private void writeFinishedMessage() throws MegatronException {
        // -- write dry-run info
        if (props.isMailDryRun() || props.isMailDryRun2()) {
            if (mailJobContext.getNoOfMailsTobeSent() == 0) {
// A similar message is written in MailExporter.sendSummaryMail.
//                String msg = "No mails to send. Mail job is empty, or all entries are quarantined.";
//                mailJobContext.writeToConsole(msg + Constants.LINE_BREAK);    
            } else {
                String mailLabel = (mailJobContext.getNoOfMailsTobeSent() > 1) ? " mails" : " mail";
                String msg = mailJobContext.getNoOfMailsTobeSent() + mailLabel + " would have been sent if not dry run was specified. Use --mail to actual send the emails.";
                mailJobContext.writeToConsole(msg + Constants.LINE_BREAK);    
            }
        }
        
        // -- write finished message to log and console
        String template = "Mail sending finished. Sent Mails: @noOfSentMails@, Sent Entries: @noOfSentLogEntries@, " + 
            "Quarantined Entries: @noOfQuarantineLogEntries@, Filtered Entries: @noOfFilteredLines@, ID: @mailJobId@, RTIR Parent ID: @rtirParentId@, Duration: @duration@";
        String msg = createFinishedMessage(template);
        mailJobContext.writeToConsole(msg);
        log.info(msg);
        
        // -- write finished message to RSS
        if (!(props.isMailDryRun() || props.isMailDryRun2())) {
            template = "Sent Mails: @noOfSentMails@<br>Sent Entries: @noOfSentLogEntries@<br>" + 
                "Quarantined Entries: @noOfQuarantineLogEntries@<br>Filtered Entries: @noOfFilteredLines@<br>ID: @mailJobId@<br>RTIR Parent ID: @rtirParentId@<br>Duration: @duration@";
            String titleTemplate = "Mail Job: @jobName@. Sent Mails: @noOfSentMails@";
            String title = createFinishedMessage(titleTemplate);
            String description = createFinishedMessage(template);
            JobRssFile rssFile = new JobRssFile(props);
            rssFile.addItem(title, description);
        }
    }
    
    
    private String createFinishedMessage(String template) {
        String result = template;
        result = StringUtil.replace(result, "@jobName@", "" + jobName);
        String mailJobId = (mailJob.getId() != null) ? mailJob.getId().toString() : "-"; 
        result = StringUtil.replace(result, "@mailJobId@", "" + mailJobId);
        String rtirParentId = StringUtil.isNullOrEmpty(props.getParentTicketId()) ? "-" : props.getParentTicketId(); 
        result = StringUtil.replace(result, "@rtirParentId@", rtirParentId);
        result = StringUtil.replace(result, "@noOfSentMails@", "" + mailJobContext.getNoOfSentMails());
        result = StringUtil.replace(result, "@noOfSentLogEntries@", "" + mailJobContext.getNoOfSentLogEntries());
        result = StringUtil.replace(result, "@noOfQuarantineLogEntries@", "" + mailJobContext.getNoOfQuarantineLogEntries());
        result = StringUtil.replace(result, "@noOfFilteredLines@", "" + mailJobContext.getNoOfFilteredLines());
        String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - mailJobContext.getStartedTimestamp());
        result = StringUtil.replace(result, "@duration@", durationStr);
        
        return result;
    }

    private void closeTickets() {

        // Will only close tickets if AppProperties.TICKET_HANDLER_RESOLVE_AFTER_SEND is true

        ITicketHandler ticketHandler = null;

        boolean resolveTicketAfterSend = props.getBoolean(AppProperties.TICKET_HANDLER_RESOLVE_AFTER_SEND, false);
        int ticketResolveSleepTime = props.getInt(AppProperties.TICKET_HANDLER_RESOLVE_SLEEP_TIME, 0);


        if (resolveTicketAfterSend) {
            String ticketHandlerClassName = props.getString(AppProperties.TICKET_HANDLER_CLASS, null);
            if (ticketHandlerClassName != null) {
                try {
                    Class<?> ticketHandlerClass = Class.forName(ticketHandlerClassName);
                    ticketHandler = (ITicketHandler)ticketHandlerClass.newInstance();
                } catch (Exception e) {
                    // ClassNotFoundException, InstantiationException, IllegalAccessException
                    String msg = "Class name must be for a Java-class that implements ITicketHandler: " + ticketHandlerClassName;
                    log.error(msg, e);
                }        

                List<String> childTicketIDs = mailJobContext.getCreatedChildTicketIDs();
                String resolvedStatus = props.getString(AppProperties.TICKET_HANDLER_RESOLVED_STATUS, "resolved");
                
                // Sleep for a few second to make sure that all mails has been sent before closing tickets
                try {
                    Thread.sleep(ticketResolveSleepTime);                
                } catch (InterruptedException e) {
                    log.error("Error during ticket closing sleep", e);                    
                }
                for (Iterator<String> iterator = childTicketIDs.iterator(); iterator.hasNext(); ) {
                    String childTicketId = iterator.next();
                    ticketHandler.updateTicketStatus(resolvedStatus, childTicketId);
                }
                String parentTicketId = props.getParentTicketId();
                if (ticketHandler != null && parentTicketId != null) {                
                    // Resolve/close the parent ticket                                    
                    ticketHandler.updateTicketStatus(resolvedStatus, parentTicketId);
                }
            }
        }
    }
}
