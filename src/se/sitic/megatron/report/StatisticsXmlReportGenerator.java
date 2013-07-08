package se.sitic.megatron.report;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TimePeriod;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.FileUtil;
import se.sitic.megatron.util.StringUtil;

// Swedish characters: aao AAO: \u00e5\u00e4\u00f6 \u00c5\u00c4\u00d6


/**
 * Creates XML files with overview statistics. Can be used in JavaScript or
 * Flash graphs.
 */
public class StatisticsXmlReportGenerator implements IReportGenerator {
    private final Logger log = Logger.getLogger(StatisticsXmlReportGenerator.class);

    /** Entry types to include in graph. */
    private static final String[] ENTRY_TYPES = { "Botn\u00e4t", "Skr\u00e4ppost (RBL)", "\u00d6vrigt" };
    
    private static final int PROCESSED_LINES_FILE = 1;
    private static final int PROCESSED_LINES_SUM_FILE = 2;
    private static final int ALL_LOG_ENTRIES_FILE = 3;
    private static final int MATCHED_LOG_ENTRIES_FILE = 4;
    
    private static final String PROCESSED_LINES_FILENAME = "megatron-processed-lines.xml";
    private static final String PROCESSED_LINES_SUM_FILENAME = "megatron-processed-lines-sum.xml";
    private static final String ALL_LOG_ENTRIES_FILENAME = "megatron-all-entries.xml";
    private static final String MATCHED_LOG_ENTRIES_FILENAME = "megatron-matched-entries.xml";
    
    private static final String TOTAL_ENTRY_TYPE_NAME = "Totalt";
    
    private static final String DOC_TEMPLATE = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + Constants.LINE_BREAK + 
        "<data>" + Constants.LINE_BREAK + 
        "@seriesNames@" + Constants.LINE_BREAK +
        "@seriesValues@" +
        "</data>";

    private static final String SERIES_NAMES_TEMPLATE = 
        "  <seriesNames>" + Constants.LINE_BREAK +
        "@nameSeries@" + 
        "  </seriesNames>" + Constants.LINE_BREAK; 

    private static final String NAME_SERIE_TEMPLATE = "    <nameSerie@index@>@name@</nameSerie@index@>" + Constants.LINE_BREAK;

    private static final String SERIES_VALUES_TEMPLATE = 
        "  <seriesValues time=\"@time@\">" + Constants.LINE_BREAK +
        "@valueSeries@" +
        "  </seriesValues>" + Constants.LINE_BREAK;

    private static final String VALUE_SERIE_TEMPLATE = "    <valueSerie@index@>@value@</valueSerie@index@>" + Constants.LINE_BREAK;

    private TypedProperties props;
    
    
    public StatisticsXmlReportGenerator() {
        // empty
    }

    
    public void init() {
        this.props = AppProperties.getInstance().getGlobalProperties();
    }
    

