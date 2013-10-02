package se.sitic.megatron.report;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.FileExporter;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TimePeriod;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.decorator.DecoratorManager;
import se.sitic.megatron.decorator.GeolocationDecorator;
import se.sitic.megatron.decorator.IDecorator;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.NameValuePair;
import se.sitic.megatron.entity.Priority;
import se.sitic.megatron.filter.CountryCodeFilter;
import se.sitic.megatron.filter.LogEntryFilterManager;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.FileUtil;
import se.sitic.megatron.util.IpAddressUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Creates JSON files with geolocation data. Can be used in JavaScript map 
 * charts. The following JSON files are generated:<ul>
 * <li>summary.json: General information about bad hosts for the period.</li> 
 * <li>all-days.json: Overview of all cities and bad hosts per city for the
 * whole time period.</li>
 * <li>2011-11-20.json, 2011-11-21.json, ...: As above but for a specific day.
 * <li>2011-11-20/stockholm.json, ...: All entries for a city, in this case 
 * Stockholm.   
 * <li>city.json: Shows bad hosts per city (top list).</li>
 * <li>organization.json: Shows bad hosts per organization type.</li>
 * <li>summary-all.json: summary.json + city.json + organization.json</li>
 * </ul>
 * Two version of files will be generated (in different directories):<ul>
 * <li>Public: All data are anonymous.</li>
 * <li>Internal: For internal use (contains e.g. IP addresses).
 * </ul>
 */
public class GeolocationJsonReportGenerator implements IReportGenerator {
    private final Logger log = Logger.getLogger(GeolocationJsonReportGenerator.class);

    private static final String PUB_DIR = "mapdata-pub";
    private static final String INTERNAL_DIR = "mapdata-internal";
    
    private static final String PRIO_NAME_KEY = "prioName";
    private static final String CITY_KEY = "city";
    private static final String CITY_SLUG_KEY = "citySlug";
    private static final String TIMES_SEEN_KEY = "timesSeen";
    private static final String LAST_SEEN_KEY = "lastSeen";
    private static final String IP_ADDRESS_MASKED_KEY = "ipAddressMasked";
    private static final String TOTAL_NO_OF_BAD_HOSTS_KEY = "totalNoOfBadHosts";
    private static final String UNIQUE_NO_OF_BAD_HOSTS_KEY = "uniqueNoOfBadHosts";
    
    private static final String HEADER_REPORT_STARTED_KEY = "header_reportStarted";
    private static final String HEADER_START_DATE_KEY = "header_startDate";
    private static final String HEADER_END_DATE_KEY = "header_endDate";
    private static final String HEADER_TIME_PERIOD_LABEL_KEY = "header_timePeriodLabel";
    private static final String HEADER_DAYS_KEY = "header_days";
    private static final String HEADER_DAY_LABELS_KEY = "header_dayLabels";
    private static final String HEADER_NO_OF_BAD_HOSTS_WITH_GEOLOCATION_KEY = "header_noOfBadHostsWithGeolocation";
    private static final String HEADER_NO_OF_BAD_HOSTS_WITHOUT_GEOLOCATION_KEY = "header_noOfBadHostsWithoutGeolocation";
    private static final String HEADER_NO_OF_BAD_HOSTS_WITH_ORGANIZATION_KEY = "header_noOfBadHostsWithOrganization";
    private static final String HEADER_NO_OF_BAD_HOSTS_WITHOUT_ORGANIZATION_KEY = "header_noOfBadHostsWithoutOrganization";
    
    // remove line breaks and trailing comma
    private static final String[][] REPLACE_ARRAY = { { "\n", "" } }; 
    
    private static final String MISSING_VALUE = "-";
    
    private TypedProperties props;
    private Map<String, String> additionalProps;
    private Map<String, String> headerMap;
    private Long dummyId = 0L;

    
    public GeolocationJsonReportGenerator() {
        // empty
    }
    

