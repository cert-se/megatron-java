import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.mail.Message;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.db.DbException;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.Version;

/**
 * !!! IMPORTANT !!! This code should only be run to migrate data and schema for
 * the organisation database from version 1.0.12 to version 1.1.0.
 *
 *
 * This class migrates the database schema for the releases prior from version 
 * 1.1.0 to the schema introduced in version 1.1.0. 
 * 
 * The tasks done by this class in sequential order are:
 *  
 * 1. Creating a migration history table to keep track of the migration state
 * 2. Backing up the organization table to the table organization_bak
 * 3. Creating the contact table
 * 4. Copying the email addresses to the comment column, can be skipped with
 *    the '-n' command switch.
 * 5. Insert a contact row for each email address found in each organization.
 * 6. Completes the migration, is invoked with the command switch '-c'.
 *    This step removed the migration history table and the organization
 *    backup table. It also removes the columns auto_update_email and
 *    email_adresses in the organization table.
 * 
 * The migration is done in two steps where task 1-5 is performed in step 1.
 * After step 1 the system can be used for a test period if needed. 
 * When the migration tests are done successfully the last step (task 6) should be
 * performed.
 * 
 * USAGE:
 * 
 * This migraton tool is invoked through the megatron.sh script using the --class 
 * switch. 
 * 
 * Additional switches are:
 *   -n if the e-mail addresses should not be copied in to the comment field
 *      for the organization.
 *   -c to complete the migration
 *
 * 
 **/

public class OrganizationContactMigrator {
    private static final Logger log = Logger
            .getLogger(OrganizationContactMigrator.class);

    private static final String VALID_VERSION = "1.1.";

    private TypedProperties props;
    private Connection conn;
    private String user;

    public OrganizationContactMigrator(TypedProperties props)
            throws DbException {
        this.props = props;

        try {
            conn = createConnection();
            this.user = System.getenv("SUDO_USER");
            if (this.user == null) {
                this.user = System.getenv("USER");
            }
        } catch (Exception e) {
            // ClassNotFoundException, SQLException
            log.error("Cannot create a database connection " + e.getMessage());
            throw new DbException("Cannot create a database connection.", e);
        }
    }

    public void addHistory(int step, String status, String comment) {

        log.info(step + ": " + status + " - " + comment);
        String sql = "INSERT into migration_history (step,status,comment) values("
                + step + ", '" + status + "', '" + comment + "');";
        int result = executeUpdate(sql);
        log.debug("Add history result: " + result);
    }

    public boolean doesTableExist(String name) {

        // Check if the migration has already been started or completed by
        // checking if
        // the contact or migration_history tables exists.

        boolean hasTable = false;

        String lookForTable = "SHOW TABLES LIKE '" + name + "'";

        try {
            ResultSet resultSet = executeQuery(lookForTable);
            hasTable = resultSet.next();
        } catch (Exception e) {
            log.error("Could not check if table " + name + " exists.");
        }

        return hasTable;
    }

    public void createMigrationHistoryTable(int step) {

        log.debug("Creating createMigrationHistoryTable table");

        String sql = "CREATE TABLE `migration_history` ("
                + " `id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,"
                + " `step` int(3) unsigned," + " `status` VARCHAR(15),"
                + " `comment` text," + "  PRIMARY KEY (`id`)"
                + ") ENGINE=MyISAM AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;";

        executeUpdate(sql);

        addHistory(step, "completed", "added history table");

    }

    public void backupOrganizationTable(int step) {

        addHistory(step, "started", "Backing-up of organization table.");
        String sql = "CREATE TABLE organization_bak LIKE organization;";
        executeUpdate(sql);
        sql = "INSERT organization_bak SELECT * FROM organization;";
        executeUpdate(sql);
        addHistory(step, "completed", "Backing-up of organization table.");
    }

