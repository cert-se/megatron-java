package se.sitic.megatron.util;

import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.MegatronException;


/**
 * JUnit test.
 */
public class IpAddressUtilTest {
    private static final String LOG4J_FILENAME = "conf/dev/log4j.properties";

    
    @Before
    public void init() throws Exception {
        PropertyConfigurator.configure(LOG4J_FILENAME);
        System.setProperty("megatron.configfile", "conf/dev/megatron-globals.properties");
        AppProperties.getInstance().init(new String[0]);
    }

    
    @Test
    public void convertIpRange() throws Exception {
        String[][] validIpRanges = {
                { "202.88.48.0/20", "202.88.48.0-202.88.63.255" },
                { "212.116.64.0/19", "212.116.64.0-212.116.95.255" },
                { "192.121.218.0/24", "192.121.218.0-192.121.218.255" },
                { "192.121.218.0/32", "192.121.218.0-192.121.218.0" },
                { "192.121.218.255/32", "192.121.218.255-192.121.218.255" },
                { "8.0.0.0", "8.0.0.0-8.0.0.0" },
                { "202.131.0.0", "202.131.0.0-202.131.0.0" },
                { "202.131.128.64", "202.131.128.64-202.131.128.64" },
                { "202.131.64.0-255", "202.131.64.0-202.131.64.255" },
                { "202.131.64.128-255", "202.131.64.128-202.131.64.255" },
                { "202.131.64.0-0", "202.131.64.0-202.131.64.0" },
                { "202.131.64.0-1", "202.131.64.0-202.131.64.1" },
                { "202.131.64.0-202.131.64.255", "202.131.64.0-202.131.64.255" },
                { "202.131.64.0-202.131.64.0", "202.131.64.0-202.131.64.0" },
                { "202.131.64.0-202.131.128.255", "202.131.64.0-202.131.128.255" },
                { "8.0.0.0/8", "8.0.0.0-8.255.255.255" },
                { "8.0.0.0/16", "8.0.0.0-8.0.255.255" },
                { "8.0.0.0/24", "8.0.0.0-8.0.0.255" },
                { " 8.0.0.0/24 ", "8.0.0.0-8.0.0.255" },
                { "8.x.x.x", "8.0.0.0-8.255.255.255" },
                { "8.x.X.x", "8.0.0.0-8.255.255.255" },
                { "8.0.x.x", "8.0.0.0-8.0.255.255" },
                { " 8.0.x.x ", "8.0.0.0-8.0.255.255" },
                { "8.0.0.x", "8.0.0.0-8.0.0.255" },
                { "8.0.0.0", "8.0.0.0-8.0.0.0" },
                { "8.101.x.x", "8.101.0.0-8.101.255.255" },
            };

        String[] invalidIpRanges = {
                "202.88.48.0/33",
                "202.88.48.0/1",
                "8.0.0.0.0",
                "8.0.0",
                "8.0.0.",
                null,
                "",
                "          ",
                "202.131.64.128-127",
                "202.131.64.0-",
                "202.131.64.255-254",
                "202.131.64.0-202.131.63.0",
                "-202.131.63.0",
                "202.131.63.0-",
                "202.131.64.0-202.131.63.0.1",
                "202.131.64.0-202.131.64",
                "202.131.64.0-202.131.63.0",
                "202.131.64.1-202.131.64.0",
                "8.x.x.xxx",
                "x.x.x.x",
                "8.x.x.x.x",
                "8.0.x.0",
                "8.0.0.x/16"
            };

        for (int i = 0; i < validIpRanges.length; i++) {
            String ipRangeStr = validIpRanges[i][0];
            long[] ipRange = IpAddressUtil.convertIpRange(ipRangeStr);
            Assert.assertEquals(validIpRanges[i][1], ipRangeToString(ipRange));
        }
        
        for (int i = 0; i < invalidIpRanges.length; i++) {
            try {
                IpAddressUtil.convertIpRange(invalidIpRanges[i]);
                fail("IP-range is invalid and should cause an exception. IP-range: " + invalidIpRanges[i]);
            } catch (MegatronException e) {
                // empty
            }
        }
        
        // expandZeroOctets
        long[] ipRange = null; 
        ipRange = IpAddressUtil.convertIpRange("8.0.0.0", true);
        Assert.assertEquals("8.0.0.0-8.255.255.255", ipRangeToString(ipRange));
        ipRange = IpAddressUtil.convertIpRange("8.0.0.0", false);
        Assert.assertEquals("8.0.0.0-8.0.0.0", ipRangeToString(ipRange));
        ipRange = IpAddressUtil.convertIpRange("8.0.0.1", true);
        Assert.assertEquals("8.0.0.1-8.0.0.1", ipRangeToString(ipRange));
        ipRange = IpAddressUtil.convertIpRange("8.101.0.0", true);
        Assert.assertEquals("8.101.0.0-8.101.255.255", ipRangeToString(ipRange));
        ipRange = IpAddressUtil.convertIpRange("8.101.102.0", true);
        Assert.assertEquals("8.101.102.0-8.101.102.255", ipRangeToString(ipRange));
        ipRange = IpAddressUtil.convertIpRange("8.101.102.101", true);
        Assert.assertEquals("8.101.102.101-8.101.102.101", ipRangeToString(ipRange));
        String[] invalidIpRanges2 = {
                "0.0.0.0",
                "8.0.0.0.0",
                "0.0",
                "0",
                "8.x.0.0"
            };
        for (int i = 0; i < invalidIpRanges2.length; i++) {
            try {
                IpAddressUtil.convertIpRange(invalidIpRanges2[i], true);
                fail("IP-range is invalid and should cause an exception. IP-range: " + invalidIpRanges2[i]);
            } catch (MegatronException e) {
                // empty
            }
        }
    }

    
    @Test
    public void convertBgpPrefix() throws Exception {
        String[][] validPrefixes = {
                { "202.88.48.0/20", "202.88.48.0-202.88.63.255" },
                { "212.116.64.0/19", "212.116.64.0-212.116.95.255" },
                { "192.121.218.0/24", "192.121.218.0-192.121.218.255" },
                { "8.0.0.0", "8.0.0.0-8.255.255.255" },
                { "202.131.0.0", "202.131.0.0-202.131.255.255" },
                { "202.131.64.0", "202.131.64.0-202.131.64.255" },
                { "202.131.64.64", "202.131.64.64-202.131.64.64" },
            };
        
        for (int i = 0; i < validPrefixes.length; i++) {
            String bgpPrefix = validPrefixes[i][0];
            long[] ipRange = IpAddressUtil.convertBgpPrefix(bgpPrefix);
            Assert.assertEquals(validPrefixes[i][1], ipRangeToString(ipRange));
        }
    }

    
    @Test
    public void convertIpAddressFromLong() throws Exception {
        InetAddress address = null;
        
        // local 192.121.218.4 3229211140 
        // (first octet * 16777216) + (second octet * 65536) + (third octet * 256) + (fourth octet) 
        address = IpAddressUtil.convertIpAddress(3229211140L);
        Assert.assertEquals("192.121.218.4", address.getHostAddress());
        Assert.assertEquals("192.121.218.4", address.getCanonicalHostName());

        // www.sunet.se 130.239.8.25 2196703257
        // 130*16777216 + 239*65536 + 8*256 + 25 = 2196703257
        address = IpAddressUtil.convertIpAddress(2196703257L);
        Assert.assertEquals("130.239.8.25", address.getHostAddress());
        Assert.assertEquals("wsunet.umdc.umu.se", address.getHostName());
        Assert.assertEquals("wsunet.umdc.umu.se", address.getCanonicalHostName());
        Assert.assertEquals("130.239.8.25 (wsunet.umdc.umu.se)", IpAddressUtil.convertIpAddress(2196703257L, true));

        // local 127.0.0.1 2130706433
        // 127*16777216 + 0*65536 + 0*256 + 1 = 2130706433
        address = IpAddressUtil.convertIpAddress(2130706433L);
        Assert.assertEquals("127.0.0.1", address.getHostAddress());
        Assert.assertEquals("127.0.0.1", address.getCanonicalHostName());
    
        // ff.ff.ff.ff = 256*16777216 + 256*65536 + 256*256 + 256 = 4311810304
        long[] invalidAddresses = { -1L, 0L, 4311810304L };
        for (int i = 0; i < invalidAddresses.length; i++) {
            try {
                address = IpAddressUtil.convertIpAddress(invalidAddresses[i]);
                fail("IP-address is invalid and should cause an exception. IP-address: " + invalidAddresses[i]);
            } catch (UnknownHostException e) {
                // empty
            }
        }
    }

    
    @Test
    public void convertIpAddressFromString() throws Exception {
        long address = 0L;
        
        address = IpAddressUtil.convertIpAddress("212.116.94.254");
        Assert.assertEquals(3564396286L, address);

        address = IpAddressUtil.convertIpAddress("192.121.218.4");
        Assert.assertEquals(3229211140L, address);

        address = IpAddressUtil.convertIpAddress("130.239.8.25");
        Assert.assertEquals(2196703257L, address);

        address = IpAddressUtil.convertIpAddress("127.0.0.1");
        Assert.assertEquals(2130706433L, address);

        // 130*16777216 + 239*65536 + 8*256 + 255 = 2196703487 
        address = IpAddressUtil.convertIpAddress("130.239.8.255");
        Assert.assertEquals(2196703487L, address);

        String[] invalidAddresses = { "130.239.8.256", "130.239.256.255", "130.239.8", "130.239.X.255", "", null }; 
        for (int i = 0; i < invalidAddresses.length; i++) {
            try {
                address = IpAddressUtil.convertIpAddress(invalidAddresses[i]);
                fail("IP-address is invalid and should cause an exception. IP-address: " + invalidAddresses[i]);
            } catch (UnknownHostException e) {
                // empty
            }
        }
    }
    
    
    @Test
    public void reverseDnsLookup() throws Exception {
        String[][] testArray = { 
                { "-1", "" },
                { "0", "" }, 
                { "3229211226", "192.121.218.90 (www.cert.se)" },
                { "1142340358", "68.22.187.6 (www.team-cymru.org)" },
                { "3239050580", "193.15.253.84 (ras84.cmdata.se)" },
            };
        for (int i = 0; i < testArray.length; i++) {
            long ipAddress = Long.parseLong(testArray[i][0]);
            String hostname = IpAddressUtil.convertIpAddress(ipAddress, true);
            Assert.assertEquals(testArray[i][1], hostname);

            String ipAddressStr = IpAddressUtil.reverseDnsLookup(ipAddress);
            String expectedIpAddressStr = StringUtil.splitHeadTail(testArray[i][1], " (", false)[1];
            if (expectedIpAddressStr.length() > 0) {
                expectedIpAddressStr = expectedIpAddressStr.substring(0, expectedIpAddressStr.length()-1);
            }
            Assert.assertEquals(expectedIpAddressStr, ipAddressStr);
        }
    }

    
    @Test
    public void reverseDnsLookupPerformance() throws Exception {
        // Result:
        //   - dnsjava
        //        noOfSuccessfulLookups=177
        //        noOfErrorLookups=28
        //        Time: 0:00:01.200
        //
        //   - JDK implementation
        //        noOfSuccessfulLookups=148
        //        noOfErrorLookups=57
        //        Time: 0:02:27.649
        
        String[] testArray = {
                "59.52.56.233",
                "217.28.34.132",
                "89.178.155.170",
                "180.254.154.127",
                "112.78.2.26",
                "70.21.253.46",
                "194.237.142.20",
                "194.237.142.21",
                "62.247.6.10",
                "192.36.80.8",
                "193.13.73.77",
                "193.15.240.59",
                "193.15.240.60",
                "90.139.99.230",
                "91.149.34.32",
                "91.149.35.53",
                "92.241.206.100",
                "130.242.82.146",
                "213.113.42.12",
                "213.114.115.101",
                "90.239.96.223",
                "90.239.97.91",
                "195.100.0.49",
                "195.100.0.60",
                "80.109.144.203",
                "194.237.142.20",
                "194.237.142.21",
                "62.247.6.10",
                "192.36.80.8",
                "193.13.73.77",
                "193.15.240.59",
                "193.15.240.60",
                "90.139.99.230",
                "91.149.34.32",
                "91.149.35.53",
                "92.241.206.100",
                "130.242.82.146",
                "213.113.42.12",
                "213.114.115.101",
                "90.239.96.223",
                "90.239.97.91",
                "195.100.0.49",
                "195.100.0.60",
                "80.109.144.203",
                "80.109.178.72",
                "80.169.182.241",
                "80.169.182.242",
                "80.169.182.244",
                "80.169.182.246",
                "80.169.182.248",
                "212.37.27.194",
                "213.80.57.34",
                "195.58.125.96",
                "212.105.18.122",
                "212.105.18.123",
                "84.216.76.93",
                "85.197.131.143",
                "85.197.150.237",
                "212.116.65.23",
                "212.116.65.27",
                "212.116.65.32",
                "212.116.65.33",
                "212.116.65.34",
                "212.116.65.36",
                "85.24.234.92",
                "194.71.138.42",
                "194.71.138.48",
                "194.71.138.61",
                "194.71.139.60",
                "212.112.167.85",
                "87.96.228.244",
                "62.80.211.180",
                "192.36.34.249",
                "212.112.63.122",
                "62.101.37.201",
                "62.101.43.199",
                "62.101.48.106",
                "62.101.48.109",
                "62.101.51.66",
                "217.72.48.85",
                "217.72.50.132",
                "217.72.53.159",
                "217.72.58.224",
                "217.72.58.232",
                "217.72.62.251",
                "89.236.0.91",
                "89.236.1.205",
                "217.28.34.132",
                "192.44.242.18",
                "192.44.243.18",
                "83.209.45.12",
                "83.241.213.36",
                "83.241.213.38",
                "83.241.213.39",
                "83.241.213.41",
                "83.241.235.154",
                "194.6.252.11",
                "217.76.94.15",
                "62.63.212.120",
                "62.63.215.12",
                "62.63.215.23",
                "62.63.215.7",
                "62.63.224.22",
                "91.126.8.112",
                "83.145.50.120",
                "79.102.100.82",
                "79.102.135.220",
                "79.102.142.157",
                "79.102.154.71",
                "79.102.155.239",
                "79.102.194.163",
                "79.102.89.71",
                "81.8.140.42",
                "217.68.36.10",
                "217.68.36.11",
                "62.88.128.21",
                "87.249.172.160",
                "62.108.199.108",
                "62.108.202.163",
                "62.108.204.115",
                "62.108.216.39",
                "88.206.204.54",
                "88.83.57.119",
                "80.86.65.66",
                "80.86.75.221",
                "85.119.130.132",
                "217.76.52.168",
                "83.233.112.73",
                "83.233.144.155",
                "83.233.144.74",
                "83.233.145.13",
                "83.233.145.77",
                "217.21.232.237",
                "82.115.151.195",
                "212.37.99.39",
                "212.37.99.44",
                "88.80.6.91",
                "84.55.103.67",
                "84.55.103.85",
                "84.55.98.24",
                "85.89.65.12",
                "85.89.81.108",
                "85.30.144.147",
                "85.30.183.127",
                "194.103.188.58",
                "194.103.189.24",
                "194.103.189.35",
                "194.103.189.42",
                "77.110.15.53",
                "91.90.31.10",
                "91.90.31.11",
                "91.90.31.16",
                "91.90.31.21",
                "77.105.218.234",
                "77.105.218.70",
                "77.105.226.50",
                "77.105.227.102",
                "77.105.228.113",
                "77.105.228.58",
                "82.96.28.165",
                "82.96.28.166",
                "82.96.28.210",
                "82.96.28.249",
                "82.96.31.31",
                "82.96.31.7",
                "213.89.128.136",
                "213.89.128.149",
                "213.89.128.228",
                "83.255.2.236",
                "83.255.33.234",
                "82.117.105.45",
                "82.117.105.7",
                "82.117.108.139",
                "82.117.108.156",
                "82.117.108.172",
                "82.117.108.89",
                "83.223.11.120",
                "83.223.14.69",
                "77.53.32.127",
                "77.53.44.229",
                "77.53.45.68",
                "77.53.46.106",
                "77.53.46.177",
                "77.53.46.198",
                "77.53.46.57",
                "77.91.220.5",
                "212.27.0.125",
                "212.27.0.127",
                "212.27.0.130",
                "212.27.0.134",
                "212.27.0.149",
                "212.27.0.15",
                "80.251.207.129",
                "85.8.1.173",
                "130.242.82.146",
                "192.121.192.22",
                "192.121.234.65",
                "192.121.234.66",
                "192.165.239.30",
                "192.165.247.1",
                "192.34.107.10",
                "192.34.107.12",
                "192.34.107.13",
                "192.34.107.200",
                "192.34.107.222",
                "192.34.107.77",
                "192.36.34.249",
                "192.36.80.8",
                "192.44.242.18",
                "192.44.243.18",
                "193.13.73.77",
                "193.15.240.59",
                "193.15.240.60",
                "193.15.253.84",
                "193.180.228.186",
                "193.44.157.68",
                "193.44.157.95",
                "193.44.6.118",
                "193.44.6.134",
                "193.44.6.50",
                "194.103.188.58",
                "194.103.189.24",
                "194.103.189.35",
                "194.103.189.42",
                "194.132.44.115",
                "194.132.44.122",
                "194.132.44.126",
                "194.132.65.195",
                "194.16.47.4",
                "194.17.12.146",
                "59.52.56.233",
                "217.28.34.132",
                "89.178.155.170",
                "180.254.154.127",
                "112.78.2.26",
                "70.21.253.46",
                "194.237.142.20",
                "194.237.142.21",
                "62.247.6.10",
                "192.36.80.8",
                "193.13.73.77",
                "193.15.240.59",
                "193.15.240.60",
                "90.139.99.230",
                "91.149.34.32",
                "91.149.35.53",
                "92.241.206.100",
                "130.242.82.146",
                "213.113.42.12",
                "213.114.115.101",
                "90.239.96.223",
                "90.239.97.91",
                "195.100.0.49",
                "195.100.0.60",
                "80.109.144.203",
                "194.237.142.20",
                "194.237.142.21",
                "62.247.6.10",
            };
        long startTime = System.currentTimeMillis();
        List<String> hostnames = new ArrayList<String>();
        int noOfSuccessfulLookups = 0;
        int noOfErrorLookups = 0;
        for (int i = 0; i < testArray.length; i++) {
            long t1 = System.currentTimeMillis();
            long ipAddress = IpAddressUtil.convertIpAddress(testArray[i]);
            String hostname = IpAddressUtil.reverseDnsLookup(ipAddress);
            long duration = System.currentTimeMillis() - t1;
            System.out.println(ipAddress + " --> '" + hostname + "' (" + duration + " ms).");
            if (hostname.length() == 0) {
                ++noOfErrorLookups;
            } else {
                ++noOfSuccessfulLookups;
                hostnames.add(hostname);
            }
        }
        System.out.println("noOfSuccessfulLookups=" + noOfSuccessfulLookups);
        System.out.println("noOfErrorLookups=" + noOfErrorLookups);
        String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - startTime);
        System.out.println("Time: " + durationStr);
        
