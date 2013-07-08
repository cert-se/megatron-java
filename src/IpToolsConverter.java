import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.StringUtil;


/**
 * Converts IP-tools database to an Megatron import file.
 */
public class IpToolsConverter {
    // Finans;65
    // Samhallsviktigt foretag;60
    // Foretag;30
    // ISP;10
    private static final String PRIO = "10";
    
    private static final String INPUT_DIR = "ip-tools/contacts/ISP";
    private static final String IMPORT_FILE = "ip-tools/isp.txt";
    private static final String SEED_FILE = "ip-tools/isp_seed.txt";
    private static final String ALL_ADDRESSES_FILE = "ip-tools/all-ip-tools-addresses.txt";
    
    private static final String IMPORT_HEADER = 
        "!ASNumber;*Number" + Constants.LINE_BREAK +
        "!DomainName;*Name" + Constants.LINE_BREAK +
        "!IpRange;*StartAddress,EndAddress" + Constants.LINE_BREAK +
        "!Organization;*Name,RegistrationNo,CountryCode,EmailAddresses,DomainNames,IpRanges,ASNumbers,ModifiedBy,Priority";
    private static final String IMPORT_ROW_TEMPLATE = "@domainName@;;SE;@emailAddresses@;@domainName@;;@asn@;IpToolsConverter;@prio@;" + Constants.LINE_BREAK;
    private static final String SEED_ROW_TEMPLATE = "@domainName@;;@domainName@" + Constants.LINE_BREAK;
    
    
    public IpToolsConverter() {
        // empty
    }


    public static void main(String[] args) {
        IpToolsConverter converter = new IpToolsConverter();
        try {
            converter.convert();
            converter.extractEmailAddresses();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    
    public void extractEmailAddresses() throws Exception {
        String[] dirs = { "ip-tools/contacts/finans_org", "ip-tools/contacts/ISP", "ip-tools/contacts/ovriga_org",  
                "ip-tools/contacts/privata_ftg", "ip-tools/contacts/statliga_org", "ip-tools/contacts/SUNET" };
        
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(new File(ALL_ADDRESSES_FILE)));
            for (int i = 0; i < dirs.length; i++) {
                out.write("# Dir: " + dirs[i] + Constants.LINE_BREAK);
                Map<String, Organization> orgMap = readFiles(dirs[i]);
                List<String> strList = new ArrayList<String>(orgMap.keySet());
                Collections.sort(strList);
                for (Iterator<String> iterator = strList.iterator(); iterator.hasNext(); ) {
                    String domain = iterator.next();
                    String emailAddresses = orgMap.get(domain).getEmailAddresses();
                    out.write(domain + " " + emailAddresses + Constants.LINE_BREAK);
                }
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    
    public void convert() throws Exception {
        Map<String, Organization> orgMap = readFiles(INPUT_DIR);
        writeFiles(orgMap);
    }

    
    private Map<String, Organization> readFiles(String inputDir) throws IOException {
        Map<String, Organization> result = new HashMap<String, Organization>();
        File dir = new File(inputDir);
        if (!dir.isDirectory()) {
            throw new IOException("Cannot find directory: " + inputDir);
        }
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().startsWith("AS")) {
                handleAsFile(result, files[i]);
            } else {
                handleDomainFile(result, files[i]);
            }
        }
        return result;
    }

    
    private void handleAsFile(Map<String, Organization> orgMap, File file) throws IOException {
        List<String> emails = readEmailAddresses(file);
        if (emails.size() == 0) {
            return;
        }
        
        String[] headTail = StringUtil.splitHeadTail(emails.get(0), "@", false);
        String domain = headTail[1];
        Organization org = orgMap.get(domain);
        if (org == null) {
            org = new Organization();
            org.setDomainName(domain);
        }
        org.setAsn(file.getName());
        org.addEmailAddresses(emails);
        orgMap.put(domain, org);
    }

    
    private void handleDomainFile(Map<String, Organization> orgMap, File file) throws IOException {
        List<String> emails = readEmailAddresses(file);
        if (emails.size() == 0) {
            return;
        }

        String domain = file.getName();
        Organization org = orgMap.get(domain);
        if (org == null) {
            org = new Organization();
            org.setDomainName(domain);
        }
        org.addEmailAddresses(emails);
        orgMap.put(domain, org);
    }

    
    private List<String> readEmailAddresses(File file) throws IOException {
        List<String> result = new ArrayList<String>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if ((line.length() > 0) && !line.startsWith("#")) {
                    result.add(line);
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return result;
    }
    

    private void writeFiles(Map<String, Organization> orgMap) throws IOException {
        BufferedWriter importFile = null;
        BufferedWriter seedFile = null;
        try {
            importFile = new BufferedWriter(new FileWriter(IMPORT_FILE));
            importFile.write(IMPORT_HEADER);
            seedFile = new BufferedWriter(new FileWriter(SEED_FILE));
            List<String> domainNames = new ArrayList<String>(orgMap.keySet());
            Collections.sort(domainNames);
            for (Iterator<String> iterator = domainNames.iterator(); iterator.hasNext();) {
                Organization org = orgMap.get(iterator.next());
                String row = null;
                
                row = IMPORT_ROW_TEMPLATE;
                row = StringUtil.replace(row, "@domainName@", org.getDomainName());
                row = StringUtil.replace(row, "@emailAddresses@", org.getEmailAddresses());
                row = StringUtil.replace(row, "@asn@", org.getAsn());
                row = StringUtil.replace(row, "@prio@", PRIO);
                importFile.write(row);
                
                row = SEED_ROW_TEMPLATE;
                row = StringUtil.replace(row, "@domainName@", org.getDomainName());
                row = StringUtil.replace(row, "@emailAddresses@", org.getEmailAddresses());
                row = StringUtil.replace(row, "@asn@", org.getAsn());
                row = StringUtil.replace(row, "@prio@", PRIO);
                seedFile.write(row);
            }
        } finally {
          if (importFile != null) {
              importFile.close();
          }
          if (seedFile != null) {
              seedFile.close();
          }
        }
        File file = new File(IMPORT_FILE);
        System.out.println("File created: " + file.getAbsoluteFile() + " (" + file.length() + " bytes).");
        file = new File(SEED_FILE);
        System.out.println("File created: " + file.getAbsoluteFile() + " (" + file.length() + " bytes).");
    }
    
    
    private class Organization {
        private String domainName;
        private List<String> emailAddresses;
        private String asn;


        public Organization() {
            this.emailAddresses = new ArrayList<String>();
        }
        
        public String getDomainName() {
            return domainName;
        }

        public void setDomainName(String domainName) {
            this.domainName = domainName;
        }

        public void addEmailAddress(String email) {
            if (!emailAddresses.contains(email)) {
                emailAddresses.add(email);
            }
        }

        public void addEmailAddresses(List<String> emails) {
            for (Iterator<String> iterator = emails.iterator(); iterator.hasNext();) {
                addEmailAddress(iterator.next());
            }
        }

        public String getEmailAddresses() {
            StringBuilder result = new StringBuilder(512);
            Collections.sort(emailAddresses);
            for (Iterator<String> iterator = emailAddresses.iterator(); iterator.hasNext(); ) {
                if (result.length() != 0) {
                    result.append(",");
                }
                result.append(iterator.next());
            }
            return result.toString();
        }
        
        public String getAsn() {
            return (asn != null) ? asn : "";
        }

        public void setAsn(String asn) {
            if (!asn.startsWith("AS")) {
                System.out.println("Invalid AS number: " + asn);
                return;
            }
            
            this.asn = asn.substring(2);
        }
        
    }
    
}
