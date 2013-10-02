package se.sitic.megatron.fileprocessor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.FileUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Executes an OS command on the input file.
 */
public class OsCommandProcessor implements IFileProcessor {
    private static final Logger log = Logger.getLogger(OsCommandProcessor.class);

    private static final String TMP_PREFIX = "megatron-OsCommandProcessor-"; 
    private static final String TMP_SUFFIX = ".txt";

    private String osCommand;
    private File tmpDir;
    private boolean deleteTmpFiles;

    
    public OsCommandProcessor() {
        // emptry
    }
    
    
    @Override
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        
        osCommand = props.getString(AppProperties.FILE_PROCESSOR_OS_COMMAND_KEY, null);
        
        tmpDir = new File(props.getString(AppProperties.TMP_DIR_KEY, "tmp"));
        try {
            FileUtil.ensureDir(tmpDir);
        } catch (IOException e) {
            String msg = "Cannot create directory for temporary files: " + tmpDir.getAbsolutePath();
            throw new MegatronException(msg, e);
        }
        deleteTmpFiles = props.getBoolean(AppProperties.FILE_PROCESSOR_DELETE_TMP_FILES_KEY, true);
    }
    
    
    public void setOsCommand(String osCommand) {
        this.osCommand = osCommand;
    }

    
    @Override
    public File execute(File inputFile) throws MegatronException {
        if (osCommand == null) {
            String msg = "Mandatory property not defined: " + AppProperties.FILE_PROCESSOR_OS_COMMAND_KEY;
            throw new MegatronException(msg);
        }
        
        File result = createTemporaryFile(TMP_PREFIX);
        
        String cmd = StringUtil.replace(osCommand, "$inputFile", inputFile.getAbsolutePath());
        try {
            // -- execute OS-command
            log.info("Exectuing OS-command: " + cmd);
            long t1 = System.currentTimeMillis();
            Process process = Runtime.getRuntime().exec(cmd);
            // -- read output
            BufferedOutputStream out = null;
            InputStream in = null;
            try {
                out = new BufferedOutputStream(new FileOutputStream(result));
                in = process.getInputStream();
                int chr = 0;
                while ((chr = in.read()) != -1) {
                    out.write(chr);
                }
            } catch (IOException e) {
                throw new MegatronException("Cannot read output from OS-command.", e);
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignored) {}
                try { if (out != null) out.close(); } catch (Exception ignored) {}
            }
            // -- check exit code
            if (process.waitFor() != 0) {
                log.warn("OS-command terminated with a non-zero exit code: " + process.exitValue());
            } else {
                String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - t1);
                log.debug("OS-command finished. Duration: " + durationStr);
            }
        } catch (Exception e) {
            // IOException, InterruptedException
            String msg = "Cannot execute OS-command: " + cmd;
            throw new MegatronException(msg, e);
        }
        
        return result;
    }


    @Override
    public void close(boolean jobSuccessful) throws MegatronException {
        // empty
    }
    
    
    protected File createTemporaryFile(String prefix) throws MegatronException {
        // TODO delete old temporary files that have not been deleted due to a crash
        
        File result = null;
        try {
            result = File.createTempFile(prefix, TMP_SUFFIX, tmpDir);
            if (deleteTmpFiles) {
                result.deleteOnExit();
            }
        } catch (IOException e) {
            String msg = "Cannot create temporary file.";
            throw new MegatronException(msg, e);
        }
        return result;
    }

}
