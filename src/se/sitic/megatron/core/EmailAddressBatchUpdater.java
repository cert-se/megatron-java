package se.sitic.megatron.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import se.sitic.megatron.db.DbException;
import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.util.Constants;


/**
 * Adds or deletes email addresses in the database as specified in a file.
 */
public class EmailAddressBatchUpdater {
    private final Logger log = Logger.getLogger(EmailAddressBatchUpdater.class);
        
    private TypedProperties props;
    private int readLinesC = 0;
    private boolean removeEmails = false;
    private boolean addEmails = false;
    private DbManager dbm = null;
    
    
    public EmailAddressBatchUpdater(TypedProperties props) {
        this.props = props;
        
        try {
            dbm = DbManager.createDbManager(this.props);
        } catch (DbException e) {
            handleException("ImportSystemData", e);           
        }   
    }
    
    
    public void addAddresses(String fileName) throws MegatronException {
        File file = new File(fileName);
        log.info("Adding email addresses to the database listed in file: " + file.getAbsoluteFile());

        this.addEmails = true;
        handleEmailAddresses(file); 
    }

    
    public void deleteAddresses(String fileName) throws MegatronException {
        File file = new File(fileName);
        log.info("Deleting email addresses from the database listed in file: " + file.getAbsoluteFile());

        this.removeEmails= true;
        handleEmailAddresses(file);        
    }
    
    
    private void handleEmailAddresses(File file) throws MegatronException {
                                      
        BufferedReader in = null;
        try {                       
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Constants.UTF8));
            String line = null;
            while ((line = in.readLine()) != null) {
                ++readLinesC;
                log.debug("Processing line " + line);
                processEmailRow(line);
            }           
        } catch (IOException e) {            
            throw handleException("handleEmailAddresses", e);
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}            
        }       
    }
    
    
    private void processEmailRow(String line) {

        String[] input = line.split(" ");

        if (input == null || input.length < 2) {
            log.error("Missing data in line " + this.readLinesC);
        }
        
        String domainName = input[0];
        String emailAddresses = input[1];
        
        if (emailAddresses.length() > 0) {

            Organization org = null;
            try {
                org = dbm.searchOrgForDomain(domainName);
            } catch (DbException e) {
                log.error("DbException i processEmailRow line no = " + this.readLinesC);
                e.printStackTrace();
            }

            if (org != null) {
                String[] toChangeAddresses = emailAddresses.split(",");

                ArrayList<String> currentAddresses = null;
                if (org.getEmailAddresses() == null) {
                    currentAddresses = new ArrayList<String>();
                }
                else {
                    currentAddresses = new ArrayList<String>(Arrays.asList(org.getEmailAddresses().split(",")));
                }
                
                for (String address : toChangeAddresses) {
                    if (this.removeEmails && currentAddresses.contains(address) != false) {
                        log.info("Removing address " + address + " from domain " + domainName);
                        currentAddresses.remove(address);
                    }
                    else if (this.addEmails && currentAddresses.contains(address) == false) {
                        log.info("Adding address " + address + " for domain " + domainName);
                        currentAddresses.add(address);
                    }
                }
                String newAddresses = "";
                for (String address : currentAddresses) {
                    if (address.equals("") == false) {
                        newAddresses = newAddresses + "," + address;
                    }                    
                }                
                org.setEmailAddresses(newAddresses.substring(1)); 
                try {
                    dbm.updateOrganization(org, "Batch-updater");
                } catch (DbException e) {
                    handleException("processEmailRow", e);                    
                }
            }
        }
    }   


    private MegatronException handleException(String methodName, Exception e) {
        
        MegatronException result = null;
        
        String msg = e.getClass().getSimpleName() + " in " + methodName + "; (email file line no: " + readLinesC + ") : " + e.getMessage();
        log.error(msg, e);
        if (e.getClass().getSimpleName().equals("MegatronException")){
            result = (MegatronException)e;
        }
        else {          
            e.printStackTrace();
            result = new MegatronException(msg);            
        }
        return result;
    }
}
