import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.CommandLineParseException;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;
import se.sitic.megatron.util.Version;
import se.sitic.megatron.db.DbException;


/** !!! IMPORTANT !!!
 * This code shoud only be run to migrate data and schema for the organiaton database from version 1.0.12 to version 1.1.0.
 */




public class OrganizationContactMigrator  {
    private static final Logger log = Logger.getLogger(OrganizationContactMigrator.class);

    private static final String VALID_VERSION = "1.1.0";
    
    private TypedProperties props;
    private Connection conn;


    
    public OrganizationContactMigrator(TypedProperties props) throws DbException {
        this.props = props;

        try {
            conn = createConnection();
        } catch (Exception e) {
            // ClassNotFoundException, SQLException
        	log.error("Cannot create a database connection " + e.getMessage());
            throw new DbException("Cannot create a database connection.", e);            
        }
    }

    
    public void addHistory(int step, String status, String comment) {
    	
    	log.info(step + ": " + status + " - " + comment);    	
        String sql = "INSERT into migration_history (step,status,comment) values(" + step + ", '" + status +"', '" + comment + "');";
        int result = executeUpdate(sql);    	
    }
    
    
    public void createMigrationHistoryTable(int step) {
    	
    	log.debug("Creating createMigrationHistoryTable table");
    
    	int result = 0;
        
        String sql = "CREATE TABLE `migration_history` (" +
        		" `id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT," +
        		" `step` int(3) unsigned," +
        		" `status` VARCHAR(15)," + 
        		" `comment` text," + 
        		"  PRIMARY KEY (`id`)" +
        		") ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;";


        result = executeUpdate(sql);

        addHistory(step, "completed", "added history table");              
        
    }    
    
    public void backupOrganizationTable(int step) {

    	addHistory(step, "started", "Starting backing-up of Organization table.");
    	String sql = "CREATE TABLE organization_bak LIKE organization;";    				
    	executeUpdate(sql);
    	sql = "INSERT INTO organization_bak SELECT * FROM organization;";
    	addHistory(step, "completed", "Done backing-up of Organization table.");
    }
    
    
    public int createContactTable(int step){
   
    	System.out.println("Creating contact table");
    	
    	int result = 0;
        
        
        String sql =
        		
        		//DROP TABLE IF EXISTS `contact`;
        "CREATE TABLE `contact` ( " +
        "`id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT, " +
        "`org_id` mediumint(8) unsigned NOT NULL, " +
        "`first_name` varchar(64) DEFAULT NULL, " +
        "`last_name` varchar(64) DEFAULT NULL, " +
        "`role` varchar(64) DEFAULT NULL, " +
        "`phone_number` varchar(32) DEFAULT NULL, " +
        "`email_address` varchar(128) NOT NULL, " +
        "`email_type` char(4) NOT NULL, " +
        "`enabled` tinyint(1) NOT NULL, " +
        "`comment` text, " +
        "`created` int(10) unsigned NOT NULL, " +
        "`last_modified` int(10) unsigned NOT NULL, " +
        "`modified_by` varchar(64) NOT NULL, " +
        "`external_reference` varchar(255) DEFAULT NULL, " +
        "`auto_update_email` tinyint(1) NOT NULL, " +
        "PRIMARY KEY (`id`)" +
        ") ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=latin1";

        		
        result = executeUpdate(sql);
        
        addHistory(step, "completed", "create contact table");
        
        System.out.println("result = " + result);
        
        return result;
    	
    }
    
    public void copyEmailsToComment(int step) {
    	
    	
    	
    	String sql = "UPDATE organization SET comment = CONCAT (comment, ' The following emailaddesses where copied to the contact table during the database migration: ' , email_addresses) where email_addresses is not null and email_addresses like '%@%'";
    	
    	executeUpdate(sql);    	
    	
    	addHistory(step, "completed", "Copied organization emails to comment.");
    	
    }
    
    public void createContactForEmails(int step) {


    	String sql = "SELECT id, email_addresses, auto_update_email from organization;";

    	
    	try {
    		ResultSet rs = executeQuery(sql);
    		addHistory(step, "started", "Migrating");
    		int noOrgs = 0;
    		int noEmails = 0;
    		while (rs.next()) {
    			log.debug("RS has next");
    			noOrgs++;
    			int orgId = rs.getInt("ID");
    			String emailAddresses = rs.getString("EMAIL_ADDRESSES");
    			int autoUpdate = rs.getInt("AUTO_UPDATE_EMAIL");
    			String emailType = "to";
    			String comment = "Contact auto-migrated with values from Organization table.";
    			String modifiedBy = "System migration";
    			long timeStamp = SqlUtil.convertTimestamp(new Date());
    			if (emailAddresses != null && emailAddresses.isEmpty() == false && emailAddresses.contains("@")) {
    				log.debug("mailaddresses are valid");
    				String[] addresses = emailAddresses.split(",");
    				for (String address : addresses) {
    					log.debug("have several addresses");
    					if (address.contains("@")) {    	
    						log.debug("Address is " + address);
    						String name = address.split("@")[0];
    						log.debug("name is " + name);
    						String contactSql = String.format("INSERT INTO contact (org_id, first_name, email_address, email_type, enabled, comment, auto_update_email, created, last_modified, modified_by) " +
    								"values (%d, '%s', '%s', '%s', %d, '%s', %d, %d, %d, '%s');", orgId, name, address, emailType, 1, comment, autoUpdate, timeStamp, timeStamp, modifiedBy);
    						log.debug("sql is "+ contactSql);
    						executeUpdate(contactSql);
    						noEmails++;
    					}
    				}
    			}
    		}
    		rs.getStatement().close();
    		
    		addHistory(step, "completed", "Done migrating " + noOrgs + 	" organizations, creating " + noEmails + " email contacts.");
    	}
    	catch (Exception e) {
    		log.error("Exception in createContactForEmails " + e.getMessage());
    		System.exit(1);
    	}

    }
    
