package se.sitic.megatron.fileprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.IpAddressUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Extracts hostnames or IP addresses from the input file, and makes DNS lookups
 * respective reverse DNS lookups in multiple threads to improve performance.
 * <p>
 * The result is saved in a map, which is used by IpAddressDecorator and 
 * HostnameDecorator.
 * <p>
 * TODO: Add support to write the result (IP address or hostname) in the file 
 * instead of storing the result in memory. 
 */
public class MultithreadedDnsProcessor implements IFileProcessor {
    /** Key for dnsMap in JobContext.additionalData */
    public static final String DNS_MAP_KEY = "MultithreadedDnsProcessor.dnsMap";
    /** Key for reverseDnsMap in JobContext.additionalData */
    public static final String REVERSE_DNS_MAP_KEY = "MultithreadedDnsProcessor.reverseDnsMap";
    
    // protected due to acess from innner class
    protected static final Logger log = Logger.getLogger(MultithreadedDnsProcessor.class);

    private static final String END_ITEM_MARKER = "---- End of Queue ----";
    
    // protected due to acess from innner class 
    protected CountDownLatch allThreadsFinishedLatch;
    protected BlockingQueue<String> queue;
    protected Map<String, Long> dnsMap;
    protected Map<Long, String> reverseDnsMap;
    
    private JobContext jobContext;
    private TypedProperties props;
    private int noOfThreads;
    private Matcher matcher;
    private Set<String> processedItems;
    private long printProgressInterval;
    private long lastProgressPrintTime;
    private long lastProgressPrintLineNo;
    private long noOfProcessedItems;
    private long noOfUniqueProcessedItems;
    
    
    public MultithreadedDnsProcessor() {
        // empty
    }
    
    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        this.jobContext = jobContext; 
        props = jobContext.getProps();

        // -- Setup result maps, queue, matcher etc.
        int initialCapacity = (int)jobContext.getNoOfLines();
        if (initialCapacity < 16) {
            initialCapacity = 32;
        }
        processedItems = new HashSet<String>(initialCapacity); 
        queue = new ArrayBlockingQueue<String>(256);
        boolean reverseDnsLookup = props.getBoolean(AppProperties.FILE_PROCESSOR_DNS_REVERSE_DNS_LOOKUP_KEY, true);
        String regExp = null;
        if (reverseDnsLookup) {
            reverseDnsMap = Collections.synchronizedMap(new HashMap<Long, String>(initialCapacity));
            jobContext.addAdditionalData(REVERSE_DNS_MAP_KEY, reverseDnsMap);
            regExp = props.getString(AppProperties.FILE_PROCESSOR_DNS_REG_EXP_IP_KEY, null);
            if (StringUtil.isNullOrEmpty(regExp)) {
                throw new MegatronException("Regular expression not defined: " + AppProperties.FILE_PROCESSOR_DNS_REG_EXP_IP_KEY);
            }
        } else {
            dnsMap = Collections.synchronizedMap(new HashMap<String, Long>(initialCapacity));
            jobContext.addAdditionalData(DNS_MAP_KEY, dnsMap);
            regExp = props.getString(AppProperties.FILE_PROCESSOR_DNS_REG_EXP_HOSTNAME_KEY, null);
            if (StringUtil.isNullOrEmpty(regExp)) {
                throw new MegatronException("Regular expression not defined: " + AppProperties.FILE_PROCESSOR_DNS_REG_EXP_HOSTNAME_KEY);
            }
        }
        try {
            matcher = Pattern.compile(regExp).matcher("");
        } catch (PatternSyntaxException e) {
            String msg = "Cannot compile reg-exp: " + regExp; 
            throw new MegatronException(msg, e);
        }
        // init attributes for printProgress 
        lastProgressPrintTime = System.currentTimeMillis(); 
        lastProgressPrintLineNo = 0L;
        printProgressInterval = 1000L*props.getLong(AppProperties.PRINT_PROGRESS_INTERVAL_KEY, 15L);

