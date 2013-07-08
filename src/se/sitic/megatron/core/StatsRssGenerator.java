package se.sitic.megatron.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.db.DbStatisticsData;
import se.sitic.megatron.rss.StatsRssFile;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Creates RSS file with Megatron statistics. File is re-generated, no old
 * items are saved.
 */
public class StatsRssGenerator {
    // Note: Same RSS readers, e.g. RSSOwl, matches a new RSS item to an 
    // existing using the title. Thus, if the title is the same the item
    // will not been flagged as new and the old description will be replaced 
    // by new content. Work around: add timestamp in title.
    
    private final Logger log = Logger.getLogger(StatsRssGenerator.class);
    
    private static final int NO_OF_WEEKS = 8;
    private static final String LINE_BREAK = "<br>";
    private static final String TITLE_TEMPLATE = "@period@: Saved Entries: @noOfLogEntries@, Sent Emails: @noOfSentEmails@ [@generationStarted@]";
    private static final String DESCRIPTION_TEMPLATE = 
        "Period: @period@" + LINE_BREAK + 
        "Successful Jobs: @noOfSuccessfulJobs@" + LINE_BREAK + 
        "Failed Jobs: @noOfFailedJobs@" + LINE_BREAK +
        "Processed Lines: @noOfProcessedLines@" + LINE_BREAK +
        "Saved Entries: @noOfLogEntries@" + LINE_BREAK +
        "Send Emails: @noOfSentEmails@" + LINE_BREAK +
        "New Organizations: @noOfNewOrganizations@" + LINE_BREAK +
        "Modified Organizations: @noOfModifiedOrganizations@" + LINE_BREAK +
        "Entries Matched to an Organisation: @noOfMatchedLogEntries@";
    
