package se.sitic.megatron.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * List that handles Interval-objects.
 */
public class IntervalList {
    private ArrayList<Interval> intervals;
    private boolean sorted = false;
    
    
    public IntervalList() {
        this.intervals = new ArrayList<Interval>();
    }

    
    public IntervalList(IntervalList intervalList) {
        this.intervals = new ArrayList<Interval>(intervalList.intervals);
        this.sorted = intervalList.sorted;
    }

    
    public void add(Interval interval) {
        intervals.add(interval);
        sorted = false;
    }

    
    public void sort() {
        if (!sorted) {
            Collections.sort(intervals);
            sorted = true;
        }
    }

    
    /**
     * Returns true if two or more intervals in this list intersects.
     */
    public boolean containsOverlaps() {
        if (intervals.size() <= 1) {
            return false;
        }
        
        sort();

        boolean result = false;
        for (int i = 0; !result && (i < intervals.size()); i++) {
            if (i > 0) {
                result = intervals.get(i).overlaps(intervals.get(i-1));
            }
            if (!result && (i < (intervals.size()-1))) {
                result = intervals.get(i).overlaps(intervals.get(i+1));
            }
        }
        
        return result;
    }

    
    /**
     * Returns first interval that overlaps with specified interval, or 
     * null if not found. 
     */
    public Interval findFirstInterval(Interval interval) {
        Interval result = null;

        for (Iterator<Interval> iterator = intervals.iterator(); (result == null) && iterator.hasNext(); ) {
            Interval candidate = iterator.next();
            if (candidate.overlaps(interval)) {
                result = candidate; 
            }
        }
        return result;
    }

    
    /**
     * Returns first interval in this list that contains specified value, or 
     * null if not found. 
     */
    public Interval findFirstInterval(long val) {
        List<Interval> intervals = findIntervalsInternal(val, true);
        return (intervals != null) ? intervals.get(0) : null;
    }

    
    /**
     * Returns intervals in this list that contains specified value.
     * If overlapping intervals exists, more than one element may
     * be returned. 
     */
    public List<Interval> findIntervals(long val) {
        List<Interval> intervals = findIntervalsInternal(val, false);
        return (intervals != null) ? intervals : new ArrayList<Interval>();
    }
    
    
    /**
     * Returns number of intervals specified value is a member of.
     */
    public int findNoOfIntervals(long val) {
        List<Interval> intervals = findIntervalsInternal(val, false);
        return (intervals != null) ? intervals.size() : 0;
    }
    
    
    @Override
    public String toString() {
        return (intervals != null) ? intervals.toString() : "null";
    }

    
    private List<Interval> findIntervalsInternal(long val, boolean onlyOneInterval) {
        if (intervals.size() <= 1) {
            if ((intervals.size() == 1) && intervals.get(0).contains(val)) {
                return Collections.singletonList(intervals.get(0));
            }
            return null;
        }

        sort();

        // Linear search with NO stop condition (just to be sure) 
        List<Interval> result = null;
        for (Iterator<Interval> iterator = intervals.iterator(); iterator.hasNext(); ) {
            Interval interval = iterator.next();
            if (interval.contains(val)) {
                if (result == null) {
                    result = new ArrayList<Interval>();
                }
                result.add(interval);
                if (onlyOneInterval) {
                    break;
                }
            }
        }
        
// Code block below seem to work, but the stop condition does not add a significant speed-up.  
//        // Linear search with stop condition (binary search was too complicated to implement)  
//        List<Interval> result = null;
//        Iterator<Interval> iterator = intervals.iterator();
//        Interval prevInterval = iterator.next();
//        if (prevInterval.contains(val)) {
//            if (result == null) {
//                result = new ArrayList<Interval>();
//            }
//            result.add(prevInterval);
//        }
//        boolean quit = false;
//        while (!quit && iterator.hasNext()) {
//            Interval interval = iterator.next();
//            if (interval.contains(val)) {
//                if (result == null) {
//                    result = new ArrayList<Interval>();
//                }
//                result.add(interval);
//            } else {
//                // is value in between of two intervals?
//                if ((prevInterval != null) && (prevInterval.getEnd() < val) && (val < interval.getStart())) {
//                    quit = true;
//                }
//            }
//            prevInterval = interval;
//        }

        return result;
    }
    
}
