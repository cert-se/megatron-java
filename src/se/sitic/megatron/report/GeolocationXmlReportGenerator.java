package se.sitic.megatron.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.FileExporter;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TimePeriod;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.decorator.DecoratorManager;
import se.sitic.megatron.decorator.GeolocationDecorator;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.filter.CountryCodeFilter;
import se.sitic.megatron.filter.LogEntryFilterManager;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Creates XML files with geolocation data. Can be used in JavaScript or
 * Flash map charts. The following XML files are generated:<ul>
 * <li>megatron-geolocation-entries.xml: List of machines with geolocation data.
 * This report is for public usage, i.e. all data are anonymous.</li> 
 * <li>megatron-geolocation-entries-internal.xml: As above, but for internal use 
 * (contains e.g. IP addresses).</li>
 * <li>megatron-geolocation-city.xml: Shows bad hosts per city (top list).</li>
 * <li>megatron-geolocation-organization.xml: Shows bad hosts per organization type.</li>
 * </ul>
 */
public class GeolocationXmlReportGenerator implements IReportGenerator {
    private final Logger log = Logger.getLogger(GeolocationXmlReportGenerator.class);

    private static final String PRIO_NAME_KEY = "prioName";
    private static final String CITY_KEY = "city";
    private static final String TIMES_SEEN_KEY = "timesSeen";
    private static final String TOTAL_NO_OF_BAD_HOSTS_KEY = "totalNoOfBadHosts";
    private static final String UNIQUE_NO_OF_BAD_HOSTS_KEY = "uniqueNoOfBadHosts";
    
    private static final String HEADER_REPORT_STARTED_KEY = "header_reportStarted";
    private static final String HEADER_START_DATE_KEY = "header_startDate";
    private static final String HEADER_END_DATE_KEY = "header_endDate";
    private static final String HEADER_TIME_PERIOD_LABEL_KEY = "header_timePeriodLabel";
    private static final String HEADER_NO_OF_BAD_HOSTS_WITH_GEOLOCATION_KEY = "header_noOfBadHostsWithGeolocation";
    private static final String HEADER_NO_OF_BAD_HOSTS_WITHOUT_GEOLOCATION_KEY = "header_noOfBadHostsWithoutGeolocation";
    private static final String HEADER_NO_OF_BAD_HOSTS_WITH_ORGANIZATION_KEY = "header_noOfBadHostsWithOrganization";
    private static final String HEADER_NO_OF_BAD_HOSTS_WITHOUT_ORGANIZATION_KEY = "header_noOfBadHostsWithoutOrganization";
    
    private static final String MISSING_VALUE = "-";
    
    private TypedProperties props;
    private Map<String, String> headerMap;
    private Long dummyId = 0L;

    
    public GeolocationXmlReportGenerator() {
        // empty
    }
    