    @Override
    public void init() throws MegatronException {
        this.props = AppProperties.getInstance().createTypedPropertiesForCli("report-geolocation");
    }
    
    
    @Override
    public void createFiles() throws MegatronException {
        DbManager dbManager = null;
        try {
            dbManager = DbManager.createDbManager(props);
            int noOfWeeks = props.getInt(AppProperties.REPORT_GEOLOCATION_NO_OF_WEEKS_KEY, 4);
            TimePeriod timePeriod = getTimePeriod(noOfWeeks);
            String[] jobTypeKillList = props.getStringListFromCommaSeparatedValue(AppProperties.REPORT_GEOLOCATION_JOB_TYPE_KILL_LIST_KEY, new String[0], true);

            log.info("Creating JSON files with geolocation data. Period: " + timePeriod);
            List<LogEntry> logEntries = dbManager.fetchLogEntriesForGeolocation(timePeriod.getStartDate(), timePeriod.getEndDate(), Arrays.asList(jobTypeKillList));
            log.debug("No. of log entries for period (before country filter): " + logEntries.size());
            logEntries = filterAndDecorateLogEntries(logEntries);
            log.debug("No. of log entries for period (after country filter): " + logEntries.size());
            convertTimeStamps(logEntries);
            this.headerMap = createHeaderMap(timePeriod);
            this.headerMap.putAll(createTallySummary(logEntries));
            logEntries = filterEmptyCityLogEntries(logEntries);
            log.debug("No. of log entries for period (after 'empty city'-filter): " + logEntries.size());

            List<Priority> allPriorities = dbManager.getAllPriorities();
            List<LogEntry> organizationSummaryData = createOrganizationSummaryData(logEntries, allPriorities);
            List<LogEntry> citySummaryData = createCitySummaryData(logEntries);            
            
            additionalProps = new HashMap<String, String>();
            props.addAdditionalProps(additionalProps);
            additionalProps.put(AppProperties.EXPORT_TEMPLATE_DIR_KEY, props.getString(AppProperties.REPORT_TEMPLATE_DIR_KEY, null));

            boolean generateInternalReport = props.getBoolean(AppProperties.REPORT_GEOLOCATION_GENERATE_INTERNAL_REPORT_KEY, false);
            if (generateInternalReport) {
                deleteOutputDir(true);
                createSummaryFile(true);
                createOrganizationFile(organizationSummaryData, true);
                createCityFile(citySummaryData, true);
                createEntriesOverviewFiles(timePeriod, logEntries, true);
                concatenateFiles(true);
            }

            deleteOutputDir(false);
            createSummaryFile(false);
            createOrganizationFile(organizationSummaryData, false);
            createCityFile(citySummaryData, false);
            createEntriesOverviewFiles(timePeriod, logEntries, false);
            concatenateFiles(false);
        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
    }

    
    private TimePeriod getTimePeriod(int noOfWeeks) throws MegatronException {
        // DEBUG
//        try {
//            Date startDate = DateUtil.parseDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, "2011-09-28 00:00:00");
//            Date endDate = DateUtil.parseDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, "2011-09-28 23:59:59");
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
        // day before yesterday (yesterday does not contain so much data and 
        // is excluded, because not all log records have yet been received)
        return new DateTime().minusDays(7*noOfWeeks+1);        
    }

    
    private String getOutputDir(boolean internal) throws MegatronException {
        String reportDir = props.getString(AppProperties.REPORT_OUTPUT_DIR_KEY, null);
        if (reportDir == null) {
            throw new MegatronException("Property missing: " + AppProperties.REPORT_OUTPUT_DIR_KEY);
        }
        
        String dir = internal ? INTERNAL_DIR : PUB_DIR;
        return FileUtil.concatPath(reportDir, dir);
    }

    
    private void deleteOutputDir(boolean internal) throws MegatronException {
        String dirStr = getOutputDir(internal);
        File dir = new File(dirStr);
        log.info("Deleting all files and directories in report directory: " + dir.getAbsolutePath());
        if (!dir.getAbsolutePath().contains(internal ? INTERNAL_DIR : PUB_DIR)) {
            throw new MegatronException("Assertion Error: report directory name not valid.");
        }
        FileUtil.removeDirectory(dir);
    }

    
    private String initOutputDir(boolean internal) throws MegatronException {
        String result = getOutputDir(internal);

        // -- ensure dir 
        try {
            boolean dirCreated = FileUtil.ensureDir(result);
            if (dirCreated) {
                log.debug("Report directory created: " + result);
            }
        } catch (IOException e) {
            // Bug workaround: For some strange reason, a directory cannot 
            // (sometimes) be created right after it have been deleted (using 
            // JDK 1.5.0_22 under Windows). The problem seems to occur more 
            // often if much I/O have been perfomed prior deletion.
            // Solution: Retry with sleep. 
            boolean success = false;
            for (int i = 0; !success && (i < 10); i++) {
                log.info("Retry #" + (i+1) + " to create directory: " + result);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e2) {
                    log.warn("Thread.sleep failed.");
                }

                try {
                    FileUtil.ensureDir(result);
                    success = true;
                } catch (IOException e1) {
                    success = false;
                }
            }
            if (!success) {
                throw new MegatronException("Cannot create directory: " + result, e);
            }
        }

        return result;
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
        // CitySlugDecorator and MaskedIpAddressDecorator is an inner-class and cannot be instantiated by name
        CitySlugDecorator citySlugDecorator = new CitySlugDecorator();
        MaskedIpAddressDecorator maskedIpAddressDecorator = new MaskedIpAddressDecorator();
        
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            if (!filterManager.executeFilters(logEntry)) {
                continue;
            }
            decoratorManager.executeDecorators(logEntry);
            citySlugDecorator.execute(logEntry);
            maskedIpAddressDecorator.execute(logEntry);
            result.add(logEntry);
        }

