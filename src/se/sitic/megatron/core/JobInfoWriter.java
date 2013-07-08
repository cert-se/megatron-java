package se.sitic.megatron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Days;

import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.MailJob;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.entity.Priority;
import se.sitic.megatron.util.AppUtil;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.IpAddressUtil;
import se.sitic.megatron.util.ObjectStringSorter;
import se.sitic.megatron.util.StringUtil;


/**
 * Fetches data about a log job and writes the info to stdout or to a string.
 * Handles output for the "--job-info" switch.
 */
public class JobInfoWriter {
    private final Logger log = Logger.getLogger(JobInfoWriter.class);
    private static final int MAX_NO_OF_LOG_ENTRIES = 2000;
    private static final String INIT_PRIO_FAILED_MSG = "Interval list in priority not supported for job info. Use a simple value, e.g. --prio 45.";
    private static final String MORE_LOG_ENTRIES_EXISTS_MSG = "Note: Not all log entries listed. Number of log entries exceeded threshold.";
    
    private static final String SECTION_HEADER_TEMPLATE  = "==== $organizationName ($prio) ====";
    private static final String SECTION_ROW_TEMPLATE = "$ipAddress#padRight15 $hostname#padRight35 Last Seen: $lastSeen#padRight25 Last Emailed: $lastEmailed#padRight25 $org2";
    private static final String SECTION_FOOTER_TEMPLATE  = "To: $toAddresses";
    