    public int createContactTable(int step) {

        addHistory(step, "started", "Create contact table.");

        int result = 0;

        String sql = "CREATE TABLE contact ( "
                + "id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT, "
                + "org_id MEDIUMINT UNSIGNED NOT NULL, "
                + "first_name VARCHAR(64), " + "last_name VARCHAR(64), "
                + "role VARCHAR(64), " + "phone_number VARCHAR(32), "
                + "email_address VARCHAR(128) NOT NULL, "
                + "email_type CHAR(4) NOT NULL, "
                + "enabled BOOLEAN NOT NULL, " + "`comment` text, "
                + "created INT unsigned NOT NULL, "
                + "last_modified INT unsigned NOT NULL, "
                + "modified_by VARCHAR(64) NOT NULL, "
                + "external_reference VARCHAR(255), "
                + "auto_update_email BOOLEAN NOT NULL, " + "PRIMARY KEY (id)"
                + ") ENGINE=MyISAM";
        result = executeUpdate(sql);

        // Change default value for auto_update_email until migration is completed
        sql = "ALTER TABLE organization ALTER auto_update_email SET DEFAULT 1;";
        result = executeUpdate(sql);
        
        addHistory(step, "completed", "Create contact table.");

        log.info("Create contact table result = " + result);

        return result;

    }

    public void copyEmailsToComment(int step) {

        addHistory(step, "started", "Copy organization emails to comment.");

        String timeStamp = "["
                + this.user
                + " "
                + DateUtil.formatDateTime(
                        DateUtil.DATE_TIME_FORMAT_WITH_T_CHAR, new Date())
                + "] ";

        String sql = "UPDATE organization SET comment = CONCAT (comment, ' "
                + timeStamp
                + "\nEmail addresses copied by OrganizationContactMigrator: ' , email_addresses ) where email_addresses is not null and email_addresses like '%@%'";

        executeUpdate(sql);

        addHistory(step, "completed", "Copy organization emails to comment.");

    }

