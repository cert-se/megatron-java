package se.sitic.megatron.decorator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.Interval;
import se.sitic.megatron.core.IntervalList;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.db.DbException;
import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.ASNumber;
import se.sitic.megatron.entity.DomainName;
import se.sitic.megatron.entity.IpRange;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.ObjectStringSorter;
import se.sitic.megatron.util.StringUtil;


/**
 * Adds organization id for primary- and secondary organization matching the
 * following:<ul>
 * <li>IP ranges
 * <li>ASNs
 * <li>hostnames
 * </ul>
 */
public class OrganizationMatcherDecorator implements IDecorator {
    private static final Logger log = Logger.getLogger(OrganizationMatcherDecorator.class);

    // -- cache handling
    private static final long CACHE_TTL = 30*60*1000L;
    private static final Object cacheMutex = new Object();
    private static Map<Long, ASNumber> asnMapCached;
    private static Map<String, DomainName> domainNameMapCached;
    private static IntervalList ipIntervalsCached;
    private static long lastCacheFetch;
    
    private JobContext jobContext;
    private Map<Long, ASNumber> asnMap;
    private Map<String, DomainName> domainNameMap;
    private IntervalList ipIntervals;
    private boolean matchIpAddress;
    private boolean matchHostname;
    private boolean matchAsn;
    private boolean checkHighPriorityOrganization;
    
    private long noOfOrganizationsFound;
    private long noOfOrganizationsFound2;
    private long noOfHighPriorityOrganizationsFound;
    private long noOfHighPriorityOrganizationsFound2;

    private List<Organization> highPriorityOrganizations;
    private int highPriorityThreshold;
    
    
    public OrganizationMatcherDecorator() {
        // empty
    }

    
    public void init(JobContext jobContext) throws MegatronException {
        this.jobContext = jobContext;
        init();
    }


    public void execute(LogEntry logEntry) throws MegatronException {
        int orgId = 0;
        boolean highPriorityEntryFound = false;
        DbManager dbManager = jobContext.getDbManager();
        
        orgId = findOrganizationId(logEntry.getIpAddress(), logEntry.getHostname(), logEntry.getAsn());
        if (orgId == -1) {
            orgId = findOrganizationId(logEntry.getIpRangeStart(), logEntry.getIpRangeEnd()); 
        }
        if (orgId != -1) {           
            Organization organization = dbManager.getOrganization(orgId);
            logEntry.setOrganization(organization);
            ++noOfOrganizationsFound;
            if (isHighPriorityOrganization(organization)) {
                addHighPriorityOrganization(organization);
                ++noOfHighPriorityOrganizationsFound;
                highPriorityEntryFound = true;
            }
        }

        orgId = findOrganizationId(logEntry.getIpAddress2(), logEntry.getHostname2(), logEntry.getAsn2());
        if (orgId != -1) {
        	Organization organization2 = dbManager.getOrganization(orgId);
            logEntry.setOrganization2(organization2);
            ++noOfOrganizationsFound2;
            if (isHighPriorityOrganization(organization2)) {
                addHighPriorityOrganization(organization2);
                ++noOfHighPriorityOrganizationsFound2;
                highPriorityEntryFound = true;
            }
        }
        
        if (highPriorityEntryFound) {
            jobContext.incNoOfHighPriorityEntries(1);
        }
    }

    
    public void close() throws MegatronException {
        long noOfTotalOrganizationsFound = noOfOrganizationsFound + noOfOrganizationsFound2;
        log.info("No. of organizations found: " + noOfTotalOrganizationsFound + " (" + noOfOrganizationsFound + "+" + noOfOrganizationsFound2 + ")."); 

        long noOfTotalHighPriosFound = noOfHighPriorityOrganizationsFound + noOfHighPriorityOrganizationsFound2;
        log.info("No. of high priority organizations found: " + noOfTotalHighPriosFound + " (" + noOfHighPriorityOrganizationsFound + "+" + noOfHighPriorityOrganizationsFound2 + ")."); 
    }

    
    public List<Organization> getHighPriorityOrganizations() {
        ObjectStringSorter prioritySorter = new ObjectStringSorter() {
                @Override
                protected String getObjectString(Object obj) {
                    if (obj == null) {
                        return null;
                    }
                    Integer prio = ((Organization)obj).getPriority().getPrio();
                    return (prio != null) ? prio.toString() : null;
                }
            };
        Collections.sort(highPriorityOrganizations, prioritySorter);
        Collections.reverse(highPriorityOrganizations);
        return highPriorityOrganizations;
    }

    
    public void setMatchIpAddress(boolean matchIpAddress) {
        this.matchIpAddress = matchIpAddress;
    }