    private TypedProperties props; 
    private boolean toStdout;
    private DbManager dbManager;
    private Job job;
    private boolean useHighPriorityThreshold;
    private int prio;
    private int quarantinePeriodInSecs; 
    private boolean initPrioFailed = false;
    private Map<Organization, List<LogEntry>> organizationMap;
    private Map<String, String> attrMap;
    private Set<LogEntry> quarantinedLogEntries;
    private StringBuilder rows;
    private boolean moreLogEntriesExists;
    private int noOfIps;
    private int noOfQuarantinedIps;

    
    /**
     * Creates a writer that writes to stdout as soon as data are available. 
     */
    public JobInfoWriter(TypedProperties globalProps) {
        this.props = globalProps;
        this.toStdout = true;
        useHighPriorityThreshold = false;
    }

    
    /**
     * Creates a writer that stores data in a map, which is used in
     * createContent to substitute variables in a template. Use this
     * method e.g. to create a notification mail. 
     */
    public JobInfoWriter(TypedProperties props, DbManager dbManager, Job job) {
        this.props = props;
        this.dbManager = dbManager;
        this.job = job;
        this.toStdout = false;
        useHighPriorityThreshold = true;
    }

    
    public void execute() throws MegatronException {
        if (!initPrio()) {
            return;
        }
        
        boolean dbManagerCreated = false;
        try {
            long t1 = System.currentTimeMillis();
            if (dbManager == null) {
                dbManager = DbManager.createDbManager(props);
                dbManagerCreated = true;
            }
            init();
            
            // write header
            write("Job Name: " + job.getName());
            write("Filename: " + job.getFilename() + " (" + job.getFileSize() + " bytes, " + job.getProcessedLines() + " lines)" + Constants.LINE_BREAK);
            write("Organizations with priority " + prio + " or higher:" + Constants.LINE_BREAK);
            
            // fetching log entries and build map
            List<LogEntry> logEntries = dbManager.listLogEntries(job.getId(), prio, 0, MAX_NO_OF_LOG_ENTRIES);
            if (logEntries.size() >= MAX_NO_OF_LOG_ENTRIES) {
                moreLogEntriesExists = true;
            }
            organizationMap = createOrganizationMap(logEntries);
            
            // write rows
            List<Organization> organizations = new ArrayList<Organization>(organizationMap.keySet());
            sortOrganizationsByPriority(organizations);
            for (Iterator<Organization> iterator = organizations.iterator(); iterator.hasNext(); ) {
                Organization organization = iterator.next();
                List<LogEntry> logEntriesForOrg = organizationMap.get(organization);
                sortLogEntriesById(logEntriesForOrg);
                noOfIps += writeSection(organization, logEntriesForOrg);
            }

            write("");
            write("No. of Quarantined IPs: " + noOfQuarantinedIps + " of " + noOfIps);    
            
            if (moreLogEntriesExists) {
                write("");
                write(MORE_LOG_ENTRIES_EXISTS_MSG);
            }
            
            addJobInfo(logEntries);
            
            String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - t1);
            log.debug("Duration for job info: " + durationStr);
        } finally {
            if (dbManagerCreated) {
                dbManager.close();
            }
        }
    }

    
    public String createContent(String template) throws ConversionException {
        if (initPrioFailed) {
            return INIT_PRIO_FAILED_MSG;
        }
        return AppUtil.replaceVariables(template, attrMap, false, "template-parameter in createContent");
    }

    
    private boolean initPrio() throws MegatronException {
        String prioStr = null;
        if (useHighPriorityThreshold) {
            prioStr = props.getString(AppProperties.HIGH_PRIORITY_THRESHOLD_KEY, "40");
        } else {
            prioStr = props.getPrio();
            if (StringUtil.isNullOrEmpty(prioStr)) {
                prioStr = props.getString(AppProperties.FILTER_PRIORITY_INCLUDE_INTERVALS_KEY, null);
            }
        }
        if ((prioStr == null) || prioStr.contains(",") || (prioStr.contains("-") && !prioStr.endsWith("-"))) {
            this.initPrioFailed = true;
            log.warn(INIT_PRIO_FAILED_MSG);
            write(INIT_PRIO_FAILED_MSG);
            return false;
        }
        try {
            if (prioStr.endsWith("-")) {
                prioStr = prioStr.substring(0, prioStr.length() - 1); 
            }
            this.prio = Integer.parseInt(prioStr);    
        } catch (NumberFormatException e) {
            throw new MegatronException("Invalid prio value: " + prioStr);
        }
        return true;
    }

    
    private void init() throws MegatronException {
        attrMap = new HashMap<String, String>();
        quarantinedLogEntries = new HashSet<LogEntry>();
        rows = new StringBuilder(1*1024);;

        if (job == null) {
            String jobName = props.getJob();
            log.debug("Fecthing log job: " + jobName);
            this.job = dbManager.searchLogJob(jobName);
            if (this.job == null) {
                throw new MegatronException("Cannot find job: " + jobName);
            }
        }
        quarantinePeriodInSecs = props.getInt(AppProperties.MAIL_IP_QUARANTINE_PERIOD_KEY, 7*24*60*60);
    }

    
    private Map<Organization, List<LogEntry>> createOrganizationMap(List<LogEntry> logEntries) {
        Map<Organization, List<LogEntry>> result = new HashMap<Organization, List<LogEntry>>();
        
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            List<LogEntry> logEntriesForOrg = null;
            
            Organization organization = logEntry.getOrganization();
            if ((organization != null) && (organization.getPriority().getPrio() >= prio)) {
                logEntriesForOrg = result.get(organization);
                if (logEntriesForOrg == null) {
                    logEntriesForOrg = new ArrayList<LogEntry>();
                    result.put(organization, logEntriesForOrg);
                }
                logEntriesForOrg.add(logEntry);
            }

            Organization organization2 = logEntry.getOrganization2();
            if ((organization2 != null) && (organization2.getPriority().getPrio() >= prio)) {
                logEntriesForOrg = result.get(organization2);
                if (logEntriesForOrg == null) {
                    logEntriesForOrg = new ArrayList<LogEntry>();
                    result.put(organization2, logEntriesForOrg);
                }
                logEntriesForOrg.add(logEntry);
            }
        }
        
        return result;
    }

    
    private void sortOrganizationsByPriority(List<Organization> organizations) {
        ObjectStringSorter sorter = new ObjectStringSorter() {
            @Override
            protected String getObjectString(Object obj) {
                if (obj == null) {
                    return null;
                }
                Integer prio = ((Organization)obj).getPriority().getPrio();
                return (prio != null) ? prio.toString() : null;
            }
        };
        Collections.sort(organizations, sorter);
        Collections.reverse(organizations);
    }

    
    private void sortLogEntriesById(List<LogEntry> logEntries) {
        ObjectStringSorter sorter = new ObjectStringSorter(true, true) {
            @Override
            protected String getObjectString(Object obj) {
                if (obj == null) {
                    return null;
                }
                Long id = ((LogEntry)obj).getId();
                return (id != null) ? id.toString() : null;
            }
        };
        Collections.sort(logEntries, sorter);
    }

    
    private int writeSection(Organization organization, List<LogEntry> logEntries) throws MegatronException {
        int result = 0;
        Map<String, String> sectionAttrMap = new HashMap<String, String>();        
        sectionAttrMap.put("organizationName", organization.getName());
        sectionAttrMap.put("prio", organization.getPriority().getPrio() + "");
        sectionAttrMap.put("toAddresses", organization.getEmailAddresses());

        // header
        String header = AppUtil.replaceVariables(SECTION_HEADER_TEMPLATE, sectionAttrMap, false, "SECTION_HEADER_TEMPLATE (hardcoded)");
        if (rows.length() > 0) {
            write("");
            rows.append(Constants.LINE_BREAK);
        }
        write(header);
        rows.append(header).append(Constants.LINE_BREAK);

        Set<Long> processedIps = new HashSet<Long>();
        Map<String, String> rowAttrMap = new HashMap<String, String>();
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            rowAttrMap.clear();

            // ipAddress, hostname, org2
            Long ipAddress = null;
            String hostname = null;
            String org2 = null;
            Organization logEntryOrg = logEntry.getOrganization();
            if ((logEntryOrg != null) && (logEntryOrg.equals(organization))) {
                ipAddress = logEntry.getIpAddress();
                hostname = logEntry.getHostname();
                org2 = "";
            } else {
                ipAddress = logEntry.getIpAddress2();
                hostname = logEntry.getHostname2();
                org2 = "Org2";
            }
            ipAddress = (ipAddress != null) ? ipAddress : new Long(0);
            // ignore duplicates
            if (processedIps.contains(ipAddress)) {
                continue;
            }
            processedIps.add(ipAddress);
            String ipAddressStr = (ipAddress != 0L) ? IpAddressUtil.convertIpAddress(ipAddress, false) : "[Unknown IP]";
            rowAttrMap.put("ipAddress", ipAddressStr);
            hostname = (hostname != null) ? hostname : "[Unknown Hostname]";
            rowAttrMap.put("hostname", hostname);
            rowAttrMap.put("org2", org2);
            
            // lastSeen
            LogEntry lastSeenLogEntry = dbManager.getLastSeenLogEntry(ipAddress, job.getId());
            String lastSeen = "-";
            if (lastSeenLogEntry != null) {
                lastSeen = formatLastSeenTimestamp(lastSeenLogEntry.getCreated());
            }
            rowAttrMap.put("lastSeen", lastSeen);

            // lastEmailed
            MailJob lastSeenMailJob = dbManager.getLastSeenMailJob(ipAddress);
            String lastEmailed = "-";
            if (lastSeenMailJob != null) {
                lastEmailed = formatLastSeenTimestamp(lastSeenMailJob.getStarted());
            }
            rowAttrMap.put("lastEmailed", lastEmailed);

            // quarantined?
            if (isQuarantined(lastSeenMailJob)) {
                // Some log entries may be shared in the map (organizationMap);
                // same id but both org and org2 is a high priority organization.
                // Thus, a set is used to avoid storing duplicates.
                quarantinedLogEntries.add(logEntry);
                ++noOfQuarantinedIps;
            }
            
            // write row
            String row = AppUtil.replaceVariables(SECTION_ROW_TEMPLATE, rowAttrMap, false, "SECTION_ROW_TEMPLATE (hardcoded)");
            write(row);
            rows.append(row).append(Constants.LINE_BREAK);
            ++result;
        }
        
        // footer
        String footer = AppUtil.replaceVariables(SECTION_FOOTER_TEMPLATE, sectionAttrMap, false, "SECTION_FOOTER_TEMPLATE (hardcoded)");
        write(footer);
        rows.append(footer).append(Constants.LINE_BREAK);
        
        attrMap.put("rows", rows.toString());
        
        return result;
    }
    
    
    private String formatLastSeenTimestamp(Long timestampInSec) {
        if ((timestampInSec == null) || (timestampInSec == 0L)) {
            return "-";
        }
        
        final String template = "@days@ (@date@)";
        String result = template;

        DateTime now = new DateTime();
        DateTime dateTime = new DateTime(1000L*timestampInSec);        
        Days d = Days.daysBetween(dateTime, now);
        int days = d.getDays();
        result = StringUtil.replace(result, "@days@", days + "");

        String date = DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, dateTime.toDate());
        result = StringUtil.replace(result, "@date@", date);
        
        return result;
    }

    
    private void addJobInfo(List<LogEntry> logEntries) {
        attrMap.put("jobName", job.getName());
        attrMap.put("jobFilename", job.getFilename());
        attrMap.put("jobFileSize", job.getFileSize() + "");
        attrMap.put("jobFileHash", job.getFileHash() + "");
        attrMap.put("jobProcessedLines", job.getProcessedLines() + "");
        attrMap.put("noOfHighPriorityEntries", logEntries.size() + "");
        attrMap.put("noOfHighPriorityIps", noOfIps + "");
        attrMap.put("noOfHighPriorityOrganizations", organizationMap.size() + "");
        attrMap.put("noOfQuarantinedEntries", quarantinedLogEntries.size() + "");
        attrMap.put("noOfQuarantinedIps", noOfQuarantinedIps + "");
        
        // logEntries are sorted by priority
        Organization organization = null;
        if (logEntries.size() > 0) {
            organization = logEntries.get(0).getOrganization();
            if (organization == null) {
                organization = logEntries.get(0).getOrganization2();
            }
        }
        Priority priority = null;
        if (organization != null) {
            priority = organization.getPriority();
        }
        String maxPriority = (priority != null) ? priority.getPrio().toString() : "";
        String maxPriorityName = (priority != null) ? priority.getName() : "";
        attrMap.put("maxPriority", maxPriority); 
        attrMap.put("maxPriorityName", maxPriorityName); 
        attrMap.put("priority", prio + "");
        String moreLogEntriesExistsMsg = moreLogEntriesExists ? MORE_LOG_ENTRIES_EXISTS_MSG : ""; 
        attrMap.put("moreLogEntriesExistsMsg", moreLogEntriesExistsMsg);
    }

    
    private boolean isQuarantined(MailJob mailJob) {
        if ((mailJob == null) || (mailJob.getStarted() == null)) {
            return false;
        }

        long noOfSecsAgo = (System.currentTimeMillis() / 1000L) - mailJob.getStarted();
        return (noOfSecsAgo <= quarantinePeriodInSecs);
    }


    private void write(String line) {
        if (toStdout) {
            System.out.println(line);
        }
    }

}