    public void createContactForEmails(int step) {

        String sql = "SELECT id, email_addresses, auto_update_email from organization;";

        try {
            ResultSet rs = executeQuery(sql);
            addHistory(step, "started", "Migrating email addresses.");
            int noOrgs = 0;
            int noEmails = 0;
            while (rs.next()) {
                noOrgs++;
                int orgId = rs.getInt("ID");
                String emailAddresses = rs.getString("EMAIL_ADDRESSES");
                int autoUpdate = rs.getInt("AUTO_UPDATE_EMAIL");
                String emailType = Message.RecipientType.TO.toString();
                String commentTimeStamp = "["
                        + this.user
                        + " "
                        + DateUtil.formatDateTime(
                                DateUtil.DATE_TIME_FORMAT_WITH_T_CHAR,
                                new Date()) + "] ";
                String comment = commentTimeStamp + "Contact migrated.";
                String modifiedBy = "System migration";
                long timeStamp = SqlUtil.convertTimestamp(new Date());
                if (emailAddresses != null && emailAddresses.isEmpty() == false
                        && emailAddresses.contains("@")) {
                    log.debug("mail addresses are valid");
                    String[] addresses = emailAddresses.split(",");
                    for (String address : addresses) {
                        log.debug("have several addresses");
                        if (address.contains("@")) {
                            log.debug("Address is " + address);
                            String name = address.split("@")[0];
                            log.debug("name is " + name);
                            String contactSql = String
                                    .format("INSERT INTO contact (org_id, first_name, email_address, email_type, enabled, comment, auto_update_email, created, last_modified, modified_by) "
                                            + "values (%d, '%s', '%s', '%s', %d, '%s', %d, %d, %d, '%s');",
                                            orgId, name, address, emailType, 1,
                                            comment, autoUpdate, timeStamp,
                                            timeStamp, modifiedBy);
                            log.debug("sql is " + contactSql);
                            executeUpdate(contactSql);
                            noEmails++;
                        }
                    }
                }
            }
            rs.getStatement().close();

            addHistory(step, "completed", "Migrated email addresses for "
                    + noOrgs + " organizations, created " + noEmails
                    + " email contacts.");
        } catch (Exception e) {
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

            } else if (migrStep == 6 && status.equals("completed") == true) {
                log.info("Migration already completed.");
            } else {
                addHistory(step, "started", "Migration completion.");
                sql = "ALTER TABLE organization DROP auto_update_email;";
                executeUpdate(sql);
                sql = "ALTER TABLE organization DROP email_addresses;";
                executeUpdate(sql);
                sql = "DROP TABLE organization_bak;";
                executeUpdate(sql);
                addHistory(step, "completed", "Migration completetion.");
                sql = "DROP TABLE migration_history;";
                executeUpdate(sql);
                log.info("Migration completed");
            }
        } catch (SQLException e) {
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
            log.debug("Sql update executed, result: " + result);
            stmt.close();
        } catch (SQLException e) {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception ignored) {
            }
            log.error("SQLException: " + sql + "error: " + e.getMessage());
            System.exit(1);
        }
        return result;

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
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception ignored) {
            }
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
                log.error("Could not close database connection "
                        + e.getMessage());
                throw new DbException("Cannot close database connection.", e);
            }
        }
    }

    private Connection createConnection() throws ClassNotFoundException,
            SQLException {
        String driverClassName = props.getString(
                AppProperties.JDBC_DRIVER_CLASS_KEY, "com.mysql.jdbc.Driver");
        String url = AppProperties.getInstance().getJdbcUrl();
        String user = props
                .getString(AppProperties.DB_USERNAME_KEY, "megatron");
        String password = props.getString(AppProperties.DB_PASSWORD_KEY,
                "megatron");

        Class.forName(driverClassName);
        return DriverManager.getConnection(url, user, password);
    }

    public static void printHelp() {
        System.out.println("Usage: migrate-db-sh [options] logfile");
        System.out.println("\nOptions:");
        System.out.println(" -h\tPrint this help message");
        System.out
                .println(" -n\tDo not copy migrated mail addresses into the organization comment field.");
        System.out
                .println(" -c\tComplete the migration i.e. remove unused database columns.");
    }

    /**
     * Main.
     */
    public static void main(String[] args) {

        try {

            // -- Read config files and parse command line

            // Check that this is the correct version:
            String appVersion = Version.getAppVersion();
            if (appVersion.startsWith(VALID_VERSION) == false) {
                System.out
                        .println("Sorry this is not the correct version of the Megatron relase. This code will only run on versions starting with: "
                                + VALID_VERSION);
                System.exit(1);
            }

            if (args.length >= 1
                    && args[0] != null
                    && args[0].equals("-h")
                    || (args.length >= 1 && args[0].equals("-n") == false && args[0]
                            .equals("-c") == false)) {
                printHelp();
                System.exit(0);
            }

            AppProperties.getInstance().init(null);
            TypedProperties globalProps = AppProperties.getInstance()
                    .getGlobalProperties();
            OrganizationContactMigrator ocm = new OrganizationContactMigrator(
                    globalProps);

            // Check if the migration has already been performed
            if (ocm.doesTableExist("contact") == true) {

                if (ocm.doesTableExist("migration_history")
                        && (args.length >= 1 && args[0].equals("-c"))) {
                    // Migration history table exist and argument 'c' (complete)
                    // given
                    ocm.completeMigration(6);
                    ocm.close();
                    System.exit(0);
                } else if (ocm.doesTableExist("migration_history")) {
                    // Migration history table exist and argument 'c' (complete)
                    // not given
                    String errorMessage = "The migration has been started, use argument '-c' to complete.";
                    log.error(errorMessage);
                    System.err.println(errorMessage);
                    System.exit(1);
                } else {
                    // Migration history does not exist, migration has already
                    // been completed
                    String errorMessage = "The database has already been migrated.";
                    log.error(errorMessage);
                    System.err.println(errorMessage);
                    System.exit(1);
                }
            } else if (args.length >= 1 && args[0].equals("-c")) {
                // Migration has not started but the 'c' (complete) argument has
                // been supplied
                String errorMessage = "The first stage of the migration has not been performed, do not use the '-c' argument yet.";
                log.error(errorMessage);
                System.err.println(errorMessage);
                System.exit(1);
            }

            ocm.createMigrationHistoryTable(1);
            ocm.backupOrganizationTable(2);
            ocm.createContactTable(3);

            if ((args.length >= 1 && args[0].equals("-n")) == false) {
                ocm.copyEmailsToComment(4);
            }

            ocm.createContactForEmails(5);
            ocm.close();

        } catch (Exception e) {
            String msg = "Error: Cannot initialize configuration: "
                    + e.getMessage();
            System.err.println(msg);
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }
}