        return result;
    }

    
    private void convertTimeStamps(List<LogEntry> logEntries) {
        String format = props.getString(AppProperties.EXPORT_TIMESTAMP_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss z");
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            String lastSeenInSec = logEntry.getAdditionalItems().get(LAST_SEEN_KEY);
            String lastSeenStr = DateUtil.formatDateTime(format, SqlUtil.convertTimestamp(Long.parseLong(lastSeenInSec)));
            logEntry.getAdditionalItems().put(LAST_SEEN_KEY, lastSeenStr);
        }
    }
    
    
    private List<String> getDays(TimePeriod timePeriod, String format) {
        List<String> result = new ArrayList<String>();
        DateTime startDateTime = new DateTime(timePeriod.getStartDate());
        DateTime endDateTime = new DateTime(timePeriod.getEndDate());
        // TODO: locale hardcoded (used for weekdays)
        SimpleDateFormat formatter = new SimpleDateFormat(format, new Locale("sv", "SE", ""));
        while (startDateTime.isBefore(endDateTime)) {
            String dateStr = formatter.format(startDateTime.toDate());
            result.add(dateStr);
            startDateTime = startDateTime.plusDays(1);
        }
        return result;
    }
    
    
    private String getDayLabels(TimePeriod timePeriod) {
        StringBuffer result = new StringBuffer(256);
        String itemTemplate = "{\"date\":\"@date@\",\"dayInWeek\":\"@dayInWeek@\"}";
        DateTime startDateTime = new DateTime(timePeriod.getStartDate());
        DateTime endDateTime = new DateTime(timePeriod.getEndDate());
        // TODO: locale and format hardcoded (used for weekdays)
        String format = "yyyy-MM-dd EEEE";
        SimpleDateFormat formatter = new SimpleDateFormat(format, new Locale("sv", "SE", ""));
        while (startDateTime.isBefore(endDateTime)) {
            String dateStr = formatter.format(startDateTime.toDate());
            String[] dateTokens = StringUtil.splitHeadTail(dateStr, " ", false);
            String itemStr = StringUtil.replace(itemTemplate, "@date@", dateTokens[0]);
            // dateTokens[1] = StringUtil.replace(dateTokens[1], "dag", "");            
            dateTokens[1] = dateTokens[1].substring(0, 1).toUpperCase() + dateTokens[1].substring(1); 
            itemStr = StringUtil.replace(itemStr, "@dayInWeek@", dateTokens[1]);
            if (result.length() > 0) {
                result.append(",");
            }
            result.append(itemStr);
            startDateTime = startDateTime.plusDays(1);
        }
        return result.toString();
    }

    
    private String convertCity(String city) {
        // replace swedish characters etc. from city name so it's a valid filename
        String result = city;

        // Swedish characters
        result = StringUtil.replace(result, "\u00e5", "aa");
        result = StringUtil.replace(result, "\u00c5", "aa");
        result = StringUtil.replace(result, "\u00e4", "ae");
        result = StringUtil.replace(result, "\u00c4", "ae");
        result = StringUtil.replace(result, "\u00f6", "oe");
        result = StringUtil.replace(result, "\u00d6", "oe");

        // Norwegian and Danish characters
        result = StringUtil.replace(result, "\u00e6", "ae");
        result = StringUtil.replace(result, "\u00c6", "ae");
        result = StringUtil.replace(result, "\u00f8", "oe");
        result = StringUtil.replace(result, "\u00d8", "oe");
        
        // German characters
        result = StringUtil.replace(result, "\u00fc", "u");
        result = StringUtil.replace(result, "\u00dc", "u");
        result = StringUtil.replace(result, "\u00df", "ss");

        // Other characters
        result = StringUtil.replace(result, " ", "_");
        
        // Remove diacritics (accents) 
        String nfdNormalizedString = Normalizer.normalize(result, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        result = pattern.matcher(nfdNormalizedString).replaceAll("");

        return result.toLowerCase();
    }
    
    
    private Map<String, String> createHeaderMap(TimePeriod timePeriod) {
        String startDateStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, timePeriod.getStartDate());
        String endDateStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, timePeriod.getEndDate());
        String timePeriodLabel = startDateStr + " - " + endDateStr;
        String days = StringUtil.toQuotedString(getDays(timePeriod, DateUtil.DATE_FORMAT));
        String dayLabels = getDayLabels(timePeriod);
        
        Map<String, String> result = new HashMap<String, String>();
        result.put(HEADER_REPORT_STARTED_KEY, DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, new Date()));
        result.put(HEADER_START_DATE_KEY, startDateStr);
        result.put(HEADER_END_DATE_KEY, endDateStr);
        result.put(HEADER_TIME_PERIOD_LABEL_KEY, timePeriodLabel);
        result.put(HEADER_DAYS_KEY, days);
        result.put(HEADER_DAY_LABELS_KEY, dayLabels);
        
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
    private List<LogEntry> createOrganizationSummaryData(List<LogEntry> logEntries, List<Priority> allPriorities) {
        allPriorities = new ArrayList<Priority>(allPriorities);
        allPriorities.add(new Priority(-1, MISSING_VALUE, -1));
        Map<String, LogEntry> orgMap = new HashMap<String, LogEntry>();
        List<String> killList = Arrays.asList(props.getStringListFromCommaSeparatedValue(AppProperties.REPORT_GEOLOCATION_ORGANIZATION_TYPE_KILL_LIST_KEY, new String[0], true));
        for (Iterator<Priority> iterator = allPriorities.iterator(); iterator.hasNext(); ) {
            String orgType = iterator.next().getName();
            if (killList.contains(orgType)) {
                continue;
            }
            LogEntry mapValue = createDummyLogEntry();
            orgMap.put(orgType, mapValue);
            if (mapValue.getAdditionalItems() == null) {
                mapValue.setAdditionalItems(new HashMap<String, String>());
            }
            mapValue.getAdditionalItems().put(UNIQUE_NO_OF_BAD_HOSTS_KEY, "0");
            mapValue.getAdditionalItems().put(TOTAL_NO_OF_BAD_HOSTS_KEY, "0");
            mapValue.getAdditionalItems().put(PRIO_NAME_KEY, orgType);
        }
        
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
                // Priority is deleted; should not happen
                log.warn("Priorty is deleted: " + orgType + ". Skipping entry.");
                continue;
            }
            int badHosts = 1 + Integer.parseInt(mapValue.getAdditionalItems().get(UNIQUE_NO_OF_BAD_HOSTS_KEY));
            mapValue.getAdditionalItems().put(UNIQUE_NO_OF_BAD_HOSTS_KEY, Integer.toString(badHosts));
            int timesSeen = Integer.parseInt(logEntry.getAdditionalItems().get(TIMES_SEEN_KEY));
            badHosts = timesSeen + Integer.parseInt(mapValue.getAdditionalItems().get(TOTAL_NO_OF_BAD_HOSTS_KEY));
            mapValue.getAdditionalItems().put(TOTAL_NO_OF_BAD_HOSTS_KEY, Integer.toString(badHosts));
        }
        List<LogEntry> result = new ArrayList<LogEntry>(orgMap.values());
        Collections.sort(result, new Comparator<LogEntry>() {
                @Override
                public int compare(LogEntry o1, LogEntry o2) {
                    int badHosts1 = Integer.parseInt(o1.getAdditionalItems().get(UNIQUE_NO_OF_BAD_HOSTS_KEY));
                    int badHosts2 = Integer.parseInt(o2.getAdditionalItems().get(UNIQUE_NO_OF_BAD_HOSTS_KEY));
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
                @Override
                public int compare(LogEntry o1, LogEntry o2) {
                    int badHosts1 = Integer.parseInt(o1.getAdditionalItems().get(UNIQUE_NO_OF_BAD_HOSTS_KEY));
                    int badHosts2 = Integer.parseInt(o2.getAdditionalItems().get(UNIQUE_NO_OF_BAD_HOSTS_KEY));
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

    
    private LogEntry clone(LogEntry logEntry) {
        // shallow clone for only the attributes that we use 
        LogEntry result = new LogEntry(logEntry.getId());
        result.setLogTimestamp(logEntry.getLogTimestamp());
        result.setCreated(logEntry.getCreated());
        result.setIpAddress(logEntry.getIpAddress());
        result.setPort(logEntry.getPort());
        result.setHostname(logEntry.getHostname());
        result.setAsn(logEntry.getAsn());
        result.setAdditionalItems(new HashMap<String, String>(logEntry.getAdditionalItems()));
        
        return result;
    }

    
    private void createSummaryFile(boolean internal) throws MegatronException {
        String dir = initOutputDir(internal);
        additionalProps.put(AppProperties.OUTPUT_DIR_KEY, dir);
        additionalProps.put(AppProperties.EXPORT_HEADER_FILE_KEY, "geolocation-summary-internal_header.json");
        additionalProps.put(AppProperties.EXPORT_ROW_FILE_KEY, null);
        additionalProps.put(AppProperties.EXPORT_FOOTER_FILE_KEY, null);
        List<LogEntry> dummyList = new ArrayList<LogEntry>();
        createEntriesFile("summary.json", dummyList);
    }

    
    private void createOrganizationFile(List<LogEntry> organizationSummaryData, boolean internal) throws MegatronException {
        // convert organization type names  
        List<NameValuePair> mappingList = props.getNameValuePairList(AppProperties.REPORT_GEOLOCATION_ORGANIZATION_TYPE_NAME_MAPPER_KEY, null);
        if ((mappingList != null) && (mappingList.size() > 0)) {
            for (Iterator<NameValuePair> iterator = mappingList.iterator(); iterator.hasNext(); ) {
                NameValuePair nameValuePair = iterator.next();
                for (Iterator<LogEntry> iterator2 = organizationSummaryData.iterator(); iterator2.hasNext(); ) {
                    Map<String, String> additionalItems = iterator2.next().getAdditionalItems();
                    String organizationTypeName = additionalItems.get(PRIO_NAME_KEY);
                    if ((organizationTypeName != null) && (organizationTypeName.equals(nameValuePair.getName()))) {
                        additionalItems.put(PRIO_NAME_KEY, nameValuePair.getValue());
                    }
                }
            }
        }

        // create file
        String dir = initOutputDir(internal);
        additionalProps.put(AppProperties.OUTPUT_DIR_KEY, dir);
        additionalProps.put(AppProperties.EXPORT_HEADER_FILE_KEY, "geolocation-organization_header.json");
        additionalProps.put(AppProperties.EXPORT_ROW_FILE_KEY, "geolocation-organization_row.json");
        additionalProps.put(AppProperties.EXPORT_FOOTER_FILE_KEY, "array-in-dict-end_footer.json");
        createEntriesFile("organization.json", organizationSummaryData);
    }

    
    private void createCityFile(List<LogEntry> citySummaryData, boolean internal) throws MegatronException {
        String dir = initOutputDir(internal);
        additionalProps.put(AppProperties.OUTPUT_DIR_KEY, dir);
        additionalProps.put(AppProperties.EXPORT_HEADER_FILE_KEY, "geolocation-city_header.json");
        additionalProps.put(AppProperties.EXPORT_ROW_FILE_KEY, "geolocation-city_row.json");
        additionalProps.put(AppProperties.EXPORT_FOOTER_FILE_KEY, "array-in-dict-end_footer.json");
        createEntriesFile("city.json", citySummaryData);
    }
    
    
    private void createEntriesOverviewFiles(TimePeriod timePeriod, List<LogEntry> logEntries, boolean internal) throws MegatronException {
        createEntriesOverviewFile("all-days.json", logEntries, internal);
        createEntriesCityFiles("all-days", logEntries, internal);
        
        List<String> days = getDays(timePeriod, DateUtil.DATE_FORMAT);
        for (Iterator<String> iterator = days.iterator(); iterator.hasNext(); ) {
            String dateStr = iterator.next();
            List<LogEntry> logEntriesInDay = new ArrayList<LogEntry>();
            for (Iterator<LogEntry> iteratorLogEntries = logEntries.iterator(); iteratorLogEntries.hasNext(); ) {
                LogEntry logEntry = iteratorLogEntries.next();
                // Note: this will only work if ISO date is used
                String dateStr1 = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, SqlUtil.convertTimestamp(logEntry.getLogTimestamp()));
                String dateStr2 = logEntry.getAdditionalItems().get(LAST_SEEN_KEY).substring(0, "2011-01-01".length());
                if ((dateStr.compareTo(dateStr1) >= 0) && (dateStr.compareTo(dateStr2) <= 0)) {
                    logEntriesInDay.add(logEntry);
                }
            }
            createEntriesOverviewFile(dateStr + ".json", logEntriesInDay, internal);
            createEntriesCityFiles(dateStr, logEntriesInDay, internal);
        }
    }

    
    private void createEntriesOverviewFile(String filename, List<LogEntry> logEntries, boolean internal) throws MegatronException {
        Map<String, LogEntry> cityMap = new HashMap<String, LogEntry>();
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            String city = logEntry.getAdditionalItems().get(CITY_KEY);
            city = convertCity(city);
            LogEntry overviewLogEntry = cityMap.get(city);
            if (overviewLogEntry == null) {
                cityMap.put(city, clone(logEntry));
            } else {
                Map<String, String> additionalItems = overviewLogEntry.getAdditionalItems();
                int timesSeen = Integer.parseInt(additionalItems.get(TIMES_SEEN_KEY));
                timesSeen += Integer.parseInt(logEntry.getAdditionalItems().get(TIMES_SEEN_KEY));
                additionalItems.put(TIMES_SEEN_KEY, timesSeen + "");
            }
        }
        
        additionalProps.put(AppProperties.OUTPUT_DIR_KEY, getOutputDir(internal));
        additionalProps.put(AppProperties.EXPORT_HEADER_FILE_KEY, "array-begin_header.json");
        additionalProps.put(AppProperties.EXPORT_ROW_FILE_KEY, "geolocation-entries-overview_row.json");
        additionalProps.put(AppProperties.EXPORT_FOOTER_FILE_KEY, "array-end_footer.json");
        createEntriesFile(filename, new ArrayList<LogEntry>(cityMap.values()));
    }

    
    private void createEntriesCityFiles(String dir, List<LogEntry> logEntries, boolean internal) throws MegatronException {
        Map<String, List<LogEntry>> cityMap = new HashMap<String, List<LogEntry>>();
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            String city = logEntry.getAdditionalItems().get(CITY_KEY);
            city = convertCity(city);
            List<LogEntry> list = cityMap.get(city);
            if (list == null) {
                list = new ArrayList<LogEntry>();
                cityMap.put(city, list);
            }
            list.add(logEntry);
        }

        String fullDir = FileUtil.concatPath(getOutputDir(internal), dir);
        try {
            FileUtil.ensureDir(fullDir);
        } catch (IOException e) {
            throw new MegatronException("Cannot create directory: " + fullDir, e);
        }
        
        additionalProps.put(AppProperties.OUTPUT_DIR_KEY, fullDir);
        additionalProps.put(AppProperties.EXPORT_HEADER_FILE_KEY, "array-begin_header.json");
        String rowFile = internal ? "geolocation-entries-city-internal_row.json" : "geolocation-entries-city_row.json"; 
        additionalProps.put(AppProperties.EXPORT_ROW_FILE_KEY, rowFile);
        additionalProps.put(AppProperties.EXPORT_FOOTER_FILE_KEY, "array-end_footer.json");
        for (Iterator<String> iterator = cityMap.keySet().iterator(); iterator.hasNext(); ) {
            String city = iterator.next();
            createEntriesFile(city + ".json", cityMap.get(city));
        }
    }

    
    private void createEntriesFile(String filename, List<LogEntry> logEntries) throws MegatronException {
        Job job = new Job(0L, filename, filename, "", 0L, System.currentTimeMillis());
        JobContext jobContext = new JobContext(props, job);
        FileExporter fileExporter = new FileExporter(jobContext);
        fileExporter.setHeaderMap(headerMap);
        fileExporter.setReplaceArray(REPLACE_ARRAY);
        fileExporter.setSeparator(",");
        fileExporter.writeHeader(job);
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            LogEntry logEntry = iterator.next();
            fileExporter.writeLogEntry(logEntry);
        }
        fileExporter.writeFooter(job);
        fileExporter.close();
        // too many log entries: fileExporter.writeFinishedMessageToLog();
    }

    
    private void concatenateFiles(boolean internal) throws MegatronException {
        // summary.json + city.json + organization.json --> summary-all.json
        String dir = getOutputDir(internal);
        File file = null;
        try {
            // reading files
            file = new File(dir, "summary.json");
            String summaryContent = FileUtil.readFile(file, Constants.UTF8);
            file = new File(dir, "city.json");
            String cityContent = FileUtil.readFile(file, Constants.UTF8);
            file = new File(dir, "organization.json");
            String organizationContent = FileUtil.readFile(file, Constants.UTF8);

            // remove general info and enclosing "{}"
            if (summaryContent.length() > 4) {
                summaryContent = summaryContent.substring(0, summaryContent.length()-2);
            }
            int cityEntriesIndex = cityContent.indexOf("\"cityEntries\"");
            if (cityEntriesIndex != -1) {
                cityContent = cityContent.substring(cityEntriesIndex, cityContent.length()-2);
            }
            int organizationTypeEntriesIndex = organizationContent.indexOf("\"organizationTypeEntries\"");
            if (organizationTypeEntriesIndex != -1) {
                organizationContent = organizationContent.substring(organizationTypeEntriesIndex, organizationContent.length()-1);
            }

            // concatenate content and write to file
            // String content = "{\"summaryInfo\":@summaryContent@,\"cityInfo\":{@cityContent@,\"organizationTypeInfo\":{@organizationContent@}";
            String content = "@summaryContent@,@cityContent@,@organizationContent@";
            content = StringUtil.replace(content, "@summaryContent@", summaryContent);
            content = StringUtil.replace(content, "@cityContent@", cityContent);
            content = StringUtil.replace(content, "@organizationContent@", organizationContent);
            content = StringUtil.replace(content, "\n", "");
            
            File outFile = new File(dir, "summary-all.json");
            FileUtil.writeFile(outFile, content);
        } catch (IOException e) {
            String fileStr = (file != null) ? file.getAbsolutePath() : "[null]";
            throw new MegatronException("Cannot read file: " + fileStr, e);
        }
    }

    
    /**
     * Adds "citySlug" to additional items from "city". 
     * "citySlug" is the filename/URL to the city file.
     */
    private class CitySlugDecorator implements IDecorator {


        public CitySlugDecorator() {
            // empty
        }

        
        @Override
        public void init(JobContext jobContext) throws MegatronException {
            // empty
        }

        
        @Override
        @SuppressWarnings("synthetic-access")
        public void execute(LogEntry logEntry) throws MegatronException {
            Map<String, String> additionalItems = logEntry.getAdditionalItems();
            if ((additionalItems != null) && (additionalItems.get(CITY_KEY) != null)) {
                String city = additionalItems.get(CITY_KEY);
                additionalItems.put(CITY_SLUG_KEY, convertCity(city));
            }
        }


        @Override
        public void close() throws MegatronException {
            // empty
        }
        
    }

    
    /**
     * Adds "ipAddressMasked" to additional items. 
     */
    private class MaskedIpAddressDecorator implements IDecorator {


        public MaskedIpAddressDecorator() {
            // empty
        }

        
        @Override
        public void init(JobContext jobContext) throws MegatronException {
            // empty
        }

        
        @Override
        public void execute(LogEntry logEntry) throws MegatronException {
            Map<String, String> additionalItems = logEntry.getAdditionalItems();
            if ((additionalItems != null) && (logEntry.getIpAddress() != null)) {
                String ipAddressMasked = IpAddressUtil.convertAndMaskIpAddress(logEntry.getIpAddress());
                additionalItems.put(IP_ADDRESS_MASKED_KEY, ipAddressMasked);
            }
        }


        @Override
        public void close() throws MegatronException {
            // empty
        }
        
    }

}