        // write array
//        for (Iterator<String> iterator = hostnames.iterator(); iterator.hasNext(); ) {
//            System.out.println("\"" + iterator.next() + "\",");
//        }
    }
    
    
    @Test
    public void dnsLookupPerformance() throws Exception {
        // Result:
        //
        // - dnsjava:
        //        noOfSuccessfulLookups=154
        //        noOfErrorLookups=23
        //        Time: 0:00:03.354
        //
        // - Standard JDK        
        //        noOfSuccessfulLookups=154
        //        noOfErrorLookups=23
        //        Time: 0:00:16.437
        
        // Why not all hostnames resolves: http://en.wikipedia.org/wiki/Forward_Confirmed_reverse_DNS
        
        String[] testArray = {
                "setnip02.ericsson.net",
                "cacher3.ericsson.net",
                "pwlan.lfv.se",
                "host-193-13-73-77.griffel.se",
                "alv-global.tietoenator.com",
                "alv-global.tietoenator.com",
                "m90-139-99-230.cust.tele2.ru",
                "ip-32-34-149-91.dialup.ice.net",
                "ip-53-35-149-91.dialup.ice.net",
                "ip-100-206-241-92.dialup.ice.net",
                "bpf-eth0.sunet.se",
                "ua-213-113-42-12.cust.bredbandsbolaget.se",
                "ua-213-114-115-101.cust.bredbandsbolaget.se",
                "host-90-239-96-223.mobileonline.telia.com",
                "host-90-239-97-91.mobileonline.telia.com",
                "chello080109144203.tirol.surfer.at",
                "chello080109178072.tirol.surfer.at",
                "mail.bitepr.se",
                "mail.matrix-se.com",
                "md469127a.utfors.se",
                "md469127b.utfors.se",
                "ip-85-197-131-143.home-o.dsl.bikab.com",
                "ip-85-197-150-237.c4stads.bikab.com",
                "www.rehab.lund.skane.se",
                "start",
                "www.knusper.com",
                "www.pcdata.se",
                "h-234-92.A222.priv.bahnhof.se",
                "af194-71-138-42.client.arvikafestivalen.net",
                "af194-71-138-48.client.arvikafestivalen.net",
                "af194-71-138-61.client.arvikafestivalen.net",
                "af194-71-139-60.client.arvikafestivalen.net",
                "244-228-96-87.cust.blixtvik.se",
                "b1mp2-ppp-180.gotanet.se",
                "lda009.lul.se",
                "212-112-63-122.lidnet.net",
                "201.kringdata.net",
                "62-101-48-106.sheab.net",
                "62-101-48-109.sheab.net",
                "62-101-51-66.sheab.net",
                "hd9483055.sedjdjl.dyn.perspektivbredband.net",
                "hd9483284.seststf.dyn.perspektivbredband.net",
                "hd948359f.seelelx.dyn.perspektivbredband.net",
                "hd9483ae0.sehjakc.dyn.perspektivbredband.net",
                "hd9483ae8.sehjakc.dyn.perspektivbredband.net",
                "hd9483efb.seststf.dyn.perspektivbredband.net",
                "h59ec005b.seluldx.dyn.perspektivbredband.net",
                "h59ec01cd.seluldx.dyn.perspektivbredband.net",
                "nsabfw1.nsab.se",
                "b1.sll.se",
                "b2.sll.se",
                "ce10194-adsl.cenara.com",
                "36.213.241.83.in-addr.dgcsystems.net",
                "38.213.241.83.in-addr.dgcsystems.net",
                "39.213.241.83.in-addr.dgcsystems.net",
                "41.213.241.83.in-addr.dgcsystems.net",
                "154.235.241.83.in-addr.dgcsystems.net",
                "ftp.repro.ttg.se",
                "s212h120n1sdt.cn.tyfon.se",
                "s215h12o1svl2.dyn.tyfon.se",
                "s215h23o1svl2.dyn.tyfon.se",
                "s215h7o1svl2.dyn.tyfon.se",
                "s224h22o1kho5.dyn.tyfon.se",
                "h-91-126-8-112.wholesale.rp80.se",
                "c-4f666452-74736162.cust.telenor.se",
                "c-4f6687dc-74736162.cust.telenor.se",
                "c-4f668e9d-74736162.cust.telenor.se",
                "c-4f669a47-74736162.cust.telenor.se",
                "c-4f669bef-74736162.cust.telenor.se",
                "c-4f66c2a3-74736162.cust.telenor.se",
                "c-4f665947-74736162.cust.telenor.se",
                "mail.castor.se",
                "mail.castor.se",
                "hostex2.goteborg.se",
                "87-249-172-160.ljusnet.se",
                "108dsl199.helsingenet.com",
                "163dsl202.helsingenet.com",
                "115dsl204.helsingenet.com",
                "39dsl216.helsingenet.com",
                "88-206-204-54.highlandnet.se",
                "ftp.hudiksvall.se",
                "megan.wm.net",
                "c-83-233-112-73.cust.bredband2.com",
                "c-83-233-144-155.cust.bredband2.com",
                "c-83-233-144-74.cust.bredband2.com",
                "c-83-233-145-13.cust.bredband2.com",
                "c-83-233-145-77.cust.bredband2.com",
                "grab-hotel-guests.gotlandsresor.se",
                "39.99.37.212.static.varnamo.net",
                "44.99.37.212.static.varnamo.net",
                "a6-91-n19.cust.prq.se",
                "84-55-103-67.customers.ownit.se",
                "84-55-103-85.customers.ownit.se",
                "84-55-98-24.customers.ownit.se",
                "brf-ang-cust12.netit.se",
                "dyn-ks-net81-cust108.netit.se",
                "host-85-30-144-147.sydskane.nu",
                "host-85-30-183-127.sydskane.nu",
                "sigipsec.skane.se",
                "flow.skane.se",
                "falkor.skane.se",
                "xifasf.skane.se",
                "ip8-53.bon.riksnet.se",
                "user234.77-105-218.netatonce.net",
                "user70.77-105-218.netatonce.net",
                "user50.77-105-226.netatonce.net",
                "user102.77-105-227.netatonce.net",
                "user113.77-105-228.netatonce.net",
                "user58.77-105-228.netatonce.net",
                "dialup-82-96-28-165.rixtele.com",
                "dialup-82-96-28-166.rixtele.com",
                "dialup-82-96-28-210.rixtele.com",
                "dialup-82-96-28-249.rixtele.com",
                "dialup-82-96-31-31.rixtele.com",
                "dialup-82-96-31-7.rixtele.com",
                "c213-89-128-136.bredband.comhem.se",
                "c213-89-128-149.bredband.comhem.se",
                "c213-89-128-228.bredband.comhem.se",
                "c83-255-2-236.bredband.comhem.se",
                "c83-255-33-234.bredband.comhem.se",
                "45-105-117-82.cust.blixtvik.se",
                "7-105-117-82.cust.blixtvik.se",
                "139-108-117-82.cust.blixtvik.se",
                "156-108-117-82.cust.blixtvik.se",
                "172-108-117-82.cust.blixtvik.se",
                "89-108-117-82.cust.blixtvik.se",
                "120-11-223-83.fastbit.se",
                "69.14.223.83.fastbit.se",
                "cust-127.geab-032-1.ephone.se",
                "cust-229.geab-044-1.ephone.se",
                "cust-068.geab-045-1.ephone.se",
                "cust-106.geab-046-1.ephone.se",
                "cust-177.geab-046-1.ephone.se",
                "cust-198.geab-046-1.ephone.se",
                "cust-057.geab-046-1.ephone.se",
                "212.27.0.125.bredband.tre.se",
                "212.27.0.127.bredband.tre.se",
                "212.27.0.130.bredband.tre.se",
                "212.27.0.134.bredband.tre.se",
                "212.27.0.149.bredband.tre.se",
                "212.27.0.15.bredband.tre.se",
                "cust-IP-129.data.tre.se",
                "85.8.1.173.static.se.wasadata.net",
                "bpf-eth0.sunet.se",
                "22.0-24.192.121.192.host.songnetworks.se",
                "gwadm.falun.se",
                "gwkultur.falun.se",
                "ingw.promacom.se",
                "outgoing.tac.com",
                "static-192.34.107.10.addr.tdcsong.se",
                "static-192.34.107.12.addr.tdcsong.se",
                "static-192.34.107.13.addr.tdcsong.se",
                "static-192.34.107.200.addr.tdcsong.se",
                "static-192.34.107.222.addr.tdcsong.se",
                "static-192.34.107.77.addr.tdcsong.se",
                "lda009.lul.se",
                "pwlan.lfv.se",
                "b1.sll.se",
                "b2.sll.se",
                "host-193-13-73-77.griffel.se",
                "alv-global.tietoenator.com",
                "alv-global.tietoenator.com",
                "ras84.cmdata.se",
                "193-44-157-68.customer.telia.com",
                "193-44-157-95.customer.telia.com",
                "r118.jokknet.se",
                "r134.jokknet.se",
                "r50.jokknet.se",
                "sigipsec.skane.se",
                "flow.skane.se",
                "falkor.skane.se",
                "xifasf.skane.se",
                "tscluster.utb.hoganas.se",
                "mail.hoganas.se",
                "pix.hoganas.se",
                "194-16-47-4.customer.telia.com",
                "194-17-12-146.customer.telia.com",
        };
        
        long startTime = System.currentTimeMillis();
        int noOfSuccessfulLookups = 0;
        int noOfErrorLookups = 0;
        for (int i = 0; i < testArray.length; i++) {
            long t1 = System.currentTimeMillis();
            long ipAddress = IpAddressUtil.dnsLookup(testArray[i]);
            if (ipAddress != 0L) {
                ++noOfSuccessfulLookups;
                long duration = System.currentTimeMillis() - t1;
                String hostAddress = IpAddressUtil.convertIpAddress(ipAddress).getHostAddress();
                System.out.println(testArray[i] + " --> '" + hostAddress + "' (" + duration + " ms).");
            } else {
                ++noOfErrorLookups;   
            }
        }
        System.out.println("noOfSuccessfulLookups=" + noOfSuccessfulLookups);
        System.out.println("noOfErrorLookups=" + noOfErrorLookups);
        String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - startTime);
        System.out.println("Time: " + durationStr);
    }

    
    private String ipRangeToString(long[] ipRange) {
        if ((ipRange == null) || (ipRange.length != 2)) {
            return null;
        }
        return IpAddressUtil.convertIpAddress(ipRange[0], false) + "-" + IpAddressUtil.convertIpAddress(ipRange[1], false); 
    }

}