    public void init() throws MegatronException {
        this.props = AppProperties.getInstance().createTypedPropertiesForCli("report-geolocation");
    }
    
    
    public void createFiles() throws MegatronException {
        DbManager dbManager = null;
        try {
            dbManager = DbManager.createDbManager(props);
            int noOfWeeks = props.getInt(AppProperties.REPORT_GEOLOCATION_NO_OF_WEEKS_KEY, 4);
            TimePeriod timePeriod = getTimePeriod(noOfWeeks);
            String[] jobTypeKillList = props.getStringListFromCommaSeparatedValue(AppProperties.REPORT_GEOLOCATION_JOB_TYPE_KILL_LIST_KEY, new String[0], true);

            log.info("Creating XML files with geolocation data. Period: " + timePeriod);
            List<LogEntry> logEntries = dbManager.fetchLogEntriesForGeolocation(timePeriod.getStartDate(), timePeriod.getEndDate(), Arrays.asList(jobTypeKillList));
            log.debug("No. of log entries for period (before country filter): " + logEntries.size());
            logEntries = filterAndDecorateLogEntries(logEntries);
            log.debug("No. of log entries for period (after country filter): " + logEntries.size());
            convertTimeStamps(logEntries);
            this.headerMap = createHeaderMap(timePeriod);
            this.headerMap.putAll(createTallySummary(logEntries));
            List<LogEntry> organizationSummaryData = createOrganizationSummaryData(logEntries);
            List<LogEntry> citySummaryData = createCitySummaryData(logEntries);
            logEntries = filterEmptyCityLogEntries(logEntries);
            log.debug("No. of log entries for period (after 'empty city'-filter): " + logEntries.size());
            
            Map<String, String> additionalProps = new HashMap<String, String>();
            props.addAdditionalProps(additionalProps);
            additionalProps.put(AppProperties.OUTPUT_DIR_KEY, props.getString(AppProperties.REPORT_OUTPUT_DIR_KEY, null));
            additionalProps.put(AppProperties.EXPORT_TEMPLATE_DIR_KEY, props.getString(AppProperties.REPORT_TEMPLATE_DIR_KEY, null));

            additionalProps.put(AppProperties.EXPORT_HEADER_FILE_KEY, "geolocation-entries_header.xml");
            additionalProps.put(AppProperties.EXPORT_ROW_FILE_KEY, "geolocation-entries_row.xml");
            additionalProps.put(AppProperties.EXPORT_FOOTER_FILE_KEY, "geolocation-entries_footer.xml");
            createEntriesFile("megatron-geolocation-entries", logEntries);
            
            boolean generateInternalReport = props.getBoolean(AppProperties.REPORT_GEOLOCATION_GENERATE_INTERNAL_REPORT_KEY, false);
            if (generateInternalReport) {
                additionalProps.put(AppProperties.EXPORT_HEADER_FILE_KEY, "geolocation-entries-internal_header.xml");
                additionalProps.put(AppProperties.EXPORT_ROW_FILE_KEY, "geolocation-entries-internal_row.xml");
                additionalProps.put(AppProperties.EXPORT_FOOTER_FILE_KEY, "geolocation-entries-internal_footer.xml");
                createEntriesFile("megatron-geolocation-entries-internal", logEntries);
            }
            
            additionalProps.put(AppProperties.EXPORT_HEADER_FILE_KEY, "geolocation-organization_header.xml");
            additionalProps.put(AppProperties.EXPORT_ROW_FILE_KEY, "geolocation-organization_row.xml");
            additionalProps.put(AppProperties.EXPORT_FOOTER_FILE_KEY, "geolocation-organization_footer.xml");
            createEntriesFile("megatron-geolocation-organization", organizationSummaryData);

            additionalProps.put(AppProperties.EXPORT_HEADER_FILE_KEY, "geolocation-city_header.xml");
            additionalProps.put(AppProperties.EXPORT_ROW_FILE_KEY, "geolocation-city_row.xml");
            additionalProps.put(AppProperties.EXPORT_FOOTER_FILE_KEY, "geolocation-city_footer.xml");
            createEntriesFile("megatron-geolocation-city", citySummaryData);
        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
    }

    
    private TimePeriod getTimePeriod(int noOfWeeks) throws MegatronException {
        // DEBUG
//        try {
//            Date startDate = DateUtil.parseDateTime(DateUtil.DATE_FORMAT, "2011-09-29");
//            Date endDate = DateUtil.parseDateTime(DateUtil.DATE_FORMAT, "2011-09-30");
//            return new TimePeriod(startDate, endDate);
//        } catch (ParseException e) {
//            throw new MegatronException("Cannot parse date.", e);
//        }
        
        DateTime startDateTime = startDateTime(noOfWeeks);
        DateTime endDateTime = startDateTime.plusDays(7*noOfWeeks);
        Date startDate = startDateTime.toDateMidnight().toDate();
        Date endDate = endDateTime.toDateMidnight().toDate();
        // set end time to 23:59:59
        endDate.setTime(endDate.getTime() - 1000L);
        return new TimePeriod(startDate, endDate);
    }

    
    private DateTime startDateTime(int noOfWeeks) {
        // find first monday 
        DateTime result = new DateTime();
        while (true) {
            if (result.getDayOfWeek() == DateTimeConstants.MONDAY) {
                break;
            }
            result = result.minusDays(1);
        }
        return result.minusDays(7*noOfWeeks);
    }

    
    private List<LogEntry> filterAndDecorateLogEntries(List<LogEntry> logEntries) throws MegatronException {
        List<LogEntry> result = new ArrayList<LogEntry>(logEntries.size());
        
        JobContext jobContext = new JobContext(props, new Job());

        LogEntryFilterManager filterManager = new LogEntryFilterManager(jobContext);
        String[] filterClassNames = { CountryCodeFilter.class.getName() };
        filterManager.init(filterClassNames);
        
        DecoratorManager decoratorManager = new DecoratorManager(jobContext);
        String[] decoratorClassNames = { GeolocationDecorator.class.getName() };
        decoratorManager.init(decoratorClassNames);

        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            if (!filterManager.executeFilters(logEntry)) {
                continue;
            }
            decoratorManager.executeDecorators(logEntry);
            result.add(logEntry);
        }

        return result;
    }

    
    private void convertTimeStamps(List<LogEntry> logEntries) {
        String format = props.getString(AppProperties.EXPORT_TIMESTAMP_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss z");
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            String lastSeenInSec = logEntry.getAdditionalItems().get("lastSeen");
            String lastSeenStr = DateUtil.formatDateTime(format, SqlUtil.convertTimestamp(Long.parseLong(lastSeenInSec)));
            logEntry.getAdditionalItems().put("lastSeen", lastSeenStr);
        }
    }
    
    
    private Map<String, String> createHeaderMap(TimePeriod timePeriod) {
        String startDateStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, timePeriod.getStartDate());
        String endDateStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, timePeriod.getEndDate());
        String timePeriodLabel = startDateStr + " - " + endDateStr;

        Map<String, String> result = new HashMap<String, String>();
        result.put(HEADER_REPORT_STARTED_KEY, DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, new Date()));
        result.put(HEADER_START_DATE_KEY, startDateStr);
        result.put(HEADER_END_DATE_KEY, endDateStr);
        result.put(HEADER_TIME_PERIOD_LABEL_KEY, timePeriodLabel);
        
        return result;
    }

    
    private Map<String, String> createTallySummary(List<LogEntry> logEntries) {
        Map<String, String> result = new HashMap<String, String>();
        long noOfBadHostsWithGeolocation = 0L;
        long noOfBadHostsWithoutGeolocation = 0L;
        long noOfBadHostsWithOrganization = 0L;
        long noOfBadHostsWithoutOrganization = 0L; 
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            if (StringUtil.isNullOrEmpty(logEntry.getAdditionalItems().get(CITY_KEY))) {
                ++noOfBadHostsWithoutGeolocation;
            } else {
                ++noOfBadHostsWithGeolocation;
            }
            if (StringUtil.isNullOrEmpty(logEntry.getAdditionalItems().get(PRIO_NAME_KEY))) {
                ++noOfBadHostsWithoutOrganization;
            } else {
                ++noOfBadHostsWithOrganization;
            }
        }
        
        result.put(HEADER_NO_OF_BAD_HOSTS_WITH_GEOLOCATION_KEY, Long.toString(noOfBadHostsWithGeolocation));
        result.put(HEADER_NO_OF_BAD_HOSTS_WITHOUT_GEOLOCATION_KEY, Long.toString(noOfBadHostsWithoutGeolocation));
        result.put(HEADER_NO_OF_BAD_HOSTS_WITH_ORGANIZATION_KEY, Long.toString(noOfBadHostsWithOrganization));
        result.put(HEADER_NO_OF_BAD_HOSTS_WITHOUT_ORGANIZATION_KEY, Long.toString(noOfBadHostsWithoutOrganization));
        return result;
    }
    
    
    private LogEntry createDummyLogEntry() {
        LogEntry result = new LogEntry(dummyId++);
        result.setCreated(System.currentTimeMillis());
        result.setLogTimestamp(System.currentTimeMillis());
        return result;
    }

    
    /**
     * Creates data for the organization summary file ("Bad hosts per organization type").
     * 
     * @return list of LogEntrys, which means FileExporter.writeLogEntry can be used.
     */
    private List<LogEntry> createOrganizationSummaryData(List<LogEntry> logEntries) {
        Map<String, LogEntry> orgMap = new HashMap<String, LogEntry>();

        List<String> killList = Arrays.asList(props.getStringList(AppProperties.REPORT_GEOLOCATION_ORGANIZATION_TYPE_KILL_LIST_KEY, new String[0]));
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            String orgType = logEntry.getAdditionalItems().get(PRIO_NAME_KEY);
            if (StringUtil.isNullOrEmpty(orgType)) {
                orgType = MISSING_VALUE;
            }
            if (killList.contains(orgType)) {
                continue;
            }
            LogEntry mapValue = orgMap.get(orgType);
            if (mapValue == null) {
                mapValue = createDummyLogEntry();
                orgMap.put(orgType, mapValue);
                if (mapValue.getAdditionalItems() == null) {
                    mapValue.setAdditionalItems(new HashMap<String, String>());
                }
                mapValue.getAdditionalItems().put(UNIQUE_NO_OF_BAD_HOSTS_KEY, "0");
                mapValue.getAdditionalItems().put(TOTAL_NO_OF_BAD_HOSTS_KEY, "0");
                mapValue.getAdditionalItems().put(PRIO_NAME_KEY, orgType);
            }
            int badHosts = 1 + Integer.parseInt(mapValue.getAdditionalItems().get(UNIQUE_NO_OF_BAD_HOSTS_KEY));
            mapValue.getAdditionalItems().put(UNIQUE_NO_OF_BAD_HOSTS_KEY, Integer.toString(badHosts));
            int timesSeen = Integer.parseInt(logEntry.getAdditionalItems().get(TIMES_SEEN_KEY));
            badHosts = timesSeen + Integer.parseInt(mapValue.getAdditionalItems().get(TOTAL_NO_OF_BAD_HOSTS_KEY));
            mapValue.getAdditionalItems().put(TOTAL_NO_OF_BAD_HOSTS_KEY, Integer.toString(badHosts));
        }
        List<LogEntry> result = new ArrayList<LogEntry>(orgMap.values());
        Collections.sort(result, new Comparator<LogEntry>() {
                public int compare(LogEntry o1, LogEntry o2) {
                    int badHosts1 = Integer.parseInt(o1.getAdditionalItems().get(TOTAL_NO_OF_BAD_HOSTS_KEY));
                    int badHosts2 = Integer.parseInt(o2.getAdditionalItems().get(TOTAL_NO_OF_BAD_HOSTS_KEY));
                    return (badHosts1 < badHosts2) ? -1 : (badHosts1 == badHosts2 ? 0 : 1);
                }
            });
        long rowId = 0;
        for (Iterator<LogEntry> iterator = result.iterator(); iterator.hasNext(); ) {
            iterator.next().setId(rowId++);
        }
        
        return result;
    }

    
    /**
     * Creates data for the city summary file ("Bad hosts per city (top list)").
     * 
     * @return list of LogEntrys, which means FileExporter.writeLogEntry can be used.
     */
    private List<LogEntry> createCitySummaryData(List<LogEntry> logEntries) {
        Map<String, LogEntry> cityMap = new HashMap<String, LogEntry>();

        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            String city = logEntry.getAdditionalItems().get(CITY_KEY);
            if (StringUtil.isNullOrEmpty(city)) {
                city = MISSING_VALUE;
            }
            LogEntry mapValue = cityMap.get(city);
            if (mapValue == null) {
                mapValue = createDummyLogEntry();
                cityMap.put(city, mapValue);
                if (mapValue.getAdditionalItems() == null) {
                    mapValue.setAdditionalItems(new HashMap<String, String>());
                }
                mapValue.getAdditionalItems().put(UNIQUE_NO_OF_BAD_HOSTS_KEY, "0");
                mapValue.getAdditionalItems().put(TOTAL_NO_OF_BAD_HOSTS_KEY, "0");
                mapValue.getAdditionalItems().put(CITY_KEY, city);
            }
            int badHosts = 1 + Integer.parseInt(mapValue.getAdditionalItems().get(UNIQUE_NO_OF_BAD_HOSTS_KEY));
            mapValue.getAdditionalItems().put(UNIQUE_NO_OF_BAD_HOSTS_KEY, Integer.toString(badHosts));
            int timesSeen = Integer.parseInt(logEntry.getAdditionalItems().get(TIMES_SEEN_KEY));
            badHosts = timesSeen + Integer.parseInt(mapValue.getAdditionalItems().get(TOTAL_NO_OF_BAD_HOSTS_KEY));
            mapValue.getAdditionalItems().put(TOTAL_NO_OF_BAD_HOSTS_KEY, Integer.toString(badHosts));
        }
        List<LogEntry> result = new ArrayList<LogEntry>(cityMap.values());
        Collections.sort(result, new Comparator<LogEntry>() {
                public int compare(LogEntry o1, LogEntry o2) {
                    int badHosts1 = Integer.parseInt(o1.getAdditionalItems().get(TOTAL_NO_OF_BAD_HOSTS_KEY));
                    int badHosts2 = Integer.parseInt(o2.getAdditionalItems().get(TOTAL_NO_OF_BAD_HOSTS_KEY));
                    return -1*((badHosts1 < badHosts2) ? -1 : (badHosts1 == badHosts2 ? 0 : 1));
                }
            });
        long rowId = 0;
        for (Iterator<LogEntry> iterator = result.iterator(); iterator.hasNext(); ) {
            iterator.next().setId(rowId++);
        }
        int noOfEntries = props.getInt(AppProperties.REPORT_GEOLOCATION_NO_OF_ENTRIES_IN_CITY_REPORT_KEY, 20);
        if (result.size() > noOfEntries) {
            result = result.subList(0, noOfEntries);
        }
        
        return result;
    }


    private List<LogEntry> filterEmptyCityLogEntries(List<LogEntry> logEntries) {
        List<LogEntry> result = new ArrayList<LogEntry>(logEntries.size());
        long id = 0L;
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            if (!StringUtil.isNullOrEmpty(logEntry.getAdditionalItems().get(CITY_KEY))) {
                logEntry.setId(id++);
                result.add(logEntry);
            }
        }
        return result;
    }

    
    private void createEntriesFile(String filename, List<LogEntry> logEntries) throws MegatronException {
        Job job = new Job(0L, filename, filename, "", 0L, System.currentTimeMillis());
        JobContext jobContext = new JobContext(props, job);
        FileExporter fileExporter = new FileExporter(jobContext);
        fileExporter.setHeaderMap(headerMap);
        fileExporter.writeHeader(job);
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            fileExporter.writeLogEntry(logEntry);
        }
        fileExporter.writeFooter(job);
        fileExporter.close();
        fileExporter.writeFinishedMessageToLog();
    }
    
}
