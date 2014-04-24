package se.sitic.megatron.core;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.util.AppUtil;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Fetches data about log jobs and writes the info to stdout or to a string.
 * Handles output for the "--list-jobs" switch.
 */
public class JobListWriter {
    private final Logger log = Logger.getLogger(JobListWriter.class);
    private static final int MAX_NO_OF_LOG_JOBS = 1000;
    private static final int MAX_NO_OF_LOG_ENTRIES = 2000;
    private static final String ROW_SUCCESS_TEMPLATE = "$jobName#padRight54 High Priority Entries: $highPriorityEntries#padRight5 Mail Sent: $mailSent";
    private static final String ROW_FAILED_TEMPLATE = "$jobName#padRight54 Job pending or failed.";
    
    private TypedProperties props; 
    private boolean toStdout;
    private int prio;
    private StringBuilder content;
    private Map<String, TypedProperties> jobPropsCache;
    
    
    public JobListWriter(TypedProperties globalProps, boolean toStdout) {
        this.props = globalProps;
        this.toStdout = toStdout;
        if (!toStdout) {
            this.content = new StringBuilder();
        }
    }

    
    public void execute() throws MegatronException {
        init();
        
        DbManager dbManager = null;
        try {
            long t1 = System.currentTimeMillis();
            dbManager = DbManager.createDbManager(props);
            
            // fetch log jobs
            int days = props.getDaysInListJobs();
            Date startTime = new DateTime().minusDays(days - 1).toDateMidnight().toDate();
            Date endTime = new Date();
            List<Job> jobs = dbManager.searchLogJobs(startTime, endTime, 0, MAX_NO_OF_LOG_JOBS);
            
            // write header
            String startTimeStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, startTime);
            String endTimeStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, endTime);
            write("Jobs created between " + startTimeStr + "--" + endTimeStr + " with priority " + prio + " or higher:" + Constants.LINE_BREAK);
            
            // max reached?
            if (jobs.size() >= MAX_NO_OF_LOG_JOBS) {
                log.warn("Max number of log jobs reached. Writes first " + MAX_NO_OF_LOG_JOBS + " jobs.");
            }

            // process jobs
            for (Iterator<Job> iterator = jobs.iterator(); iterator.hasNext(); ) {
                processJob(dbManager, iterator.next());
            }
            write(Constants.LINE_BREAK);