    private TypedProperties props;
    private Date generationStarted;
    
    
    public StatsRssGenerator(TypedProperties props) {
        this.props = props;
    }

    
    public void createFile() throws MegatronException {
        log.info("Creating RSS with Megatron statistics.");
        this.generationStarted = new Date();
        List<String> titles = new ArrayList<String>(32);
        List<String> descriptions = new ArrayList<String>(32);
        DbManager dbManager = null;
        try {
            dbManager = DbManager.createDbManager(props); 
            DateTime dateTime = startDateTime();
            TimePeriod weeklySummaryPeriod = createDateTimePeriod(dateTime.minusDays(6), dateTime);
            DbStatisticsData weeklySummaryData = new DbStatisticsData(weeklySummaryPeriod.getStartDate(), weeklySummaryPeriod.getEndDate());
            String title = null;
            String description = null;
            
            // -- Last 7 days
            for (int i = 0; i < 7; i++) {
                TimePeriod timePeriod = createDayTimePeriod(dateTime);
                DbStatisticsData statsData = fetchStatistics(dbManager, timePeriod);
                title = TITLE_TEMPLATE;
                title = StringUtil.replace(title, "@period@", timePeriod.getStartWeekday(TimePeriod.LONG_FORMAT));
                title = replace(title, statsData);
                titles.add(title);
                description = DESCRIPTION_TEMPLATE;
                String period = DateUtil.formatDateTime("EEEE yyyy-MM-dd", timePeriod.getStartDate());
                description = StringUtil.replace(description, "@period@", period);
                description = replace(description, statsData);
                descriptions.add(description);
                
                weeklySummaryData.inc(statsData);
                dateTime = dateTime.minusDays(1);
            }
            
            // -- Weekly summary
            title = TITLE_TEMPLATE;
            title = StringUtil.replace(title, "@period@", "Weekly Summary (last 7 days)");
            title = replace(title, weeklySummaryData);
            titles.add(title);
            description = DESCRIPTION_TEMPLATE;
            description = StringUtil.replace(description, "@period@", weeklySummaryPeriod.getFormattedPeriodString(TimePeriod.LONG_FORMAT));
            description = replace(description, weeklySummaryData);
            descriptions.add(description);
            
            // -- Weeks
            dateTime = startDateTime().minusDays(7);
            for (int i = 0; i < NO_OF_WEEKS; i++) {
                TimePeriod timePeriod = createWeekTimePeriod(dateTime);
                DbStatisticsData statsData = fetchStatistics(dbManager, timePeriod);
                title = TITLE_TEMPLATE;
                title = StringUtil.replace(title, "@period@", getShortWeekString(timePeriod));
                title = replace(title, statsData);
                titles.add(title);
                description = DESCRIPTION_TEMPLATE;
                description = StringUtil.replace(description, "@period@", timePeriod.getFormattedPeriodString(TimePeriod.LONG_FORMAT));
                description = replace(description, statsData);
                descriptions.add(description);
                
                dateTime = dateTime.minusDays(7);
            }
        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
        
        StatsRssFile rssFile = new StatsRssFile(props);
        rssFile.addItems(titles, descriptions);
        log.info("Creating statistics RSS.");
    }

    
    private DateTime startDateTime() {
        return new DateTime(); 
    }

    
    private TimePeriod createDayTimePeriod(DateTime dateTime) throws MegatronException {
        String dateStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, dateTime.toDate());
        String periodStr = dateStr + "--" + dateStr;
        return new TimePeriod(periodStr);
    }

    
    private TimePeriod createDateTimePeriod(DateTime startDate, DateTime endDate) throws MegatronException {
        String startDateStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, startDate.toDate());
        String endDateStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, endDate.toDate());
        String periodStr = startDateStr + "--" + endDateStr;
        return new TimePeriod(periodStr);
    }

    
    private TimePeriod createWeekTimePeriod(DateTime dateTime) throws MegatronException {
        int week = dateTime.getWeekOfWeekyear();
        String periodStr = "w" + dateTime.getYear() + "-" + week;
        return new TimePeriod(periodStr);
    }
    
    
    private String getShortWeekString(TimePeriod timePeriod) {
        String result = timePeriod.getFormattedPeriodString(TimePeriod.SHORT_FORMAT);
        // remove year suffix, e.g. "2009-"
        String yearSuffix = DateUtil.formatDateTime("yyyy", timePeriod.getStartDate()) + "-"; 
        result = StringUtil.replace(result, yearSuffix, "");
        return result;
    }
    
    
    private String replace(String template, DbStatisticsData statsData) {
        String result = template;
        result = StringUtil.replace(result, "@noOfSuccessfulJobs@", statsData.getNoOfSuccessfulJobs() + "");
        result = StringUtil.replace(result, "@noOfFailedJobs@", statsData.getNoOfFailedJobs() + "");
        result = StringUtil.replace(result, "@noOfProcessedLines@", statsData.getNoOfProcessedLines() + "");
        result = StringUtil.replace(result, "@noOfLogEntries@", statsData.getNoOfLogEntries() + "");
        result = StringUtil.replace(result, "@noOfSentEmails@", statsData.getNoOfSentEmails() + "");
        result = StringUtil.replace(result, "@noOfNewOrganizations@", statsData.getNoOfNewOrganizations() + "");
        result = StringUtil.replace(result, "@noOfModifiedOrganizations@", statsData.getNoOfModifiedOrganizations() + "");
        result = StringUtil.replace(result, "@noOfMatchedLogEntries@", statsData.getNoOfMatchedLogEntries() + "");
        result = StringUtil.replace(result, "@generationStarted@", DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, generationStarted));
        
        return result;
    }

    
    private DbStatisticsData fetchStatistics(DbManager dbManager, TimePeriod timePeriod) throws MegatronException {
        log.debug("Fetching statistics for period: " + timePeriod.toString());
        
//        // create dummy
//        DbStatisticsData result = new DbStatisticsData(timePeriod.getStartDate(), timePeriod.getEndDate());
//        result.setNoOfSuccessfulJobs(1);
//        result.setNoOfFailedJobs(2);
//        result.setNoOfProcessedLines(3);
//        result.setNoOfLogEntries(4);
//        result.setNoOfSentEmails(5);
//        result.setNoOfNewOrganizations(6);
//        result.setNoOfModifiedOrganizations(7);
//        result.setNoOfMatchedLogEntries(8);
//        return result;

        return dbManager.fetchStatistics(timePeriod.getStartDate(), timePeriod.getEndDate());
    }

}
