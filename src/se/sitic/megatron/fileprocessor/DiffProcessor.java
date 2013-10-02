package se.sitic.megatron.fileprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.JobType;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.FileUtil;
import se.sitic.megatron.util.ObjectStringSorter;
import se.sitic.megatron.util.StringUtil;


/**
 * Diff current input file with the one in previous run, and filter out old 
 * lines. Use this processor if the input file contains existing lines as well
 * as new lines. This is the case with RBL files.  
 */
public class DiffProcessor extends OsCommandProcessor {
    private static final Logger log = Logger.getLogger(DiffProcessor.class);
    private static final String FILE_PREFIX_SEPARATOR = "__";
    private static final String TMP_PREFIX = "megatron-new-and-modified-lines-"; 
    private static final String CURRENT_EXT = ".txt"; 
    private static final String BACKUP_EXT = ".bak"; 
    
    /** Property: fileProcessor.diffProcessor.oldFilesDir */
    private File oldFilesDir;
    /** oldFilesDir + jobType */
    private File workDir;
    /** oldFilesDir + jobType + input filename prefix (skips timestamp) */
    private File filePrefix;
    private TypedProperties props;
    private File inputFile;
    
    
    public DiffProcessor() {
        super();
    }
    
    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        super.init(jobContext);
        props = jobContext.getProps();
        
