package se.sitic.megatron.core;

import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Time period with a start- and end-time. 
 */
public class TimePeriod {
    public static final int SHORT_FORMAT = 1;
    public static final int LONG_FORMAT = 2;
    public static final int DATE_FORMAT = 4;
    
    private String periodString;
    private boolean weekFormat;
    private Date startDate;
    private Date endDate;

    
    /**
     * Constructs a time period by parsing specified string.
     */
    public TimePeriod(String periodString) throws MegatronException {
        this.periodString = periodString;
        parsePeriod();
    }

    
    /**
     * Constructs a time period from specified dates. Result will not be in 
     * week format.
     */
    public TimePeriod(Date startDate, Date endDate) throws MegatronException {
        if ((startDate == null) || (endDate == null)) {
            throw new MegatronException("Start date or end date is null.");
        }
        
        this.startDate = startDate; 
        this.endDate = endDate; 

        this.periodString = toString();
        this.weekFormat = false;
    }

    
    /**
     * Creates a time period before specified period. The duration for the
     * returned time period will be same as the specified. 
     */
    public static TimePeriod createPreviousPeriod(TimePeriod currentPeriod) throws MegatronException {
        Date orgStartDate = currentPeriod.getStartDate();
        Date orgEndDate = currentPeriod.getEndDate();
        long periodDiff = orgEndDate.getTime() - orgStartDate.getTime();  
        DateTime startDateTime = new DateTime(orgStartDate);
        DateTime endDateTime = new DateTime(orgEndDate);
        startDateTime = startDateTime.minus(periodDiff);
        endDateTime = endDateTime.minus(periodDiff);
        Date startDate = startDateTime.toDateMidnight().toDate();
        Date endDate = endDateTime.toDateMidnight().toDate();

        // set end time to 23:59:59
        endDate.setTime(endDate.getTime() - 1000L);
        
        return new TimePeriod(startDate, endDate);
    }

    
    public String getPeriodString() {
        return periodString;
    }

    
    public String getFormattedPeriodString(int format) {
        StringBuilder result = new StringBuilder(128);
        
        String startDateStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, getStartDate());
        String endDateStr = DateUtil.formatDateTime(DateUtil.DATE_FORMAT, getEndDate());
        if (format == DATE_FORMAT) {
            result.append(startDateStr).append(" - ").append(endDateStr);
        } else {
            if (weekFormat) {
                result.append("w.").append(periodString);
            }
            
            if (!weekFormat || (format == LONG_FORMAT)) {
                if (weekFormat && (format == LONG_FORMAT)) {
                    result.append(" (").append(startDateStr).append(" - ").append(endDateStr).append(")");
                } else {
                    result.append(startDateStr).append(" - ").append(endDateStr);
                }
            }
        }
        