    public void createFiles() throws MegatronException {
        log.info("Creating XML files with overview statistics.");
        DbManager dbManager = null;
        try {
            dbManager = DbManager.createDbManager(props);
            
            createFile(dbManager, PROCESSED_LINES_FILE);
            createFile(dbManager, PROCESSED_LINES_SUM_FILE);
            createFile(dbManager, ALL_LOG_ENTRIES_FILE);
            createFile(dbManager, MATCHED_LOG_ENTRIES_FILE);
        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
    }

    
    private void createFile(DbManager dbManager, int fileType) throws MegatronException {
        // -- init
        int noOfWeeks = props.getInt(AppProperties.REPORT_STATISTICS_NO_OF_WEEKS_KEY, 0);
        if (noOfWeeks == 0) {
            // read deprecated property
            noOfWeeks = props.getInt(AppProperties.FLASH_NO_OF_WEEKS_KEY, 5);
        }
        DateTime dateTime = startDateTime(noOfWeeks);
        String outputDir = props.getString(AppProperties.REPORT_OUTPUT_DIR_KEY, null);
        if (outputDir == null) {
            // read deprecated property
            outputDir = props.getString(AppProperties.FLASH_OUTPUT_DIR_KEY, "/var/megatron/flash-xml");
        }
        try {
            FileUtil.ensureDir(outputDir);
        } catch (IOException e) {
            throw new MegatronException("Cannot create directory: " + outputDir, e);
        }
        String[] entryTypes = { TOTAL_ENTRY_TYPE_NAME };
        if (fileType != PROCESSED_LINES_SUM_FILE) {
            entryTypes = ENTRY_TYPES;
        }

        // -- seriesNames
        StringBuilder nameSeries = new StringBuilder(1*1024);
        for (int i = 0; i < entryTypes.length; i++) {
            String entryType = entryTypes[i];
            String nameSerie = NAME_SERIE_TEMPLATE;
            nameSerie = StringUtil.replace(nameSerie, "@index@", i + "");
            nameSerie = StringUtil.replace(nameSerie, "@name@", entryType);
            nameSeries.append(nameSerie);
        }
        String seriesNames = SERIES_NAMES_TEMPLATE;
        seriesNames = StringUtil.replace(seriesNames, "@nameSeries@", nameSeries.toString());
        
        // -- seriesValues
        StringBuilder seriesValues = new StringBuilder(2*1024);
        for (int i = 0; i < noOfWeeks; i++) {
            Map<String, Long> valuesMap = null;
            if ((fileType == PROCESSED_LINES_FILE) || (fileType == PROCESSED_LINES_SUM_FILE)) {
                valuesMap = fetchNoOfProcessedLinesPerEntryType(dbManager, dateTime);
            } else if (fileType == ALL_LOG_ENTRIES_FILE) {
                valuesMap = fetchNoOfLogEntriesPerEntryType(dbManager, dateTime, false);
            } else {
                // MATCHED_LOG_ENTRIES_FILE
                valuesMap = fetchNoOfLogEntriesPerEntryType(dbManager, dateTime, true);
            }

            StringBuilder valueSeries = new StringBuilder(1*1024);
            for (int j = 0; j < entryTypes.length; j++) {
                String entryType = entryTypes[j];
                Long val = valuesMap.get(entryType);
                if (val == null) {
                    log.warn("Cannot find value for entry type: " + entryType);
                    val = new Long(0);
                }
                String valueSerie = VALUE_SERIE_TEMPLATE;
                valueSerie = StringUtil.replace(valueSerie, "@index@", j + "");
                valueSerie = StringUtil.replace(valueSerie, "@value@", val.toString());
                valueSeries.append(valueSerie);
            }
            String seriesValuesItem = SERIES_VALUES_TEMPLATE;
            int week = dateTime.getWeekOfWeekyear();
            seriesValuesItem = StringUtil.replace(seriesValuesItem, "@time@", "v." + week);
            seriesValuesItem = StringUtil.replace(seriesValuesItem, "@valueSeries@", valueSeries.toString());
            seriesValues.append(seriesValuesItem);
            
            dateTime = dateTime.plusDays(7);
        }
        
        // -- XML doc
        String doc = DOC_TEMPLATE;
        doc = StringUtil.replace(doc, "@seriesNames@", seriesNames);
        doc = StringUtil.replace(doc, "@seriesValues@", seriesValues.toString());
        
        // -- write to file
        String filename = null;
        if (fileType == PROCESSED_LINES_FILE) {
            filename = PROCESSED_LINES_FILENAME;
        } else if (fileType == PROCESSED_LINES_SUM_FILE) {
            filename = PROCESSED_LINES_SUM_FILENAME;
        } else if (fileType == ALL_LOG_ENTRIES_FILE) {
            filename = ALL_LOG_ENTRIES_FILENAME;
        } else {
            // MATCHED_LOG_ENTRIES_FILE
            filename = MATCHED_LOG_ENTRIES_FILENAME;
        }
        File file = new File(outputDir, filename);
        try {
            FileUtil.writeFile(file, doc, Constants.UTF8);
        } catch (IOException e) {
            throw new MegatronException("Cannot create Flash XML file: " + file.getAbsoluteFile(), e);
        }
        log.info("Flash XML file created: " + file.getAbsoluteFile() + " (" + file.length() + " bytes).");
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

    
    private Map<String, Long> fetchNoOfProcessedLinesPerEntryType(DbManager dbManager, DateTime startDate) throws MegatronException {
        TimePeriod timePeriod = createWeekTimePeriod(startDate);
        log.debug("Fetching no. of processed lines for period: " + timePeriod.getFormattedPeriodString(TimePeriod.LONG_FORMAT));
        
//        // create dummy
//        Map<String, Long> result = new HashMap<String, Long>();
//        int intervalStart = 20000;
//        int intervalEnd = 40000;
//        result.put("Foo", randomLong(intervalStart, intervalEnd));
//        result.put("Skr\u00e4ppost (RBL)", randomLong(intervalStart, intervalEnd));
//        result.put("\u00d6vrigt", randomLong(intervalStart, intervalEnd));
//        result.put("Botn\u00e4t", randomLong(intervalStart, intervalEnd));
//        result.put("Bar", randomLong(intervalStart, intervalEnd));
//        return result;
        
        Map<String, Long> result = dbManager.fetchNoOfProcessedLinesPerEntryType(timePeriod.getStartDate(), timePeriod.getEndDate());
        
        // Add "Total" entry type
        long total = 0L; 
        for (int i = 0; i < ENTRY_TYPES.length; i++) {
            String entryType = ENTRY_TYPES[i];
            Long val = result.get(entryType);
            if (val == null) {
                log.warn("Cannot find value for entry type: " + entryType);
                val = new Long(0);
            }
            total += val;
        }
        result.put(TOTAL_ENTRY_TYPE_NAME, total);

        return result;
    }

    
    private Map<String, Long> fetchNoOfLogEntriesPerEntryType(DbManager dbManager, DateTime startDate, boolean onlyMatchedEntries) throws MegatronException {
        TimePeriod timePeriod = createWeekTimePeriod(startDate);
        log.debug("Fetching no. of log entries for period: " + timePeriod.getFormattedPeriodString(TimePeriod.LONG_FORMAT));
        
//        // create dummy
//        Map<String, Long> result = new HashMap<String, Long>();
//        int intervalStart = onlyMatchedEntries ? 5000 : 8000;
//        int intervalEnd = onlyMatchedEntries ? 12000 : 15000;
//        result.put("\u00d6vrigt", randomLong(intervalStart, intervalEnd));
//        result.put("Skr\u00e4ppost (RBL)", randomLong(intervalStart, intervalEnd));
//        result.put("Foo", randomLong(intervalStart, intervalEnd));
//        result.put("Botn\u00e4t", randomLong(intervalStart, intervalEnd));
//        result.put("Bar", randomLong(intervalStart, intervalEnd));
//        return result;
        
        return dbManager.fetchNoOfLogEntriesPerEntryType(timePeriod.getStartDate(), timePeriod.getEndDate(), onlyMatchedEntries);
    }
    
    
    private TimePeriod createWeekTimePeriod(DateTime dateTime) throws MegatronException {
        int week = dateTime.getWeekOfWeekyear();
        String periodStr = "w" + dateTime.getYear() + "-" + week;
        return new TimePeriod(periodStr);
    }

    
// UNUSED   
//    private Long randomLong(int intervalStart, int intervalEnd) {
//        Random rand = new Random();
//        long result = Math.abs(rand.nextLong());
//        result = intervalStart + (result % (intervalEnd - intervalStart));
//        return new Long(result);
//    }

}
