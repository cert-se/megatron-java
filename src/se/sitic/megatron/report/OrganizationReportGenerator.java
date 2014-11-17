package se.sitic.megatron.report;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.MailExporter;
import se.sitic.megatron.core.MailJobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TimePeriod;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.db.DbException;
import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.MailJob;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Emails a report as attachment with log entries for a certain period.
 * Generates and emails a report per specified organization. Supports a kill 
 * list of job-types which will not be included in the report.
 * <p>
 * This report is target for organizations with a huge volume of abuse cases,
 * e.g. ISPs and web hotels. Instead of getting one report per data source,
 * they will receive one report every 24 hours that is machine parsable.
 */
public class OrganizationReportGenerator implements IReportGenerator {
    private final Logger log = Logger.getLogger(OrganizationReportGenerator.class);
    // variables in template
    private static final String NO_OF_LOG_ENTRIES = "noOfLogEntries";
    private static final String TIME_PERIOD = "timePeriod";
    
    private TypedProperties props;
    private DbManager dbManager;
    private MailJobContext mailJobContext;

    
    public OrganizationReportGenerator() {
        // empty
    }


    @Override
    public void init() throws MegatronException {
        this.props = AppProperties.getInstance().createTypedPropertiesForCli("report-organization");
        
        dbManager = DbManager.createDbManager(props);
        mailJobContext = createJobContext(dbManager);
    }

    
    @Override
    public void createFiles() throws MegatronException {
        MailExporter mailExporter = null;
        try {
            mailExporter = new MailExporter(mailJobContext);
            int noOfHours = props.getInt(AppProperties.REPORT_ORGANIZATION_NO_OF_HOURS_KEY, 24);
            TimePeriod timePeriod = getTimePeriod(noOfHours);
            String format = props.getString(AppProperties.EXPORT_TIMESTAMP_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss z");
            String timePeriodStr = DateUtil.formatDateTime(format, timePeriod.getStartDate()) + " - " + DateUtil.formatDateTime(format, timePeriod.getEndDate());
            List<String> jobTypes = Arrays.asList(props.getStringList(AppProperties.REPORT_ORGANIZATION_JOB_TYPES_KEY, new String[0]));
            String[] orgIds = props.getStringList(AppProperties.REPORT_ORGANIZATION_RECIPIENTS_KEY, new String[0]);
            for (int i = 0; i < orgIds.length; i++) {
                // -- fetch organization
                int orgId = 0;
                try {
                    orgId = Integer.parseInt(orgIds[i]);
                } catch (NumberFormatException e) {
                    log.error("Cannot parse organization id from property '" + AppProperties.REPORT_ORGANIZATION_RECIPIENTS_KEY + "': " + orgIds[i]);
                    continue;
                }
                log.debug("Generating organization report for orgId: " + orgId);
                Organization organization = null;
                try {
                    organization = dbManager.getOrganization(orgId);
                } catch(DbException e) {
                    log.error("Cannot fetch organization: " + orgId, e);
                }
                if ((organization == null) || !organization.isEnabled()) {
                    log.error("Organization not found or disabled; skipping report for orgId: " + orgId);
                    continue;
                }
                // -- fetch log entries
                long t1 = System.currentTimeMillis();
                List<LogEntry> logEntries = fetchLogEntries(dbManager, orgId, timePeriod, jobTypes);
                String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - t1);
                if ((logEntries == null) || (logEntries.size() == 0)) {
                    log.info("Empty result set for report; skipping report for orgId: " + orgId);
                    continue;
                }
                log.debug(logEntries.size() + " log entries fetched. Time: " + durationStr);
                // -- add attributes that can be used in template
                mailExporter.addAdditionalGlobalAttribute(TIME_PERIOD, timePeriodStr);
                mailExporter.addAdditionalGlobalAttribute(NO_OF_LOG_ENTRIES, logEntries.size() + "");
                // -- send mail
                mailExporter.sendMail(logEntries);
            }
            // Skip summary mail; not a valid MailJob (could fake one).
            // mailExporter.sendSummaryMail();
        } finally {
            try { if (mailExporter != null) mailExporter.close(); } catch (Exception ignored) {}
            try { if (dbManager != null) dbManager.close(); } catch (Exception ignored) {}
        }
    
        writeFinishedMessage();
    }

    
    private TimePeriod getTimePeriod(int noOfHours) throws MegatronException {
        // DEBUG
//        try {
//            Date startDate = DateUtil.parseDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, "2013-01-01 00:00:00");
//            Date endDate = DateUtil.parseDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, "2014-01-01 23:59:59");
//            return new TimePeriod(startDate, endDate);
//        } catch (ParseException e) {
//            throw new MegatronException("Cannot parse date.", e);
//        }
        
        DateTime now = new DateTime();
        DateTime endDateTime = now.hourOfDay().roundFloorCopy();
        DateTime startDateTime = endDateTime.minusHours(noOfHours);
        // set end time to 23:59:59
        Date endDate = endDateTime.toDate();
        endDate.setTime(endDate.getTime() - 1000L);
        return new TimePeriod(startDateTime.toDate(), endDate);
    }
    
    
    private List<LogEntry> fetchLogEntries(DbManager dbManager, int orgId, TimePeriod timePeriod, List<String> jobTypes) throws MegatronException {
        return dbManager.fetchLogEntriesForOrganizationReport(orgId, timePeriod.getStartDate(), timePeriod.getEndDate(), jobTypes);
    }


    private MailJobContext createJobContext(DbManager dbManager) {
        MailJob mailJob = new MailJob();
        Job job = new Job(0L, "", "", "", 0L, System.currentTimeMillis());        
        mailJob.setJob(job);
        mailJob.setUsePrimaryOrg(true);
        mailJob.setStarted(SqlUtil.convertTimestampToSec(System.currentTimeMillis()));

        MailJobContext result = new MailJobContext(props, mailJob); 
        result.setStartedTimestamp(System.currentTimeMillis());
        result.setDbManager(dbManager);

        return result;
    }
    
    
    private void writeFinishedMessage() {
        // -- write dry-run info
        if (props.isMailDryRun() || props.isMailDryRun2()) {
            String mailLabel = (mailJobContext.getNoOfMailsTobeSent() > 1) ? " mails" : " mail";
            String msg = mailJobContext.getNoOfMailsTobeSent() + mailLabel + " would have been sent if not dry run was specified. Use --mail to actual send the emails.";
            mailJobContext.writeToConsole(msg + Constants.LINE_BREAK);    
        }
        
        // -- write finished message to log and console
        String template = "Mail sending finished. Sent Mails: @noOfSentMails@, Sent Entries: @noOfSentLogEntries@, " + "RTIR Parent ID: @rtirParentId@, Duration: @duration@";
        String msg = createFinishedMessage(template);
        mailJobContext.writeToConsole(msg);
        log.info(msg);
    }
    
    
    private String createFinishedMessage(String template) {
        String result = template;
        String rtirId = StringUtil.isNullOrEmpty(props.getParentTicketId()) ? "-" : props.getParentTicketId(); 
        result = StringUtil.replace(result, "@rtirParentId@", rtirId);
        result = StringUtil.replace(result, "@noOfSentMails@", "" + mailJobContext.getNoOfSentMails());
        result = StringUtil.replace(result, "@noOfSentLogEntries@", "" + mailJobContext.getNoOfSentLogEntries());
        String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - mailJobContext.getStartedTimestamp());
        result = StringUtil.replace(result, "@duration@", durationStr);
        
        return result;
    }

}
