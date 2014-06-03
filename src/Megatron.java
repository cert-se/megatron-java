import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.CommandLineParseException;
import se.sitic.megatron.core.EmailAddressBatchUpdater;
import se.sitic.megatron.core.JobInfoWriter;
import se.sitic.megatron.core.JobListWriter;
import se.sitic.megatron.core.JobScheduler;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.NetnameUpdater;
import se.sitic.megatron.core.StatsRssGenerator;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.db.ImportBgpTable;
import se.sitic.megatron.db.ImportSystemData;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.Priority;
import se.sitic.megatron.report.IReportGenerator;
import se.sitic.megatron.ui.OrganizationHandler;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.FileUtil;
import se.sitic.megatron.util.StringUtil;
import se.sitic.megatron.util.Version;


/**
 * Main class for Megatron. This class is called from the CLI.
 */
public class Megatron {
    /** Key in system properties for log4j config file. */
    private static final String LOG4J_FILE_KEY = "megatron.log4jfile";

    // Note:
    // * --overwrite is not used. use --delete plus re-run instead.
    // * --export may be used with --output-dir or use filename in config
    // * Log job + mail job is not allowed, because it breaks preview.
    // * --mail requires --id plus and job. --no-db is not allowed.
    private static final String USAGE =
        "_______ _______  ______ _______ _______  ______  _____  __   _" + Constants.LINE_BREAK +
        "|  |  | |______ |  ____ |_____|    |    |_____/ |     | | \\  |" + Constants.LINE_BREAK +
        "|  |  | |______ |_____| |     |    |    |    \\_ |_____| |  \\_|" + Constants.LINE_BREAK +
        "" + Constants.LINE_BREAK +
        "Usage: megatron.sh [options] logfiles(s)" + Constants.LINE_BREAK +
        "" + Constants.LINE_BREAK +
        "Options:" + Constants.LINE_BREAK +
        "  -v, --version        Print application version and exit." + Constants.LINE_BREAK +
        "  -h, --help           Print this help message." + Constants.LINE_BREAK +
        "  -s, --slurp          Process files in the slurp directory." + Constants.LINE_BREAK +
        "  -l, --list-jobs      List processed log jobs. No. of days may be specified." + Constants.LINE_BREAK +
        "  -e, --export         Export log records to file." + Constants.LINE_BREAK +
        "  -d, --delete         Delete job including log records." + Constants.LINE_BREAK +
        "  -D, --delete-all     Delete job plus mail jobs." + Constants.LINE_BREAK +
        "  -j, --job            Specifies job, e.g. 'shadowserver-drone_2009-06-22_084510'." + Constants.LINE_BREAK +
        "  -t, --job-type       Specifies job type for input files, e.g. 'shadowserver-drone'." + Constants.LINE_BREAK +
        "  -o, --output-dir     Specifies directory for export files." + Constants.LINE_BREAK +
        "  -i, --id             Specifies RTIR id." + Constants.LINE_BREAK +
        "  -p, --prio           Specifies priority for log records to be emailed or exported." + Constants.LINE_BREAK +
        "  -P, --list-prios     List priorities." + Constants.LINE_BREAK +
        "  -I, --job-info       Print info about specified log job." + Constants.LINE_BREAK +
        "  -n, --no-db          Skip writes to the database."  + Constants.LINE_BREAK +
        "  -S, --stdout         Writes to stdout instead of export file."  + Constants.LINE_BREAK +
        "  -1, --mail-dry-run   Create a mail report but does not send any mail." + Constants.LINE_BREAK +
        "  -2, --mail-dry-run2  As '--mail-dry-run' but more verbose." + Constants.LINE_BREAK +
        "  -m, --mail           Send mails for a job."  + Constants.LINE_BREAK +
        "  -b, --use-org2       Use secondary organization when mailing." + Constants.LINE_BREAK +
        "" + Constants.LINE_BREAK +
        "Admin Options:" + Constants.LINE_BREAK +
        "  --import-contacts    Import organizations to the database." + Constants.LINE_BREAK +
        "  --import-bgp         Import BGP dump file (specified in config)." + Constants.LINE_BREAK +
        "  --update-netname     Update netname field from whois queries." + Constants.LINE_BREAK +
        "  --add-addresses      Add email addresses listed in specified file." + Constants.LINE_BREAK +
        "  --delete-addresses   Delete email addresses listed in specified file." + Constants.LINE_BREAK +
        "  --create-rss         Create RSS with Megatron statistics."+ Constants.LINE_BREAK +
        "  --create-reports     Create report files (json, xml, html, etc.)."+ Constants.LINE_BREAK +
        "  --create-report      Run a specific report."+ Constants.LINE_BREAK +
        "  --ui-org             Administration of organizations (command line interface)." + Constants.LINE_BREAK +
        "" + Constants.LINE_BREAK +
        "Examples:" + Constants.LINE_BREAK +
        "  Process file and save result in the database:" + Constants.LINE_BREAK +
        "    megatron.sh --job-type shadowserver-drone 2009-06-22-drone-report-se.csv" + Constants.LINE_BREAK +
        "  Preview of mail to be sent:" + Constants.LINE_BREAK +
        "    megatron.sh --job shadowserver-drone_2009-06-22_160142 --id 4242 --mail-dry-run" + Constants.LINE_BREAK +
        "  Send mails for the job:" + Constants.LINE_BREAK +
        "    megatron.sh --job shadowserver-drone_2009-06-22_160142 --id 4242 --mail" + Constants.LINE_BREAK +
        "  As above, but sends only to organizations with a prio of 50 or above:" + Constants.LINE_BREAK +
        "    megatron.sh --job shadowserver-drone_2009-06-22_160142 --id 4242 --prio 50 --mail" + Constants.LINE_BREAK +
        "  Display information, e.g. high priority organizations, about specified job:" + Constants.LINE_BREAK +
        "    megatron.sh --job-info shadowserver-drone_2009-06-15_124508" + Constants.LINE_BREAK +
        "  Delete specified job:" + Constants.LINE_BREAK +
        "    megatron.sh --delete shadowserver-drone_2009-06-15_124508" + Constants.LINE_BREAK +
        "  Display log jobs created the last 4 days:" + Constants.LINE_BREAK +
        "    megatron.sh --list-jobs 4" + Constants.LINE_BREAK +
        "  Process files and exports them to a different format:" + Constants.LINE_BREAK +
        "    megatron.sh --export --job-type whois-cymru-verbose --no-db file1.txt file2.txt" + Constants.LINE_BREAK +
        "  Add email addresses listed in file to the database:" + Constants.LINE_BREAK +
        "    megatron.sh --add-addresses new-addresses.txt" + Constants.LINE_BREAK +
        "  Start admin-UI for organizations, IP-blocks, ASNs, and domain names." + Constants.LINE_BREAK +
        "    megatron.sh --ui-org" + Constants.LINE_BREAK +
        "  Run the organization report (emails an abuse-report to selected organizations)." + Constants.LINE_BREAK +
        "    megatron.sh --create-report se.sitic.megatron.report.OrganizationReportGenerator" + Constants.LINE_BREAK +
        "  Process files in the slurp directory and exports them to a different format:" + Constants.LINE_BREAK +
        "    megatron.sh --slurp --no-db --export" + Constants.LINE_BREAK +
        "  Process files in the slurp directory and save result in the database:" + Constants.LINE_BREAK +
        "    megatron.sh --slurp" + Constants.LINE_BREAK;