        // -- Setup threads
        noOfThreads = props.getInt(AppProperties.FILE_PROCESSOR_DNS_NO_OF_THREADS_KEY, 100);
        if (jobContext.getNoOfLines() < 10L) {
            noOfThreads = 4;
        }
        allThreadsFinishedLatch = new CountDownLatch(noOfThreads);
        for (int i = 0; i < noOfThreads; i++) {
            Thread thread = null;
            if (reverseDnsLookup) {
                thread = new Thread(new ReverseDnsLookupConsumer());
            } else {
                thread = new Thread(new DnsLookupConsumer());
            }
            String name = "Consumer-" + i;
            thread.setName(name);
            log.debug("Starting DNS lookup consumer thread: " + name);
            thread.start();
        }
    }

    
    @Override
    public File execute(File inputFile) throws MegatronException {
        long t1 = System.currentTimeMillis();

        // -- Read file
        BufferedReader in = null;
        try {
            String charSet = props.getString(AppProperties.INPUT_CHAR_SET_KEY, Constants.UTF8);
            in = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), charSet));
            long lineNo = 0L;
            String line = null;
            while ((line = in.readLine()) != null) {
                matcher.reset(line);
                while (matcher.find()) {
                    if (matcher.groupCount() > 0) {
                        for (int i = 1; i <= matcher.groupCount(); i++) {
                            processItem(matcher.group(i));    
                        }
                    } else {
                        processItem(matcher.group());
                    }
                }
                ++lineNo;
                printProgress(lineNo - queue.size());
            }
            if ((noOfProcessedItems == 0L) && (lineNo > 10L)) {
                throw new MegatronException("No IP addresses or hostnames extracted. Please check regular expression: " +
                        AppProperties.FILE_PROCESSOR_DNS_REG_EXP_IP_KEY + " or " + AppProperties.FILE_PROCESSOR_DNS_REG_EXP_HOSTNAME_KEY);
            }
        } catch (IOException e) {
            String msg = "Cannot read file: " + inputFile.getAbsolutePath();
            throw new MegatronException(msg, e);
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
        }
        
        
        // -- Add "end of queue" items
        for (int i = 0; i < noOfThreads; i++) {
            try {
                queue.put(END_ITEM_MARKER);
            } catch (InterruptedException e) {
                throw new MegatronException("Cannot put 'end of queue' marker in queue (should not happen)", e);
            }
        }
        
        // -- Wait for threads to finish
        log.debug("All items added to queue; waiting for threads to finish."); 
        try {
            allThreadsFinishedLatch.await();
        } catch (InterruptedException e) {
            throw new MegatronException("Wait for all threads to finish interrupted (should not happen)", e);
        }
        
        String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - t1);
        log.info("Total time for DNS lookups: " + durationStr);

        return inputFile;
    }

    
    @Override
    public void close(boolean jobSuccessful) throws MegatronException {
        if (dnsMap != null) {
            log.info("No. of parsed hostnames for DNS lookup [total / total unique]: " + noOfProcessedItems + " / " + noOfUniqueProcessedItems);
            log.info("No. of DNS lookups (hostname --> ip) [successful / failed]: " + dnsMap.size() + " / " + (noOfUniqueProcessedItems - dnsMap.size()));
        } else {
            log.info("No. of parsed IP addresses for reverse DNS lookup [total / total unique]: " + noOfProcessedItems + " / " + noOfUniqueProcessedItems);
            log.info("No. of DNS lookups (ip --> hostname) [successful / failed]: " + reverseDnsMap.size() + " / " + (noOfUniqueProcessedItems - reverseDnsMap.size()));
        }
    }

    
    /**
     * Process specified IP address or hostname.
     */
    private void processItem(String itemStr) throws MegatronException {
        if (StringUtil.isNullOrEmpty(itemStr)) {
            return;
        }
        ++noOfProcessedItems;
        if (processedItems.contains(itemStr)) {
            return;
        }
        ++noOfUniqueProcessedItems;
        
        processedItems.add(itemStr);
        try {
            queue.put(itemStr);
        } catch (InterruptedException e) {
            throw new MegatronException("Cannot put DNS item in queue (should not happen)", e);
        }
    }
  
    
    private void printProgress(long lineNo) {
        long now = System.currentTimeMillis();
        if ((printProgressInterval > 0L) && ((lastProgressPrintTime + printProgressInterval) < now)) {
            long noOfLines = jobContext.getNoOfLines();
            double progress = 100d*((double)lineNo / (double)noOfLines); 
            double linesPerSecond = ((double)lineNo - (double)lastProgressPrintLineNo) / (((double)now - (double)lastProgressPrintTime) / 1000d);
            DecimalFormat format = new DecimalFormat("0.00");
            String progressStr = format.format(progress);
            String lineInfoStr = lineNo + " of " + noOfLines + " lines";
            String linesPerSecondStr = format.format(linesPerSecond);
            String msg = "DNS Prefetch: " + progressStr + "% (" + lineInfoStr + ", " + linesPerSecondStr + " lines/second)"; 
            if (props.isStdout()) {
                log.info(msg);
            } else {
                jobContext.writeToConsole(msg);
            }
            lastProgressPrintLineNo = lineNo;
            lastProgressPrintTime = now;
        }
    }


    /**
     * Takes an item (hostname or IP address) from queue and process it 
     * (DNS lookup or reverse DNS lookup). 
     */
    private abstract class AbstractConsumer implements Runnable {

        
        public AbstractConsumer() {
            // empty
        }
        
        
        @Override
        public void run() {
            try {
                while (true) { 
                    String itemStr = queue.take();
                    if (itemStr == END_ITEM_MARKER) {
                        break;
                    }
                    processItem(itemStr);
                }
            } catch (InterruptedException e) {
                log.error("DNS lookup or reverse DNS lookup failed; thread interrupted (should not happened).", e);
            }
            
            allThreadsFinishedLatch.countDown();
            log.debug("Thread is exiting: " + Thread.currentThread().getName());
        }
        
        
        protected abstract void processItem(String itemStr);
        
    }

    
    /**
     * Makes a DNS lookup. 
     */
    private class DnsLookupConsumer extends AbstractConsumer {
       

        public DnsLookupConsumer() {
            // empty
        }
        
        
        @Override
        protected void processItem(String itemStr) {
            long ipAddress = IpAddressUtil.dnsLookup(itemStr);
            if (ipAddress != 0L) {
                dnsMap.put(itemStr, ipAddress);
            }
        }
        
    }

    
    /**
     * Makes a reverse DNS lookup. 
     */
    private class ReverseDnsLookupConsumer extends AbstractConsumer {
        

        public ReverseDnsLookupConsumer() {
            // empty
        }
        
        
        @Override
        protected void processItem(String itemStr) {
            try {
                long ipAddress = IpAddressUtil.convertIpAddress(itemStr);
                String hostname = IpAddressUtil.reverseDnsLookupWithoutCache(ipAddress);
                if (!StringUtil.isNullOrEmpty(hostname)) {
                    reverseDnsMap.put(ipAddress, hostname);
                }
            } catch (UnknownHostException e) {
                log.warn("Cannot convert hostname to IP address: " + itemStr);
            }
        }
        
    }


}
