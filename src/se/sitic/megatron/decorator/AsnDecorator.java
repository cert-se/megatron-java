package se.sitic.megatron.decorator;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.db.AsnLookupDbManager;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.AppUtil;


/**
 * Adds ASN if missing and ip-address exists.
 * <p>
 * This class is using the database for "ip --> ASN"-lookups. BGP data must
 * be imported, otherwise lookups will fail. Use AsnGeoIpDecorator if
 * BGP data are not available. 
 * <p>
 * Alternative implementation:
 * Login to BGP-router using SSH and parse result from the command
 * "sh bgb ip-address". Use "best route". ssh-libraries:<ul>
 * <li>JSch: http://www.jcraft.com/jsch/</li>
 * <li>Ganymed http://www.cleondra.ch/ssh2/</li>
 * </ul>
 * <p>
 * Using Cymru DNS service does not with so well, because ASN cannot
 * be cached using BGP-prefix. A BGP-prefix can have smaller prefixes;
 * only /24-prefixes can securely be cached since they cannot be
 * fragmented.
 */
public class AsnDecorator implements IDecorator {
    private static final Logger log = Logger.getLogger(AsnDecorator.class);

    private AsnLookupDbManager dbManager;
    private long noOfLookups;
    private long noOfLookups2;

    
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        dbManager = new AsnLookupDbManager(props);
    }    


    public void execute(LogEntry logEntry) throws MegatronException {
        List<Long> ipAddresses = AppUtil.getIpAddressesToDecorate(logEntry);
        Iterator<Long> iterator = (ipAddresses != null) ? ipAddresses.iterator() : null;
        while ((logEntry.getAsn() == null) && (iterator != null) && iterator.hasNext()) {
            long asn = dbManager.searchAsn(iterator.next());
            if (asn != -1L) {
                logEntry.setAsn(new Long(asn));
            }
            ++noOfLookups;
        }

        if ((logEntry.getAsn2() == null) && (logEntry.getIpAddress2() != null)) {
            long asn = dbManager.searchAsn(logEntry.getIpAddress2().longValue());
            if (asn != -1L) {
                logEntry.setAsn2(new Long(asn));
            }
            ++noOfLookups2;
        }
    }

    
    public void close() throws MegatronException {
        long noOfTotalLookups = noOfLookups + noOfLookups2;
        log.info("No. of lookups by AsnDecorator (ip --> asn): " + noOfTotalLookups + " (" + noOfLookups + "+" + noOfLookups2 + ")."); 

        dbManager.close();
    }

}
