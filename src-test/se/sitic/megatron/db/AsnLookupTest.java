package se.sitic.megatron.db;
import junit.framework.Assert;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.IpAddressUtil;


/**
 * JUnit test.
 */
public class AsnLookupTest {
    private static final String LOG4J_FILENAME = "conf/dev/log4j.properties";
    TypedProperties props;

    
    @Before
    public void init() throws Exception {
        PropertyConfigurator.configure(LOG4J_FILENAME);
        System.setProperty("megatron.configfile", "conf/dev/megatron-globals.properties");
        AppProperties.getInstance().init(new String[0]);
        props = AppProperties.getInstance().getGlobalProperties();
    }

    
    // @Test
    public void importBgpTable() throws Exception {
        ImportBgpTable importer = new ImportBgpTable(props);
        importer.importFile();
    }
    
    
    @Test
    public void searchAsnTest() throws Exception {
        String[][] ipToAsnArray = {
                { "202.88.47.254", "-1" },
                { "202.88.47.255", "-1" },
                { "202.88.48.0", "2519" },
                { "202.88.48.1", "2519" },
                { "202.88.48.255", "2519" },
                { "202.88.63.0", "2519" },
                { "202.88.63.254", "2519" },
                { "202.88.63.255", "2519" },
                { "202.88.64.0", "9839" },
                { "202.88.64.1", "9839" },
                { "212.116.63.254", "13170" },
                { "212.116.63.255", "13170" },
                { "212.116.64.0", "8473" },
                { "212.116.64.1", "8473" },
                { "212.116.70.255", "8473" },
                { "212.116.80.255", "8473" },
                { "212.116.90.255", "8473" },
                { "212.116.93.255", "8473" },
                { "212.116.94.254", "8473" },
                { "212.116.94.255", "8473" },
                { "212.116.95.255", "8473" },
                { "212.116.96.0", "48704" },
                { "212.116.96.1", "48704" },
                { "192.121.218.4", "41884" },
                { "192.121.211.100", "3292" },
            };

        AsnLookupDbManager dbManager = new AsnLookupDbManager(props);
        for (int i = 0; i < ipToAsnArray.length; i++) {
            String ipAddressStr = ipToAsnArray[i][0];
            long ipAddress = IpAddressUtil.convertIpAddress(ipAddressStr);
            long asn = dbManager.searchAsn(ipAddress);
            Assert.assertEquals(ipToAsnArray[i][1], "" + asn);
        }
        dbManager.close();
    }

}