    private static final String CLI_ERROR =
        "@errorMsg@" + Constants.LINE_BREAK +
        "--help will show help message." + Constants.LINE_BREAK +
        "" + Constants.LINE_BREAK;


    /**
     * Main.
     */
    public static void main(String[] args) {
        Logger log = null;
        try {
            Megatron megatron = new Megatron();

            // -- Read config files and parse command line
            try {
                AppProperties.getInstance().init(args);
            } catch (CommandLineParseException e) {
                if (e.getAction() == CommandLineParseException.SHOW_USAGE_ACTION) {
                    System.out.println(USAGE);
                    System.exit(0);
                } else if (e.getAction() == CommandLineParseException.SHOW_VERSION_ACTION) {
                    System.out.println(Version.getVersionInfo());
                    System.exit(0);
                } else {
                    String errorMsg = (e.getMessage() != null) ? e.getMessage() : "";
                    String msg = StringUtil.replace(CLI_ERROR, "@errorMsg@", errorMsg);
                    System.err.println(msg);
                    System.exit(1);
                }
            } catch (MegatronException e) {
                String msg = "Error: Cannot initialize configuration: " + e.getMessage();
                System.err.println(msg);
                e.printStackTrace();
                System.exit(1);
            }
            
            // -- Init logging
            megatron.ensureDirs();
            try {
                megatron.initLogging();
            } catch (FileNotFoundException e) {
                String msg = "Error: Cannot initialize logging: " + e.getMessage();
                System.err.println(msg);
                e.printStackTrace();
                System.exit(2);
            }
            log = Logger.getLogger(Megatron.class);
            
            log.info(megatron.getVersion() + " started.");
            megatron.processCommands();
            log.info(Version.getAppName() + " finished.");
            
            System.exit(0);
        } catch (Throwable e) {
            String msg = (e instanceof MegatronException) ? "Error: " + e.getMessage() : "Error (unhandled exception): " + e.getMessage();

            if (log != null) {
                // Big Brother looks for "fatal" errors.
                log.fatal(msg, e);
            }


            // Write message to console (without a stack trace, which may scare the user)
            System.err.println(msg);
            Throwable cause = e.getCause();
            while (cause != null) {
                msg = cause.getMessage();
                if (StringUtil.isNullOrEmpty(msg) || msg.equalsIgnoreCase("null")) {
                    msg = "[Message not available]";
                }
                System.err.println("  " + msg);
                cause = cause.getCause();
            }
            try {
                String dir = AppProperties.getInstance().getGlobalProperties().getString(AppProperties.LOG_DIR_KEY, "[property missing]");
                // hardcoded; filename is specified in log4j.properties
                String filename = "megatron.log";    
                File logFile = new File(dir, filename);
                System.err.println("See log file for more info: " + logFile.getAbsoluteFile());
            } catch (Exception e2) {
                // ignored
            }

            System.exit(100);
        }
    }