    public void setMatchHostname(boolean matchHostname) {
        this.matchHostname = matchHostname;
    }


    public void setMatchAsn(boolean matchAsn) {
        this.matchAsn = matchAsn;
    }


    public void setCheckHighPriorityOrganization(boolean checkHighPriorityOrganization) {
        this.checkHighPriorityOrganization = checkHighPriorityOrganization;
    }


    private static void initCache(DbManager dbManager) throws DbException {
        long t1 = System.currentTimeMillis();
        if (lastCacheFetch + CACHE_TTL < t1) {
            boolean success = false;
            try {
                // asnMap
                asnMapCached = new HashMap<Long, ASNumber>();
                List<ASNumber> asNumbers = dbManager.getAllASNumbers(false);
                for (Iterator<ASNumber> iterator = asNumbers.iterator(); iterator.hasNext(); ) {
                    ASNumber asNumber = iterator.next();
                    asnMapCached.put(asNumber.getNumber(), asNumber);
                }
                log.info("All ASNs read from db. Size: " + asnMapCached.size());
                
                // domainNameMap
                domainNameMapCached = new HashMap<String, DomainName>();
                List<DomainName> domainNames = dbManager.getAllDomainNames(false);
                for (Iterator<DomainName> iterator = domainNames.iterator(); iterator.hasNext(); ) {
                    DomainName domainName = iterator.next();
                    domainNameMapCached.put(domainName.getName().toLowerCase(), domainName);
                }
                log.info("All domain names read from db. Size: " + domainNameMapCached.size());
                
                // ipIntervals
                ipIntervalsCached = new IntervalList();
                List<IpRange> ipRanges = dbManager.getAllIpRanges(false);
                for (Iterator<IpRange> iterator = ipRanges.iterator(); iterator.hasNext(); ) {
                    IpInterval ipInterval = new IpInterval(iterator.next());
                    ipIntervalsCached.add(ipInterval);
                }
                log.info("All IP ranges read from db. Size: " + ipRanges.size());
                
                if (log.isDebugEnabled()) {
                    String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - t1);
                    log.debug("Time to read contact info from db: " + durationStr);
                }
                
                lastCacheFetch = System.currentTimeMillis();
                success = true;
            } finally {
                if (!success) {
                    lastCacheFetch = 0L;
                }
            }
        }
    }

    
    private Map<Long, ASNumber> createAsnMap() throws DbException {
        // Note: initCache will be locked by cacheMutex. A new list must be 
        // returned; asnMapCached may be in use when it's re-initialized.
        synchronized (cacheMutex) {
            initCache(jobContext.getDbManager());
            return new HashMap<Long, ASNumber>(asnMapCached);
        }
    }

    
    private Map<String, DomainName> createDomainNameMap() throws DbException {
        synchronized (cacheMutex) {
            initCache(jobContext.getDbManager());
            return new HashMap<String, DomainName>(domainNameMapCached);
        }
    }
    
    
    private IntervalList createIpIntervals() throws DbException {
        synchronized (cacheMutex) {
            initCache(jobContext.getDbManager());
            return new IntervalList(ipIntervalsCached);
        }
    }
    
    
    private void init() throws DbException {
        asnMap = createAsnMap();
        domainNameMap = createDomainNameMap();
        ipIntervals = createIpIntervals();

        highPriorityOrganizations = new ArrayList<Organization>();
        highPriorityThreshold = jobContext.getProps().getInt(AppProperties.HIGH_PRIORITY_THRESHOLD_KEY, 50);
        
        matchIpAddress = jobContext.getProps().getBoolean(AppProperties.DECORATOR_MATCH_IP_ADDRESS_KEY, true);
        matchHostname = jobContext.getProps().getBoolean(AppProperties.DECORATOR_MATCH_HOSTNAME_KEY, true);
        matchAsn = jobContext.getProps().getBoolean(AppProperties.DECORATOR_MATCH_ASN_KEY, true);
        checkHighPriorityOrganization = true;
    }

    
    private int findOrganizationId(Long ipAddress, String hostname, Long asn) {
        if (matchIpAddress && (ipAddress != null) && (ipAddress.longValue() != 0L)) {
            IpInterval ipInterval = (IpInterval)ipIntervals.findFirstInterval(ipAddress.longValue());
            if (ipInterval != null) {
                log.debug("Organization#" + ipInterval.getOrganizationId() + " found by IP: " + ipAddress);
                return ipInterval.getOrganizationId();
            }
        }
        
        String originalHostname = hostname;
        while (matchHostname && (hostname != null)) {
            DomainName domainName = domainNameMap.get(hostname.toLowerCase());
            if (domainName != null) {
                log.debug("Organization#" + domainName.getOrganizationId() + " found by hostname: " + originalHostname);
                return domainName.getOrganizationId();
            }
            hostname = extractPrefixLabel(hostname);
        }

        if (matchAsn && (asn != null) && (asn.longValue() != 0L)) {
            ASNumber asNumber = asnMap.get(asn.longValue());
            if (asNumber != null) {
                log.debug("Organization#" + asNumber.getOrganizationId() + " found by ASN: " + asn);
                return asNumber.getOrganizationId();
            }
        }

        return -1;
    }

    
    private int findOrganizationId(Long ipRangeStart, Long ipRangeEnd) {
        if (matchIpAddress && (ipRangeStart != null) && (ipRangeStart.longValue() != 0L) && (ipRangeEnd != null) && (ipRangeEnd.longValue() != 0L)) {
            Interval ipRange = new Interval(ipRangeStart.longValue(), ipRangeEnd.longValue());
            IpInterval ipInterval = (IpInterval)ipIntervals.findFirstInterval(ipRange);
            if (ipInterval != null) {
                log.debug("Organization#" + ipInterval.getOrganizationId() + " found by IP-range: " + ipRange + " overlaps with " + ipInterval);
                return ipInterval.getOrganizationId();
            }
        }
        return -1;
    }
    
    
    private boolean isHighPriorityOrganization(Organization organization) {
        if (!checkHighPriorityOrganization) {
            return false;
        }

        Integer priority = organization.getPriority().getPrio();
        boolean result = false;
        if (priority != null) {
            result = priority.intValue() >= highPriorityThreshold;
        } else {
            log.warn("Priority is missing for organization: " + organization.getName());
        }
        return result;
    }

    
    private void addHighPriorityOrganization(Organization organization) {
        if (!highPriorityOrganizations.contains(organization)) {
            highPriorityOrganizations.add(organization);
        }
    }
    
    
    /**
     * Extracts first label from specified hostname, e.g. "www.foobar.se" --> "foobar.se".
     * 
     * @return hostname - prefix label, or null if extraction not possible.
     */
    private String extractPrefixLabel(String hostname) {
        String[] headTail = StringUtil.splitHeadTail(hostname, ".", false);
        return ((headTail == null) || (headTail[1].indexOf('.') == -1)) ? null : headTail[1];
    }

    
    private static class IpInterval extends Interval {
        private int organizationId;
        
        
        public IpInterval(IpRange ipRange) {
            super(ipRange.getStartAddress(), ipRange.getEndAddress());
            organizationId = ipRange.getOrganizationId();
        }

        
        public int getOrganizationId() {
            return organizationId;
        }

    }

}
