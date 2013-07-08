package se.sitic.megatron.core;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.whois.WhoisClient;
import org.apache.log4j.Logger;

import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.IpRange;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.util.IpAddressUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Updates the field "ip_range.net_name" from whois query. 
 */
public class NetnameUpdater {
    private final Logger log = Logger.getLogger(NetnameUpdater.class);

    private static final String NETNAME_REG_EXP = "^netname:\\s+(.+)";
    private static final String MODIFIED_BY = "--update-netname";
    
    private TypedProperties props;
    private WhoisClient whoisClient;
    private Matcher matcher;

    
    public NetnameUpdater(TypedProperties props) {
        this.props = props;
        this.whoisClient = new WhoisClient();
        this.matcher = Pattern.compile(NETNAME_REG_EXP, Pattern.MULTILINE).matcher("");
    }

    
    public void update() throws MegatronException {
        log.info("Starting to update the netname field from whois queries.");
        
        int noOfWhoisQueries = 0;
        int noOfNewEntries = 0;
        int noOfModifiedEntries = 0;
        DbManager dbManager = DbManager.createDbManager(props);
        try {
            List<IpRange> ipRanges = dbManager.getAllIpRanges(true);
            for (Iterator<IpRange> iterator = ipRanges.iterator(); iterator.hasNext(); ) {
                IpRange ipRange = iterator.next();
                String ipStr = IpAddressUtil.convertIpAddress(ipRange.getStartAddress(), false);
                if (!StringUtil.isNullOrEmpty(ipStr)) {
                    String netname = null;
                    try {
                        ++noOfWhoisQueries;
                        netname = parseNetname(queryWhois(ipStr));
                    } catch (IOException e) {
                        log.error("Whois query failed for IP: " + ipStr + "(" + ipRange.getStartAddress() + ")");
                        continue;
                    }

                    if (!StringUtil.isNullOrEmpty(netname)) {
                        if (StringUtil.isNullOrEmpty(ipRange.getNetName()) || !netname.equals(ipRange.getNetName())) {
                            Organization organization = dbManager.getOrganization(ipRange.getOrganizationId());
                            String msg = null;
                            if (StringUtil.isNullOrEmpty(ipRange.getNetName())) {
                                msg = "Adding new netname for " + organization.getName() + ": " + netname + " (" + ipStr + ").";
                                ++noOfNewEntries;
                            } else {
                                msg = "Netname updated for " + organization.getName() + ": " + ipRange.getNetName() + "-->" + netname + " (" + ipStr + ").";
                                ++noOfModifiedEntries;
                            }
                            log.info(msg);
                            System.out.println(msg);
                            ipRange.setNetName(netname);
                            dbManager.updateOrganization(organization, MODIFIED_BY);
                        }
                    }
                }
            }
            String msg = "Netname updated. Whois Queries: " + noOfWhoisQueries + ", New Netnames: " + noOfNewEntries + ", Modified Netnames: " + noOfModifiedEntries;
            log.info(msg);
            System.out.println(msg);
        } finally {
            dbManager.close();
        }
    }
    
    
    private String parseNetname(String whoisResult) {
        String result = null;
        
        matcher.reset(whoisResult);
        if (matcher.find() && (matcher.groupCount() == 1)) {
            result = matcher.group(1);
        }
        
        return result;
    }

    
    private String queryWhois(String query) throws IOException {
        String result = null;
        try {
            String whoisServer = props.getString(AppProperties.WHOIS_SERVER_KEY, "whois.ripe.net");
            whoisClient.connect(whoisServer);
            result = whoisClient.query(query);
        } finally {
            whoisClient.disconnect();
        }
        return result;
    }

}
