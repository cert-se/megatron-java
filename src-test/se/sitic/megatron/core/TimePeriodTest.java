package se.sitic.megatron.core;

import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import se.sitic.megatron.util.DateUtil;


/**
 * JUnit test.
 */
public class TimePeriodTest {

    @Test
    public void timePeriodTest() throws Exception {
        
        // DST: 2008-03-30–2008-10-26
        final String[][] validPeriods = {
                { "w2009-33", "2009-08-10", "2009-08-16", },
                { "w2008-33", "2008-08-11", "2008-08-17" },
                { "w2008-52", "2008-12-22", "2008-12-28" },
                { "w2009-01", "2008-12-29", "2009-01-04" },
                { "w2009-02", "2009-01-05", "2009-01-11" },
                { "w2008-13", "2008-03-24", "2008-03-30" },  // DST
                { "w2008-14", "2008-03-31", "2008-04-06" },
                { "w2008-43", "2008-10-20", "2008-10-26" },  // DST
                { "w2007-01", "2007-01-01", "2007-01-07" },
                { "w2007-52", "2007-12-24", "2007-12-30" },
                { "w2008-01", "2007-12-31", "2008-01-06" },
                { "w2008-1", "2007-12-31", "2008-01-06" },
                { "w2008-02", "2008-01-07", "2008-01-13" },
                { "w2008-22", "2008-05-26", "2008-06-01" },
                { "w2008-52", "2008-12-22", "2008-12-28" },
                { "w2009-01", "2008-12-29", "2009-01-04" },
                { "w2009-52", "2009-12-21", "2009-12-27" },
                { "w2009-53", "2009-12-28", "2010-01-03" },
                { "w2010-01", "2010-01-04", "2010-01-10" },
                { "w2010-52", "2010-12-27", "2011-01-02" },
                { "w2011-01", "2011-01-03", "2011-01-09" },
                { "w2011-02", "2011-01-10", "2011-01-16" },
                { "w2011-52", "2011-12-26", "2012-01-01" },
                { "w2012-01", "2012-01-02", "2012-01-08" },
                { "w2008-44", "2008-10-27", "2008-11-02" },
                { "w01", "2010-01-04", "2010-01-10" },
                { "w1", "2010-01-04", "2010-01-10" },
                { "w22", "2010-05-31", "2010-06-06" },
                { "w52", "2010-12-27", "2011-01-02" },
                { "2008-08-11--2008-08-17", "2008-08-11", "2008-08-17" },
                { "2008-03-29--2008-03-31", "2008-03-29", "2008-03-31" },  // DST
                { "2008-10-20--2008-10-27", "2008-10-20", "2008-10-27" },  // DST
        };

        final String[][] invalidPeriods = {
                { "w54", "dummy", "dummy", },
                { "w2011X52", "dummy", "dummy" },
                { "w", "dummy", "dummy" },
                { "w-1", "dummy", "dummy" },
                { "w0", "dummy", "dummy" },
                { "w1111", "dummy", "dummy" },
                { "2008-08-11-2008-08-17", "dummy", "dummy" },
                { "2008.08.11--2008-08-17", "dummy", "dummy" },
                { "2008-08-17--2008-08-11", "dummy", "dummy" },
        };

        for (int i = 0; i < validPeriods.length; i++) {
            String periodStr = validPeriods[i][0];
            String startDateStr = validPeriods[i][1];
            String endDateStr = validPeriods[i][2];

            TimePeriod tp = new TimePeriod(periodStr);
            System.out.println(periodStr + " --> " + tp);
            Assert.assertEquals(startDateStr, DateUtil.formatDateTime(DateUtil.DATE_FORMAT, tp.getStartDate()));
            Assert.assertEquals(endDateStr, DateUtil.formatDateTime(DateUtil.DATE_FORMAT, tp.getEndDate()));
        }

        for (int i = 0; i < invalidPeriods.length; i++) {
            String periodStr = invalidPeriods[i][0];
            try {
                TimePeriod tp = new TimePeriod(periodStr);
                fail("Period invalid and should throw exception: " + tp);
            } catch (MegatronException e) {
                // empty
            }
        }
    }

    
    // @Test
    public void jdkWeekBugTest() throws ParseException {
        // The JDK cannot parse "year of week", but Joda can.
        // For example, "2008-01" does not work but "2008-02" does.
        // More info:
        // http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=bff79f21455177ffffffffc19116dffe41396?bug_id=4267450
        
        String periodStr = "2008-01";
        
        // joda
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-ww");
        DateTime dt = fmt.parseDateTime(periodStr);
        System.out.println(periodStr + " --> " + dt.toDate());

        // jdk: will throw java.text.ParseException: Unparseable date: "2008-01"
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-ww");
        formatter.setLenient(false);
        System.out.println(periodStr + " --> " + formatter.parse(periodStr));
    }


}