        // init filePrefix
        oldFilesDir = new File(props.getString(AppProperties.FILE_PROCESSOR_DIFF_OLD_FILES_DIR_KEY, "diff-processor-old-files"));
        JobType jobType = jobContext.getJob().getJobType();
        workDir = new File(oldFilesDir, jobType.getName());
        try {
            FileUtil.ensureDir(workDir);
        } catch (IOException e) {
            String msg = "Cannot create directory for diff files: " + workDir.getAbsolutePath();
            throw new MegatronException(msg, e);
        }
        String[] headTail = StringUtil.splitHeadTail(jobContext.getJob().getFilename(), FILE_PREFIX_SEPARATOR, false);
        if ((headTail[0].length() > 0) && (headTail[1].length() > 0)) {
            filePrefix = new File(workDir, headTail[0]);
        } else {
            filePrefix = new File(workDir, workDir.getName());
        }
        log.debug("Prefix for old input files: " + filePrefix);
    }
    
    
    @Override
    public File execute(File inputFile) throws MegatronException {
        this.inputFile = inputFile; 
        String diffCmd = props.getString(AppProperties.FILE_PROCESSOR_DIFF_COMMAND_KEY, "diff $oldFile $newFile");
        File oldFile = new File(filePrefix + CURRENT_EXT);
        if (!oldFile.isFile()) {
            log.info("No old file exists to diff. Assumes that all lines in the input file are new.");
            return inputFile;
        }
        
        diffCmd = StringUtil.replace(diffCmd, "$oldFile", oldFile.getAbsolutePath());
        diffCmd = StringUtil.replace(diffCmd, "$newFile", "$inputFile");
        setOsCommand(diffCmd);
        
        File diffFile = super.execute(inputFile);
        if (!diffFile.isFile()) {
            throw new MegatronException("Diff-file not created. OS-command: " + diffCmd);
        }

        return parseDiffFile(diffFile);
    }
    
    
    @Override
    public void close(boolean jobSuccessful) throws MegatronException {
        if (jobSuccessful) {
            rotateBackupFiles();
            copyToOldFilesDir();
        }
    }

    
    /**
     * Parsers specified file and returns a created file that contains new and 
     * modified lines.
     */
    private File parseDiffFile(File file) throws MegatronException {
        File result = createTemporaryFile(TMP_PREFIX);
        log.debug("Parsing diff-file: " + file.getAbsolutePath());
        log.debug("Writing new and modified lines to file: " + result.getAbsolutePath());
        
        BufferedReader in = null;
        BufferedWriter out = null;

        int newLines = 0;
        int deletedLines = 0;
        int modifiedLines = 0;
        try {
            out = new BufferedWriter(new FileWriter(result));
            in = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("> ")) {
                    ++newLines;
                    out.write(line.substring("> ".length()));
                    out.write(Constants.LINE_BREAK);
                } else if (line.startsWith("< ")) {
                    ++deletedLines;
                } else if (line.startsWith("---")) {
                    --newLines;
                    --deletedLines;
                    ++modifiedLines;
                } else {
                    // empty; skip position marker
                }
            }
        } catch (IOException e) {
            String msg = "Cannot parse diff-file and create file with new and modified lines.";
            throw new MegatronException(msg, e);
        } finally {
            try { if (out != null) out.close(); } catch(Exception ignored) {}
            try { if (in != null) in.close(); } catch(Exception ignored) {}
        }
        log.info("Diff-file parsed. New Lines: " + newLines + ", Modified Lines: " + modifiedLines + ", Deleted Lines: " + deletedLines);
        
        return result;
    }
    
    
    /**
     * Renames backup files and deletes excessive backup files. Example: shadowserver-drone.bak1 --> shadowserver-drone.bak2,
     * shadowserver-drone.bak2 --> shadowserver-drone.bak3 etc. 
     */
    private void rotateBackupFiles() {
        // -- read props
        int noOfBackupsToKeep = props.getInt(AppProperties.FILE_PROCESSOR_DIFF_NO_OF_BACKUPS_TO_KEEP_KEY, 4);
        if (noOfBackupsToKeep < 1) {
            log.debug("Skip rotating backup files due to property: " + AppProperties.FILE_PROCESSOR_DIFF_NO_OF_BACKUPS_TO_KEEP_KEY);
            return;
        }

        // -- sort backup files (reverse order)
        final String backupShortPrefix = new File(filePrefix + BACKUP_EXT).getName();
        File[] fileArray = workDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().startsWith(backupShortPrefix) && pathname.isFile();
                }
            });
        List<File> files = Arrays.asList(fileArray);
        Collections.sort(files, new ObjectStringSorter() {
                @Override
                protected String getObjectString(Object obj) {
                    return ((File)obj).getName();
                }
            });
        Collections.reverse(files);

        // -- delete or rename backup file
        for (Iterator<File> iterator = files.iterator(); iterator.hasNext(); ) {
            File file = iterator.next();
            String[] headTail = StringUtil.splitHeadTail(file.getName(), BACKUP_EXT, true);
            if ((headTail != null) && (headTail[1].length() > 0)) {
                try {
                    int generation = Integer.parseInt(headTail[1]);
                    if (generation >= noOfBackupsToKeep) {
                        log.info("Deleting backup file: " + file.getAbsolutePath());
                        boolean deleteOk = file.delete();
                        if (!deleteOk) {
                            log.error("Cannot delete backup file: " + file.getAbsolutePath());
                            return;
                        }
                    } else {
                        File toFile = new File(filePrefix + BACKUP_EXT + (generation + 1));
                        log.info("Renaming file: " + file.getName() + " --> " + toFile.getName());
                        boolean renameOk = file.renameTo(toFile);
                        if (!renameOk) {
                            log.error("Cannot rename file: " + file.getAbsoluteFile() + " --> " + toFile.getAbsoluteFile());
                            return;
                        }
                    }
                } catch (NumberFormatException e) {
                    log.error("Cannot rotate file: " + file.getAbsolutePath(), e);
                    return;
                }
            } else {
                log.error("Cannot rotate file: " + file.getAbsolutePath());
                return;
            }
        }
    }

    
    /**
     * Copy input file to old files directory.
     */
    private void copyToOldFilesDir() throws MegatronException {
        File currentFile = new File(filePrefix + CURRENT_EXT);
        
        // -- read props; create backup? 
        int noOfBackupsToKeep = props.getInt(AppProperties.FILE_PROCESSOR_DIFF_NO_OF_BACKUPS_TO_KEEP_KEY, 4);
        if (noOfBackupsToKeep >= 1) {
            if (currentFile.exists()) {
                // -- create backup file
                File targetFile = new File(filePrefix + BACKUP_EXT + "1");
                try {
                    FileUtil.copyFile(currentFile, targetFile, false);
                } catch (IOException e) {
                    throw new MegatronException("Cannot create backup file.", e);
                }
            } else {
                log.debug("Backup not created; current file is missing: " + currentFile.getAbsolutePath());
            }
        } else {
            log.debug("Skip creating a backup file due to property: " + AppProperties.FILE_PROCESSOR_DIFF_NO_OF_BACKUPS_TO_KEEP_KEY);
        }
        
        // -- copy to current file
        try {
            FileUtil.copyFile(inputFile, currentFile, true);
        } catch (IOException e) {
            throw new MegatronException("Cannot copy input file to old files directory.", e);
        }
    }

}
