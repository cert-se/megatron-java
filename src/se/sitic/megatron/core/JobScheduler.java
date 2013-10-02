package se.sitic.megatron.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.MailJob;
import se.sitic.megatron.util.FileUtil;


/**
 * Scheduler for log jobs and mail jobs. Responsible for keeping track of
 * pending jobs, e.g. the same job should not be executed twice. Handles 
 * files in the slurp directory. 
 */
public class JobScheduler {
    private static final Logger log = Logger.getLogger(JobScheduler.class);

    private static JobScheduler singleton;
    
    private static final String OK_DIR = "ok";
    private static final String ERROR_DIR = "error";

    
    /**
     * Returns singleton.
     */
    public static synchronized JobScheduler getInstance() {
        if (singleton == null) {
            singleton = new JobScheduler();
        }
        return singleton;
    }


    /**
     * Constructor. Private due to singleton.
     */
    private JobScheduler() {
        // empty
    }

    
    /**
     * Slurps files in the slurp directory; calls processFile for each file.  
     */
    public int processSlurpDirectory() throws MegatronException {
        int noOfFiles = 0;
        int noOfOkFiles = 0;
        
        // -- Ensure okDir and errorDir
        File okDir = null;
        File errorDir = null;
        TypedProperties globalProps = AppProperties.getInstance().getGlobalProperties();
        String slurpDirName = globalProps.getString(AppProperties.SLURP_DIR_KEY, "slurp");
        File slurpDir = new File(slurpDirName);
        try {
            okDir = new File(slurpDir, OK_DIR);
            FileUtil.ensureDir(okDir);
            errorDir = new File(slurpDir, ERROR_DIR);
            FileUtil.ensureDir(errorDir);
        } catch (IOException e) {
            String dirName = (errorDir != null) ? errorDir.getAbsolutePath() : okDir.getAbsolutePath(); 
            String msg = "Cannot create directory: " + dirName;
            throw new MegatronException(msg, e);
        }
        
        // -- Process files
        File[] fileArray = slurpDir.listFiles();
        if (fileArray == null) {
            throw new MegatronException("Cannot retrieve file list for the slurp directory: " + slurpDir.getAbsolutePath());
        }
        List<File> files = new ArrayList<File>(Arrays.asList(fileArray));
        Collections.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    // oldest file first
                    return (o1.lastModified() < o2.lastModified()) ? -1 : (o1.lastModified() == o2.lastModified() ? 0 : 1);
                }
            });
        Map<String, Long> jobTypeLastExecutedMap = new HashMap<String, Long>();
        for (Iterator<File> iterator = files.iterator(); iterator.hasNext(); ) {
            File file = iterator.next();
            if (file.isDirectory()) {
                continue;
            }
            ++noOfFiles;
            log.info("Processing file in the slurp directory: " + file.getName());
            // create job type props
            String jobType = AppProperties.getInstance().mapFilenameToJobType(file.getName(), true);
            log.info("Job type found: " + jobType);
            if (jobType == null) {
                String msg = "Skipping file; cannot map job-type to file in slurp directory: " + file.getName();
                log.error(msg);
                System.out.println(msg);
                moveFile(file, errorDir);
                continue;
            }
            // process file
            try {
                TypedProperties props = AppProperties.getInstance().createTypedPropertiesForCli(jobType);
                
                // ensure unique job name by sleeping 1 second if risk of a name clash (timestamp is in seconds)
                Long lastExecuted = jobTypeLastExecutedMap.get(jobType);
                if ((lastExecuted != null) && (System.currentTimeMillis() - lastExecuted.longValue()) < 2000L) {
                    log.debug("Sleeps 1 second to ensure unique job name.");
                    Thread.sleep(1000L);
                }
                jobTypeLastExecutedMap.put(jobType, new Long(System.currentTimeMillis())); 
                
                processFile(props, file);
                moveFile(file, okDir);
                ++noOfOkFiles;
            } catch (Exception e) {
                String msg = "Processing of file in slurp directory failed: " + file.getName();
                log.error(msg, e);
                System.out.println(msg);
                moveFile(file, errorDir);
            }
        }

        int noOfErrorFiles = noOfFiles - noOfOkFiles;
        log.info("Processing of the slurp directory finished. No. of files: " + noOfFiles + " (errors: " + noOfErrorFiles + ").");

        return noOfOkFiles;
    }

    
    /**
     * Parses specified file and store result in the database, or in an export file.
     */
    public void processFile(TypedProperties props, File file) throws MegatronException {
        JobManager jobManager = new JobManager(props);
        jobManager.execute(file);
    }


    /**
     * Creates emails for specified job, which exists in the database.  
     */
    public void processMailJob(TypedProperties props, String jobName) throws MegatronException {
        MailExportManager mailExportManager = new MailExportManager(props);
        mailExportManager.execute(jobName);
    }
    
    
    /**
     * Exports specified job from the database to file.
     */
    public void processFileExport(TypedProperties props, String jobName) throws MegatronException {
        FileExportManager fileExportManager = new FileExportManager(props);
        fileExportManager.execute(jobName);
    }

    
    /**
     * Deletes specified log job including entries but not mail jobs.
     */
    public void delete(TypedProperties globalProps, String jobName) throws MegatronException {
        log.info("Deleting log job: " + jobName);
        deleteInternal(globalProps, jobName, false);
    }

    
    /**
     * Deletes specified log job including entries and mail jobs.
     */
    public void deleteAll(TypedProperties globalProps, String jobName) throws MegatronException {
        log.info("Deleting log job (including mail jobs): " + jobName);
        deleteInternal(globalProps, jobName, true);
    }

    
    private void moveFile(File file, File destDir) {
        log.debug("Moving file: " + file.getAbsolutePath() + " --> " + destDir.getAbsolutePath());
        File destFile = new File(destDir, file.getName());
        if (destFile.exists()) {
            destFile = new File(destDir, file.getName() + "_" + System.currentTimeMillis());
            log.info("Destination file exists. New name: " + destFile.getAbsolutePath());
        }
        boolean success = file.renameTo(destFile);
        if (!success) {
            log.error("Cannot move file in slurp directory: " + file.getAbsolutePath());
        }
    }
    
    
    private void deleteInternal(TypedProperties props, String jobName, boolean includeMailJobs) throws MegatronException {
        DbManager dbManager = null;
        try {
            // find job
            dbManager = DbManager.createDbManager(props);
            Job job = dbManager.searchLogJob(jobName);
            if (job == null) {
                String msg = "Cannot find job: " + jobName;
                throw new MegatronException(msg);
            }
            JobContext tmpContext = new JobContext(props, job);
            
            // -- find and delete mail jobs
            List<MailJob> mailJobs = dbManager.searchMailJobs(job);
            if (mailJobs.size() > 0) {
                if (includeMailJobs) {
                    log.info("No. of mail jobs to delete: " + mailJobs.size());
                    for (Iterator<MailJob> iterator = mailJobs.iterator(); iterator.hasNext(); ) {
                        MailJob mailJob = iterator.next();
                        log.info("Deleting mail job#" + mailJob.getId());
                        long noOfMailedLogEntries = dbManager.deleteMailJob(mailJob);
                        String msg = "Mail job#" + mailJob.getId() + " deleted. No. of mailed log entries: " + noOfMailedLogEntries;
                        tmpContext.writeToConsole(msg);
                        log.info(msg);
                    }
                } else {
                    throw new MegatronException("Mail jobs exists for specified log job. Use --delete-all to delete mail jobs as well.");
                }
            }
            
            // -- delete log job 
            log.info("Deleting log job#" + job.getId());
            long noOfDeletedEntries = dbManager.deleteLogJob(job);
            String msg = "Log job#" + job.getId() + " deleted. No. of log entries deleted: " + noOfDeletedEntries;
            tmpContext.writeToConsole(msg);
            log.info(msg);
        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
    }
    
}