            String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - t1);
            log.debug("Duration for listing jobs: " + durationStr);
        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
    }

    
    public String getContent() {
        return this.content.toString();
    }
    
    
    private void processJob(DbManager dbManager, Job job) throws MegatronException {
        // fetch high priority entries
        log.debug("Fetching high priority log entries.");
        List<LogEntry> logEntries = dbManager.listLogEntries(job.getId(), prio, 0, MAX_NO_OF_LOG_ENTRIES);
        if (logEntries.size() == 0) {
            log.debug("Skipping job: " + job.getName() + ". Contains no high priority log entries.");
            return;
        }
        int highPriorityEntries = logEntries.size();  
        String highPriorityEntriesStr = highPriorityEntries + ""; 
        if (highPriorityEntries >= MAX_NO_OF_LOG_ENTRIES) {
            highPriorityEntriesStr = highPriorityEntriesStr + "+";
        }
        
        // props for job
        String jobType = job.getJobType().getName();
        if (Constants.DEFAULT_JOB_TYPE.equals(jobType)) {
            // use job type in job name if job type not specified in db  
            String[] headTail = StringUtil.splitHeadTail(job.getName(), "_", false);
            jobType = headTail[0];
        }
        TypedProperties jobProps = jobPropsCache.get(jobType);
        if (jobProps == null) {
            log.debug("Loading properties for job-type: " + jobType);
            try {
                jobProps = AppProperties.getInstance().createTypedPropertiesForCli(jobType);
                jobPropsCache.put(jobType, jobProps);
            } catch (MegatronException e) {
                log.warn("Cannot load properties for job-type: " + jobType, e);
                jobProps = props;
            }
        }
        int quarantinePeriodInSecs = jobProps.getInt(AppProperties.MAIL_IP_QUARANTINE_PERIOD_KEY, 7*24*60*60);
        
        // mailSent
        String mailSent = null;
        if (dbManager.searchMailJobs(job).size() > 0) {
            mailSent = "Yes";
        } else if (isAllQuarantined(dbManager, logEntries, quarantinePeriodInSecs)) {
            mailSent = "All Quarantined";
        } else {
            mailSent = "No";
        }
        
        // building row
        log.debug("Building job list row.");
        Map<String, String> attrMap = new HashMap<String, String>();
        attrMap.put("jobName", job.getName());
        attrMap.put("highPriorityEntries", highPriorityEntriesStr);
        attrMap.put("mailSent", mailSent);
        String template = ((job.getFinished() != null) && (job.getFinished() != 0L) && (job.getErrorMsg() == null)) ? ROW_SUCCESS_TEMPLATE : ROW_FAILED_TEMPLATE; 
        String row = AppUtil.replaceVariables(template, attrMap, false, "ROW_TEMPLATE (hard coded)");
        write(row);
    }


    private boolean isAllQuarantined(DbManager dbManager, List<LogEntry> logEntries, int quarantinePeriodInSecs) throws MegatronException {
        if (quarantinePeriodInSecs == 0) {
            log.debug("Skip quarantine checking due to property: " + AppProperties.MAIL_IP_QUARANTINE_PERIOD_KEY);
            return false;
        }
        log.debug("Checking if all log entries are quarantined.");

        boolean result = true;
        Set<Long> existsMailForIpCache = new HashSet<Long>();
        for (Iterator<LogEntry> iterator = logEntries.iterator(); result && iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            if (!props.isUseOrg2()) {
                Organization org1 = logEntry.getOrganization();
                if ((org1 != null) && (org1.getPriority().getPrio() >= prio) && (logEntry.getIpAddress() != null) && !existsMailForIpCache.contains(logEntry.getIpAddress())) {
                    result = dbManager.existsMailForIp(logEntry.getIpAddress(), org1, quarantinePeriodInSecs, true);
                    if (result) {
                        existsMailForIpCache.add(logEntry.getIpAddress());
                    }
                }
            } else {
                Organization org2 = logEntry.getOrganization2();
                if ((org2 != null) && (org2.getPriority().getPrio() >= prio) && (logEntry.getIpAddress2() != null) && !existsMailForIpCache.contains(logEntry.getIpAddress2())) {
                    result = dbManager.existsMailForIp(logEntry.getIpAddress2(), org2, quarantinePeriodInSecs, false);
                    if (result) {
                        existsMailForIpCache.add(logEntry.getIpAddress2());
                    }
                }
            }
        }
        
        return result;
    }
    
    
    private void init() throws MegatronException {
        // prio
        String prioStr = props.getPrio();
        if (StringUtil.isNullOrEmpty(prioStr)) {
            prioStr = props.getString(AppProperties.FILTER_PRIORITY_INCLUDE_INTERVALS_KEY, null);
        }
        if ((prioStr == null) || prioStr.contains(",") || (prioStr.contains("-") && !prioStr.endsWith("-"))) {
            throw new MegatronException("Interval list in priority not supported by --list-jobs. Use a simple value, e.g. --prio 45.");
        }
        try {
            if (prioStr.endsWith("-")) {
                prioStr = prioStr.substring(0, prioStr.length() - 1); 
            }
            this.prio = Integer.parseInt(prioStr);    
        } catch (NumberFormatException e) {
            throw new MegatronException("Invalid prio value: " + prioStr);
        }
        jobPropsCache = new HashMap<String, TypedProperties>();
    }

    
    private void write(String line) {
        if (toStdout) {
            System.out.println(line);
        } else {
            content.append(line).append(Constants.LINE_BREAK);
        }
    }
    
}
