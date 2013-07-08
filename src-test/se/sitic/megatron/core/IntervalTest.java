package se.sitic.megatron.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;


/**
 * JUnit test.
 */
public class IntervalTest {

    
    @Test
    public void listTest() throws MegatronException {
        String[] intervalArray = { "1-5", "1-2", "-4", "-5", "4-10", "11-15", "12-16", "11-17", "16-", "17-", "1-4", "1-5", "1-6", "0-6" };
        String sortedIntervals = "[-4, -5, 0-6, 1-2, 1-4, 1-5, 1-5, 1-6, 4-10, 11-15, 11-17, 12-16, 16-, 17-]";
        
        List<String> intervals = new ArrayList<String>(Arrays.asList(intervalArray));
        Collections.shuffle(intervals);
        IntervalList intervalList = new IntervalList();
        for (Iterator<String> iterator = intervals.iterator(); iterator.hasNext(); ) {
            try {
                intervalList.add(new Interval(iterator.next()));
            } catch (MegatronException e) {
                fail(e.getMessage());
            }
        }
        intervalList.sort();
        assertEquals(sortedIntervals, intervalList.toString());

        assertEquals("-4", intervalList.findFirstInterval(2).toString());
        assertEquals(8, intervalList.findNoOfIntervals(2));
        assertEquals(3, intervalList.findNoOfIntervals(16));
        assertEquals(3, intervalList.findNoOfIntervals(17));
        assertEquals(2, intervalList.findNoOfIntervals(18));

        String[] array2 = { "1-5", "7-9", "11-15", "11-17", "20-20", "25-" };
        intervals = new ArrayList<String>(Arrays.asList(array2));
        Collections.shuffle(intervals);
        intervalList = new IntervalList();
        for (Iterator<String> iterator = intervals.iterator(); iterator.hasNext(); ) {
            try {
                intervalList.add(new Interval(iterator.next()));
            } catch (MegatronException e) {
                fail(e.getMessage());
            }
        }
        assertEquals("[]", intervalList.findIntervals(0).toString());
        assertEquals("[1-5]", intervalList.findIntervals(1).toString());
        assertEquals("[1-5]", intervalList.findIntervals(2).toString());
        assertEquals("[1-5]", intervalList.findIntervals(5).toString());
        assertEquals("[]", intervalList.findIntervals(6).toString());
        assertEquals("[7-9]", intervalList.findIntervals(7).toString());
        assertEquals("[]", intervalList.findIntervals(10).toString());
        assertEquals("[11-15, 11-17]", intervalList.findIntervals(11).toString());
        assertEquals("[11-15, 11-17]", intervalList.findIntervals(12).toString());
        assertEquals("[11-15, 11-17]", intervalList.findIntervals(15).toString());
        assertEquals("[11-17]", intervalList.findIntervals(16).toString());
        assertEquals("[11-17]", intervalList.findIntervals(17).toString());
        assertEquals("[]", intervalList.findIntervals(18).toString());
        assertEquals("[]", intervalList.findIntervals(19).toString());
        assertEquals("[20-20]", intervalList.findIntervals(20).toString());
        assertEquals("[]", intervalList.findIntervals(21).toString());
        assertEquals("[]", intervalList.findIntervals(24).toString());
        assertEquals("[25-]", intervalList.findIntervals(25).toString());
        assertEquals("[25-]", intervalList.findIntervals(26).toString());
        assertEquals("[25-]", intervalList.findIntervals(100).toString());
    
        assertEquals("7-9", intervalList.findFirstInterval(new Interval("8-8")).toString());
        assertEquals("7-9", intervalList.findFirstInterval(new Interval("6-10")).toString());
        assertEquals("7-9", intervalList.findFirstInterval(new Interval("8-10")).toString());
        assertEquals("7-9", intervalList.findFirstInterval(new Interval("6-8")).toString());
        assertEquals(null, intervalList.findFirstInterval(new Interval("6-6")));
        assertEquals(null, intervalList.findFirstInterval(new Interval("10-10")));
        assertEquals(null, intervalList.findFirstInterval(new Interval("18-19")));
    }

    
    @Test
    public void parseTest() {
        String[] invalidIntervals = { "-x", "x-", "4242", "x", "", "-", "- ", "42-x"  };
        for (int i = 0; i < invalidIntervals.length; i++) {
            try {
                new Interval(invalidIntervals[i]);
                fail("Interval should not have been parsed.");
            } catch (MegatronException e) {
                // ignore
            }
        }

        String[] validIntervals = { "-5", "5-", "42-42" };
        for (int i = 0; i < validIntervals.length; i++) {
            try {
                new Interval(validIntervals[i]);
            } catch (MegatronException e) {
                fail("Interval should have been parsed.");
            }
        }
    }
    
    
    @Test
    public void overlapsTest() {
        String[] nonOverlappingArray = { "1-5", "6-7", "9-11", "15-15", "16-16", "18-25", "26-" };
        String[] nonOverlappingArray2 = { "1-5" };
        String[] nonOverlappingArray3 = { };
        String[] nonOverlappingArray4 = { "-1", "2-3", "5-10", "11-15" };
        String[] nonOverlappingArray5 = { "-1", "2-"};
        
        String[] overlappingArray = { "1-5", "5-6" };
        String[] overlappingArray2 = { "-1", "1-" };
        String[] overlappingArray3 = { "1-5", "10-15", "10-12" };
        String[] overlappingArray4 = { "1-5", "10-15", "10-17" };
        String[] overlappingArray5 = { "1-5", "10-15", "11-17" };
        String[] overlappingArray6 = { "1-5", "10-15", "11-11" };
        String[] overlappingArray7 = { "1-2", "2-3" };
        String[] overlappingArray8 = { "1-2", "2-3", "5-6", "10-15" };
        String[] overlappingArray9 = { "1-5", "3-4", "6-8", "10-15" };
        String[] overlappingArray10 = { "1-2", "3-4", "6-8", "7-10" };
        String[] overlappingArray11 = { "1-2", "3-4", "6-12", "8-9" };

        try {
            assertFalse(createIntervalList(nonOverlappingArray).containsOverlaps());

            assertEquals(1, createIntervalList(nonOverlappingArray).findNoOfIntervals(15));
            assertEquals(new Interval(15, 15), createIntervalList(nonOverlappingArray).findFirstInterval(15));
            assertEquals(new Interval(15, 15), createIntervalList(nonOverlappingArray).findIntervals(15).get(0));
            assertEquals(1, createIntervalList(nonOverlappingArray).findNoOfIntervals(20));
            assertEquals(new Interval(18, 25), createIntervalList(nonOverlappingArray).findFirstInterval(20));
            assertEquals(new Interval(18, 25), createIntervalList(nonOverlappingArray).findIntervals(20).get(0));
            
            assertFalse(createIntervalList(nonOverlappingArray2).containsOverlaps());
            assertFalse(createIntervalList(nonOverlappingArray3).containsOverlaps());
            assertFalse(createIntervalList(nonOverlappingArray4).containsOverlaps());
            assertFalse(createIntervalList(nonOverlappingArray5).containsOverlaps());

            assertTrue(createIntervalList(overlappingArray).containsOverlaps());
            assertTrue(createIntervalList(overlappingArray2).containsOverlaps());
            assertTrue(createIntervalList(overlappingArray3).containsOverlaps());
            assertTrue(createIntervalList(overlappingArray4).containsOverlaps());
            assertTrue(createIntervalList(overlappingArray5).containsOverlaps());
            assertTrue(createIntervalList(overlappingArray6).containsOverlaps());
            assertTrue(createIntervalList(overlappingArray7).containsOverlaps());
            assertTrue(createIntervalList(overlappingArray8).containsOverlaps());
            assertTrue(createIntervalList(overlappingArray9).containsOverlaps());
            assertTrue(createIntervalList(overlappingArray10).containsOverlaps());
            assertTrue(createIntervalList(overlappingArray11).containsOverlaps());

            assertEquals(2, createIntervalList(overlappingArray11).findNoOfIntervals(9));
            assertEquals(new Interval(6, 12), createIntervalList(overlappingArray11).findFirstInterval(9));
            assertEquals(new Interval(6, 12), createIntervalList(overlappingArray11).findIntervals(9).get(0));
            assertEquals(0, createIntervalList(overlappingArray11).findNoOfIntervals(20));
            assertEquals(1, createIntervalList(overlappingArray11).findNoOfIntervals(10));
            assertEquals(new Interval(1, 2), createIntervalList(overlappingArray11).findFirstInterval(1));
            assertEquals(new Interval(1, 2), createIntervalList(overlappingArray11).findIntervals(1).get(0));
            assertEquals(new Interval(1, 2), createIntervalList(overlappingArray11).findIntervals(2).get(0));
        } catch (MegatronException e) {
            fail("Interval should have been parsed.");
        }
    }
    
    
    private IntervalList createIntervalList(String[] intervalArray) throws MegatronException {
        List<String> intervals = new ArrayList<String>(Arrays.asList(intervalArray));
        Collections.shuffle(intervals);
        IntervalList result = new IntervalList();
        for (Iterator<String> iterator = intervals.iterator(); iterator.hasNext(); ) {
            result.add(new Interval(iterator.next()));
        }
        return result;
    }

}