        return result.toString();
    }

    
    public boolean isWeekFormat() {
        return weekFormat;
    }


    public Date getStartDate() {
        return startDate;
    }


    public Date getEndDate() {
        return endDate;
    }

    
    public String getStartWeekday(int format) {
        String formatStr = (format == SHORT_FORMAT) ? "EEE" : "EEEE";
        return DateUtil.formatDateTime(formatStr, startDate); 
    }

    
    public String getEndWeekday(int format) {
        String formatStr = (format == SHORT_FORMAT) ? "EEE" : "EEEE";
        return DateUtil.formatDateTime(formatStr, endDate); 
    }

    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(32);
        result.append(DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, startDate));
        result.append("--");
        result.append(DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, endDate));
        return result.toString();
    }

    
    private void parsePeriod() throws MegatronException {
        // The JDK cannot parse "year of week", but Joda can.
        // For example, "2008-01" does not work but "2008-02" does.
        // More info:
        // http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=bff79f21455177ffffffffc19116dffe41396?bug_id=4267450
        // http://joda-time.sourceforge.net/
        
        if (StringUtil.isNullOrEmpty(periodString)) {
            throw new MegatronException("Invalid period; null or empty: " + periodString);
        }

        DateTime startDateTime = null;
        DateTime endDateTime = null;
        // week format?
        String periodInUpperCase = periodString.toUpperCase();
        if (periodInUpperCase.startsWith("W")) {
            this.weekFormat = true;
            this.periodString = StringUtil.removePrefix(periodInUpperCase, "W");
            // Short format (w32)?
            if (!periodString.contains("-")) {
                // expand to full format (2008-32)
                String yearStr = Calendar.getInstance().get(Calendar.YEAR) + "-";
                periodString = yearStr + periodString;
            }
            try {
                // Turns out that Joda cannot parse "year of week" either, e.g.
                // "2009-01" works but not "2010-01". Use parseIsoWeek instead.
                // Joda 1.5.2 and 1.6 contains this bug.
//                DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-ww");
//                // Joda will ensure that week always start on a monday
//                startDateTime = fmt.parseDateTime(periodString);
                startDateTime = parseIsoWeek(periodString);
                endDateTime = startDateTime.plusDays(6);
            } catch (Exception e) {
                // UnsupportedOperationException, IllegalArgumentException
                String msg = "Cannot parse period in week-format: " + periodString;
                throw new MegatronException(msg, e);
            }
        } else {
            // period format (2008-08-20--2008-08-25)
            String[] headTail = StringUtil.splitHeadTail(periodString, "--", false);
            if (headTail == null || StringUtil.isNullOrEmpty(headTail[0]) || StringUtil.isNullOrEmpty(headTail[1])) {
                throw new MegatronException("Invalid period format: " + periodString);
            }
            try {
                DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
                startDateTime = fmt.parseDateTime(headTail[0]);
                endDateTime = fmt.parseDateTime(headTail[1]);
            } catch (Exception e) {
                // UnsupportedOperationException, IllegalArgumentException
                String msg = "Cannot parse period: " + periodString;
                throw new MegatronException(msg, e);
            }
        }
        
        // Adjust for DST. Add 23:59:59 to end date.
        this.startDate = startDateTime.toDateMidnight().toDate();
        this.endDate = endDateTime.plusDays(1).toDateMidnight().toDate();
        this.endDate.setTime(this.endDate.getTime() - 1000L);
    
        if (startDate.after(endDate)) {
            throw new MegatronException("Invalid period; start-date is after end-date: " + periodString);
        }
    }

    
    /**
     * Returns date for monday in specified week.
     *  
     * @param weekStr full week string, e.g. "2010-01" (which will return 2010-01-04).
     */
    private DateTime parseIsoWeek(String weekStr) throws Exception {
        DateTime result = null;
        
        // Split year and week
        String[] headTail = StringUtil.splitHeadTail(weekStr, "-", false);
        if ((headTail == null) || StringUtil.isNullOrEmpty(headTail[0]) || StringUtil.isNullOrEmpty(headTail[1]) || (headTail[0].length() != 4)) {
            throw new Exception("Invalid week string: " + weekStr);
        }
    
        // Get monday of week 1.
        // The first week of a year is the one that includes the first Thursday of the year.
        // http://www.fourmilab.ch/documents/calendar/
        // http://joda-time.sourceforge.net/cal_iso.html
        String day1InYear = headTail[0] + "-01-01";
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        result = fmt.parseDateTime(day1InYear);
        if (result.getDayOfWeek() <= DateTimeConstants.THURSDAY) {
            while (result.getDayOfWeek() != DateTimeConstants.MONDAY) {
                result = result.minusDays(1);
            }
        } else {
            while (result.getDayOfWeek() != DateTimeConstants.MONDAY) {
                result = result.plusDays(1);
            }
        }
    
        // Add week number
        int week = Integer.parseInt(headTail[1]);
        if ((week < 1) || (week > 53)) {
            throw new Exception("Invalid week string: " + weekStr);
        }
        result = result.plusDays(7*(week-1));
        
        return result;
    }

}
