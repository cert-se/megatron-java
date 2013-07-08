package se.sitic.megatron.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.NameValuePair;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.IpAddressUtil;


/**
 * Imports a BGP dump file to the database.
 *  
 * Command to dump the whole BGP table:
 * ssh -lmegatron rdist01 "sh ip bgp" > ./bgp.table
 * <p>
 * Command to show path for a specific IP:
 * sh ip bgp 192.121.23.2
 */
public class ImportBgpTable {
    private static final Logger log = Logger.getLogger(ImportBgpTable.class);
    
    // ^.{1,3}(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(?:/\d*|))(.*?)$
    private static final String LINE_REG_EXP = "^.{1,3}(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(?:/\\d*|))(.*?)$";
    // reg-exp below inclues "?"-lines but not AS in "{}"
    private static final String LINE_SUFFIX_REG_EXP = "(\\d+) .{1}$";
    private static final String MASK_REG_EXP = "/(\\d+)$";

    private static final String[] PREFIX_KILL_ARRAY = { "0.0.0.0", };
    private static final List<String> PREFIX_KILL_LIST = new ArrayList<String>(Arrays.asList(PREFIX_KILL_ARRAY));
    
    private TypedProperties props;
    private Matcher lineMatcher;
    private Matcher lineSuffixMatcher;
    private Matcher maskMatcher;
    private AsnLookupDbManager dbManager;
    
    
    public ImportBgpTable(TypedProperties props) {
        this.props = props;
        
        lineMatcher = Pattern.compile(LINE_REG_EXP).matcher("");
        lineSuffixMatcher = Pattern.compile(LINE_SUFFIX_REG_EXP).matcher("");
        maskMatcher = Pattern.compile(MASK_REG_EXP).matcher("");
    }

    
    public void importFile() throws MegatronException {
        String filename = props.getString(AppProperties.BGP_IMPORT_FILE_KEY, "bgp-table.txt");
        File file = new File(filename);
        log.info("Importing BGP table from file: " + file.getAbsolutePath());
        int noOfImportedLines = 0;
        int noOfSkippedLines = 0;
        BufferedReader in = null;
        try {
            dbManager = new AsnLookupDbManager(props);
            dbManager.deleteAsnLookupData();
            addHardCodedPrefixes();
            
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Constants.UTF8));
            String line = null;
            while ((line = in.readLine()) != null) {
                if (processLine(line)) {
                    ++noOfImportedLines;
                } else {
                    ++noOfSkippedLines;
                }
            }
            log.info("Import finished. No. of imported line: " + noOfImportedLines + ", no. of skipped lines: " + noOfSkippedLines);
        } catch (IOException e) {
            String msg = "Cannot read file: " + file.getAbsolutePath();
            throw new MegatronException(msg, e);
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (dbManager != null) dbManager.close(); } catch (Exception ignored) {}
        }
    }
    
    
    private int addHardCodedPrefixes() throws MegatronException {
        String propKey = AppProperties.BGP_HARD_CODED_PREFIXES_KEY;
        List<NameValuePair> nvList = props.getNameValuePairList(propKey, null);
        if (nvList == null) {
            return 0;
        }
        
        int result = 0;
        for (Iterator<NameValuePair> iterator = nvList.iterator(); iterator.hasNext(); ) {
            NameValuePair nv = iterator.next();
            String bgpPrefix = nv.getName();
            String asnStr = nv.getValue();
            long[] ipRange = IpAddressUtil.convertBgpPrefix(bgpPrefix);
            long asn = 0L;
            try {
                asn = Long.parseLong(asnStr);
            } catch (NumberFormatException e) {
                throw new MegatronException("Cannot parse ASN for property: " + propKey, e);
            }
            log.info("Adding hard coded BGP prefix: " + bgpPrefix + " --> " + asnStr);
            dbManager.addAsnLookup(ipRange[0], ipRange[1], asn);
            ++result;
        }

        return result;
    }
    
    
    private boolean processLine(String line) throws MegatronException {
        boolean result = false;
        lineMatcher.reset(line);
        if (lineMatcher.find() && (lineMatcher.groupCount() == 2)) {
            String bgpPrefix = lineMatcher.group(1);
            int mask = -1;
            maskMatcher.reset(bgpPrefix);
            if (maskMatcher.find() && (maskMatcher.groupCount() == 1)) {
                try {
                    mask = Integer.parseInt(maskMatcher.group(1));
                } catch (NumberFormatException e) {
                    // convertBgpPrefix will throw exception
                    mask = -1;
                }
            }
            if (PREFIX_KILL_LIST.contains(bgpPrefix)) {
                log.info("Line skipped (is in kill list): " + line);
            } else if ((mask != -1) && ((mask <= 1) || (mask >= 25))) { 
                log.info("Line skipped (mask out of range): " + line);
            } else {
                long[] ipRange = IpAddressUtil.convertBgpPrefix(bgpPrefix);
                lineSuffixMatcher.reset(line);
                if (lineSuffixMatcher.find() && (lineSuffixMatcher.groupCount() == 1)) {
                    String asnStr = lineSuffixMatcher.group(1);
                    try {
                        long asn = Long.parseLong(asnStr);
                        log.debug("Adding prefix: " + bgpPrefix);
                        dbManager.addAsnLookup(ipRange[0], ipRange[1], asn);
                        result = true;
                    } catch (NumberFormatException e) {
                        throw new MegatronException("Cannot convert ASN.", e);
                    } catch (DbException e) {
                        if ((e.getCause() != null) && e.getCause().getMessage().startsWith("Duplicate entry")) {
                            log.info("Line skipped (BGP-prefix already exist in the database): " + line);
                        } else {
                            throw e;
                        }
                    }
                } else {
                    log.debug("Line skipped (does not match suffix reg-exp): " + line);
                }
            }
        } else {
            log.debug("Line skipped (does not match line reg-exp): " + line);
        }
        return result;
    }
    
}