    private void initLogging() throws FileNotFoundException {
        String log4jFilename = System.getProperty(LOG4J_FILE_KEY);
        String defaultFilename = AppProperties.getInstance().getGlobalProperties().getString(AppProperties.LOG4J_FILE_KEY, null);
        log4jFilename = (log4jFilename != null) ? log4jFilename : defaultFilename;
        File file = new File(log4jFilename);
        if (!file.exists()) {
            String msg = "Cannot find log4j config-file: " + file.getAbsoluteFile() + ". Working directory for Megatron is probably wrong.";
            throw new FileNotFoundException(msg);
        }

        PropertyConfigurator.configure(log4jFilename);
    }


    /**
     * Creates, if necessary, directories required by the application.
     */
    private void ensureDirs() throws IOException {
        TypedProperties globalProps = AppProperties.getInstance().getGlobalProperties();
        String dir = null; 
        dir = globalProps.getString(AppProperties.LOG_DIR_KEY, null);
        FileUtil.ensureDir(dir);
        dir = globalProps.getOutputDir();
        FileUtil.ensureDir(dir);
        dir = globalProps.getString(AppProperties.SLURP_DIR_KEY, null);
        FileUtil.ensureDir(dir);
    }


    private String getVersion() {
        return Version.getVersion(true);
    }

    
    /**
     * Process commands in the command line.
     */
    private void processCommands() throws MegatronException {
        TypedProperties globalProps = AppProperties.getInstance().getGlobalProperties();

        List<String> inputFiles = AppProperties.getInstance().getInputFiles();
        if (inputFiles.size() > 0) {
            for (Iterator<String> iterator = inputFiles.iterator(); iterator.hasNext();) {
                File file = new File(iterator.next());
                String jobType = AppProperties.getInstance().mapFilenameToJobType(file.getName(), false);
                if (jobType == null) {
                    throw new MegatronException("Cannot find a job-type. Use '--job-type' to specify one.");
                }
                TypedProperties props = AppProperties.getInstance().createTypedPropertiesForCli(jobType);
                JobScheduler.getInstance().processFile(props, file);
            }
        } else if (globalProps.isListJobs()) {
            JobListWriter writer = new JobListWriter(globalProps, true);
            writer.execute();
        } else if (globalProps.isListPrios()) {
            listPrio(globalProps);
        } 
        else if (globalProps.isJobInfo()) {
            JobInfoWriter writer = new JobInfoWriter(globalProps);
            writer.execute();
        } 
        else if (globalProps.isSlurp()) {
            JobScheduler.getInstance().processSlurpDirectory();
        } else if (globalProps.isExport()) {
            String jobName = globalProps.getJob();
            if (jobName == null) {
                throw new MegatronException("Invalid command line: No job ('--job') specified.");
            }
            
            String jobType = getJobType(globalProps, jobName);
            TypedProperties props = AppProperties.getInstance().createTypedPropertiesForCli(jobType);
            JobScheduler.getInstance().processFileExport(props, jobName);
        } else if (globalProps.isMail() || globalProps.isMailDryRun() || globalProps.isMailDryRun2()) {
            // check CLI args
            String id = globalProps.getId();
            if (id == null) {
                throw new MegatronException("Invalid command line: No RTIR-id ('--id') specified.");
            }
            String jobName = globalProps.getJob();
            if (jobName == null) {
                throw new MegatronException("Invalid command line: No job ('--job') specified.");
            }
            if (globalProps.isNoDb()) {
                throw new MegatronException("Invalid command line: --no-db cannot be used when mailing.");
            }
            
            // run mail job
            String jobType = getJobType(globalProps, jobName);
            TypedProperties props = AppProperties.getInstance().createTypedPropertiesForCli(jobType);
            JobScheduler.getInstance().processMailJob(props, jobName);
        } else if (globalProps.isImportContacts()) {
        	ImportSystemData dataImporter = new ImportSystemData(globalProps);
        	dataImporter.importFile();            
        } else if (globalProps.isImportBgp()) {
            ImportBgpTable importer = new ImportBgpTable(globalProps);
            importer.importFile();
        } else if (globalProps.isUpdateNetname()) {
            NetnameUpdater updater = new NetnameUpdater(globalProps);
            updater.update();
        } else if (globalProps.isAddAddresses()) {
            EmailAddressBatchUpdater updater = new EmailAddressBatchUpdater(globalProps);
            updater.addAddresses(globalProps.getAddressesFile());
        } else if (globalProps.isDeleteAddresses()) {
            EmailAddressBatchUpdater updater = new EmailAddressBatchUpdater(globalProps);
            updater.deleteAddresses(globalProps.getAddressesFile());
        } else if (globalProps.isCreateStatsRss()) {
            StatsRssGenerator generator = new StatsRssGenerator(globalProps);
            generator.createFile();
        } else if (globalProps.isCreateFlashXml() || globalProps.isCreateReports() || (globalProps.getCreateReport() != null)) {
            String classNames[] = null;
            if (globalProps.getCreateReport() != null) {
                classNames = new String[1];
                classNames[0] = globalProps.getCreateReport();
            } else {
                classNames = globalProps.getStringList(AppProperties.REPORT_CLASS_NAMES_KEY, new String[0]);
            }
            List<IReportGenerator> reportGenerators = createReportGenerators(classNames);
            if (reportGenerators.isEmpty()) {
                throw new MegatronException("No report generators specified. See '" + AppProperties.REPORT_CLASS_NAMES_KEY + "'." );
            }
            for (Iterator<IReportGenerator> iterator = reportGenerators.iterator(); iterator.hasNext(); ) {
                iterator.next().createFiles();
            }
        } else if (globalProps.isUiOrg()) {
            OrganizationHandler uiOrg = new OrganizationHandler(globalProps);
            uiOrg.startUI();                    
        } else if (globalProps.isDelete() || globalProps.isDeleteAll()) {
            String jobName = globalProps.getJob();
            if (jobName == null) {
                throw new MegatronException("Invalid command line: No job ('--job') specified.");
            }
            if (globalProps.isNoDb()) {
                throw new MegatronException("Invalid command line: --no-db cannot be used when deleting.");
            }

            if (globalProps.isDeleteAll()) {
                JobScheduler.getInstance().deleteAll(globalProps, jobName);
            } else {
                JobScheduler.getInstance().delete(globalProps, jobName);
            }
            System.out.println("Job deleted: " + jobName);
        } else {
            // nothing to do; show usage
            System.out.println(USAGE);
        }
    }

    
    /**
     * Fetches job type from db for specified job name.
     */
    private String getJobType(TypedProperties globalProps, String jobName) throws MegatronException {
        DbManager dbManager = null;
        try {
            dbManager = DbManager.createDbManager(globalProps);
            Job job = dbManager.searchLogJob(jobName);
            if (job == null) {
                String msg = "Cannot find job: " + jobName;
                throw new MegatronException(msg);
            }
            String result = job.getJobType().getName();
            if (Constants.DEFAULT_JOB_TYPE.equals(result)) {
                // use job type in job name if job type not specified in db  
                String[] headTail = StringUtil.splitHeadTail(jobName, "_", false);
                result = headTail[0];
            }
            return result;
        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
    }
    
    
    /**
     * List all priorites to stdout. 
     */
    private void listPrio(TypedProperties globalProps) throws MegatronException {
        DbManager dbManager = null;
        try {
            dbManager = DbManager.createDbManager(globalProps);
            List<Priority> prios = dbManager.getAllPriorities();
            System.out.println("+------+----------------------------------------------------------------------------+");
            System.out.println("| Prio | Name                                                                       |");                
            System.out.println("+------+----------------------------------------------------------------------------+");
            for (Priority prio: prios) {
                System.out.printf("| %4d | %-74s |\n", prio.getPrio(), prio.getName());
            }
            System.out.println("+------+----------------------------------------------------------------------------+");
        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
    }
    
    
    private List<IReportGenerator> createReportGenerators(String[] classNames) throws MegatronException {
        List<IReportGenerator> result = new ArrayList<IReportGenerator>();
        for (int i = 0; i < classNames.length; i++) {
            String className =  classNames[i];
            if (className.trim().length() == 0) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(className);
                IReportGenerator reportGenerator = (IReportGenerator)clazz.newInstance();
                reportGenerator.init();
                result.add(reportGenerator);
            } catch (MegatronException e) {
                String msg = "Cannot initialize report generator class: " + className;
                throw new MegatronException(msg, e);
            } catch (Exception e) {
                // ClassNotFoundException, InstantiationException, IllegalAccessException
                String msg = "Cannot instantiate report generator class: " + className;
                throw new MegatronException(msg, e);
            }
        }
        return result;
    }

}