    public void completeMigration(int step) {

    	String sql = "SELECT step, status FROM migration_history ORDER BY id DESC";
    	
    	try {

    		ResultSet rs = executeQuery(sql);

    		rs.next();
    		int migrStep = rs.getInt("step");
    		String status = rs.getString("status");    		    	
    		
    		if (migrStep != 5 || status.equals("completed") == false) {
    			log.error("Wrong migration status, can not complete the migration.");

    		}
    		else if (migrStep == 6 && status.equals("completed") == true) {
    			log.info("Migration already completed.");
    		}
    		else {	

    			addHistory(step, "started", "Completing migration");

    			sql = "ALTER TABLE organization DROP auto_update_email;";
    			executeUpdate(sql);
    			sql = "ALTER TABLE organization DROP email_addresses;";
    			executeUpdate(sql);

    			addHistory(step, "done", "Migration completed.");
    			// sql = "DROP TABLE migration_history";
    		}    
    	}
    	catch (SQLException e) {
    		log.error("Could not complete migration " + e.getMessage());
    	}
    }

    private int executeUpdate(String sql) {
        
    	int result = 0;
    	PreparedStatement stmt = null;
        try {
        	log.debug("Executing sql " + sql);
            stmt = conn.prepareStatement(sql);
            result = stmt.executeUpdate();
            log.debug("Sql executed.");
            stmt.close();
        } catch (SQLException e) {
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) { }  
            log.error("SQLException: " + sql + "error: " + e.getMessage());
            System.exit(1);
        }
        return 0;
    	
    }
    
    private ResultSet executeQuery(String sql) {
    
    	ResultSet resultSet = null;
    	PreparedStatement stmt = null;
        try {
        	log.debug("Executing sql " + sql);
            stmt = conn.prepareStatement(sql);
            resultSet = stmt.executeQuery();
            log.debug("Sql executed");
            
        } catch (SQLException e) {
            try { if (stmt != null) stmt.close(); } catch (Exception ignored) { }  
            log.error("SQLException: " + sql + "error: " + e.getMessage());
            System.exit(1);
        }
        return resultSet;
    	
    } 
      
    public void close() throws DbException {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
            	log.error("Could not close database connection " + e.getMessage());
                throw new DbException("Cannot close database connection.", e);                
            }
        }
    }
    
    
    private Connection createConnection() throws ClassNotFoundException, SQLException {
        String driverClassName = props.getString(AppProperties.JDBC_DRIVER_CLASS_KEY, "com.mysql.jdbc.Driver");
        String url = AppProperties.getInstance().getJdbcUrl();
        String user = props.getString(AppProperties.DB_USERNAME_KEY, "megatron");
        String password = props.getString(AppProperties.DB_PASSWORD_KEY, "megatron");

        System.out.println("Trying to create connection: " + driverClassName + " " + url + " " + user + " " + password);
        
        Class.forName(driverClassName);
        return DriverManager.getConnection(url, user, password);
    }
    
    
    public static void printHelp() {
    	System.out.println("Usage: migrate-db-sh [options] logfile");
    	System.out.println("\nOptions:");
    	System.out.println(" -h 	Print this help message");
    	System.out.println(" -n		Do not copy migrated mail addresses into the organization comment field.");
    	System.out.println(" -c		Complete the migration i.e. remove unused database columns.");    	    	
    	
    }
    
    
    
    /**
     * Main.
     */
    public static void main(String[] args) {
    	Logger log = null;    	
    	try {

    		
    		for (String arg : args) {
    			
    			System.out.println("Args: " + arg);
    		}
    		
    		// -- Read config files and parse command line

    		// Check that this is the correct version:
    		String appVersion = Version.getAppVersion();
    		if (appVersion.equals(VALID_VERSION)== false) {
    			System.out.println("Sorry this is not the correct version of the Megatron relase. This code will only run on version: " + VALID_VERSION);
    			System.exit(1);
    		}

    		if (args.length >= 1 && args[0] != null && args[0].equals("-h") || (args.length >= 1 && args[0].equals("-n") == false && args[0].equals("-c"))) {
    			printHelp();
    			System.exit(0);    			
    		}
    		
    		

    		AppProperties.getInstance().init(null);
    		TypedProperties globalProps = AppProperties.getInstance().getGlobalProperties();     
    		OrganizationContactMigrator ocm = new OrganizationContactMigrator(globalProps);

    		//ocm.createMigrationHistoryTable(1);
    		//ocm.backupOrganizationTable(2);
    		//ocm.createContactTable(3);
    		if (args.length >= 1 && args[0].equals("-n") == false) {
    			ocm.copyEmailsToComment(4);
    		}
    		//ocm.createContactForEmails(5);
    		if (args.length >= 1 && args[0].equals("-c") == true) {
    			ocm.completeMigration(6);
    		}
    		System.out.println("Doing nothing");
    		ocm.close();

    	} catch (Exception e) {
    		String msg = "Error: Cannot initialize configuration: " + e.getMessage();
    		System.err.println(msg);
    		e.printStackTrace();
    		System.exit(1);
    	}

    	System.exit(0);
    }
}

