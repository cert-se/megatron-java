package se.sitic.megatron.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.log4j.Logger;

import se.sitic.megatron.db.TestDb;

public class TestEmailAddressBatchUpdater {

   
    public static void main(String[] args) {
        
        Logger log = Logger.getLogger(TestDb.class);
        
        TypedProperties props = new TypedProperties(
                new HashMap<String, String>(), null);
        EmailAddressBatchUpdater updater = new EmailAddressBatchUpdater(props);
 
        String command = readInput("Enter command (A = add, D = delete) > ");
        
        String fileName = readInput("Enter file name > ");
       
        try {
            if (command.toUpperCase().startsWith("A")) {
                log.info("Starting adding of addresses");
                updater.addAddresses(fileName);
                log.info("Done with adding of addresses");
            }
            else if (command.toUpperCase().startsWith("D")) {
                log.info("Starting removal of addresses");
                updater.deleteAddresses(fileName);
                log.info("Done with removal of addresses");
            }
        } catch (MegatronException e) {
            log.error("Exception, could not perform " + command + " using file " + fileName + ".");
            e.printStackTrace();
        }
    }
    
    private static String readInput(String promt){

        String input = null;
        
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            System.out.print(promt);            
            input = in.readLine();
        } catch (java.io.IOException ioe) {   
            ioe.printStackTrace();
        }
        return input;        
    } 
}
;