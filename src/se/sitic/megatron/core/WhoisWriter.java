package se.sitic.megatron.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.FileUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Runs the whois config for IPs or hostnames, which are specified at the 
 * command line or in a file. Processes the "--whois" switch, and writes output
 * to stdout or to a string. By default, the config will display the following
 * fields: IP, AS, CC, hostname, AS name, and organization.
 */
public class WhoisWriter {
    private final Logger log = Logger.getLogger(WhoisWriter.class);
    private static final String IP_REG_EXP = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})";
    private static final String TMP_PREFIX = "megatron-whois-"; 
    private static final String TMP_SUFFIX = ".txt";
    private static final String JOB_TYPE_IP = "megatron-whois-ip";
    private static final String JOB_TYPE_HOSTNAME = "megatron-whois-hostname";

    private TypedProperties globalProps;
    private TypedProperties props; 
    private boolean toStdout;
    private File tmpDir;
    private File inputFile; 

    
    public WhoisWriter(TypedProperties globalProps, boolean toStdout) {
        this.globalProps = globalProps;
        this.toStdout = toStdout;
    }
    
    
    public void execute() throws MegatronException {
        init();
        JobScheduler.getInstance().processFile(props, inputFile);
    }

    
    public String getContent() {
        // TODO return content in created file (JobScheduler must return a filename or file content).
        return null;
    }

    
    private void init() throws MegatronException {
        tmpDir = new File(globalProps.getString(AppProperties.TMP_DIR_KEY, "tmp"));
        
        // inputFiles may be one of the following (can not mix IPs and hostnames):
        //   1. a file
        //   2. list of IPs
        //   3. list hostnames (or urls)
        List<String> inputFiles = AppProperties.getInstance().getInputFiles();
        // -- read or create input file
        String firstLine = null;
        if (inputFiles.size() == 0) {
            // already checked in AppProperties.parseCommandLine
            throw new MegatronException("Missing argument; needs a filename, or a list of IPs or hostnames."); 
        }
        inputFile = new File(inputFiles.get(0));
        if (inputFile.canRead()) {
            if (inputFiles.size() > 1) {
                throw new MegatronException("Only one file can be specified at the command-line.");
            }
            log.debug("Reading first line in file: " + inputFile.getAbsolutePath());
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), Constants.UTF8));
                String line = null;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if ((line.length() > 0) && !line.startsWith("#")) {
                        firstLine = line;
                        break;
                    }
                }
            } catch (IOException e) {
                throw new MegatronException("Cannot read input file: " + inputFile.getAbsolutePath(), e);
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignored) {}
            }
        } else {
            inputFile = createTemporaryFile();
            StringBuilder sb = new StringBuilder(2*1024);
            for (Iterator<String> iterator = inputFiles.iterator(); iterator.hasNext(); ) {
                String line = iterator.next().trim();
                if ((firstLine == null) && (line.length() > 0) && !line.startsWith("#")) {
                    firstLine = line;
                }
                sb.append(line).append(Constants.LINE_BREAK);
            }
            try {
                FileUtil.writeFile(inputFile, sb.toString(), Constants.UTF8);
            } catch (IOException e) {
                throw new MegatronException("Cannot write temporary file: " + inputFile.getAbsolutePath(), e);
            }
        }
        // -- determine job-type
        String jobType = null;
        Matcher matcher = Pattern.compile(IP_REG_EXP).matcher(StringUtil.getNotNull(firstLine));
        if (matcher.find()) {
            if (matcher.groupCount() > 1) {
                throw new MegatronException("Invalid input file; only one IP per line is allowed.");
            }
            jobType = JOB_TYPE_IP;
            log.debug("IP address found as first entry in command-line or file. Using job-type: " + jobType);
        } else {
            jobType = JOB_TYPE_HOSTNAME;
            log.debug("Assumes entries are hostnames or URLs. Using job-type: " + jobType);            
        }
        // -- init props
        props = AppProperties.getInstance().createTypedPropertiesForCli(jobType);
        HashMap<String, String> additionalProps = new HashMap<String, String>();
        props.addAdditionalProps(additionalProps);
        // --export
        additionalProps.put(TypedProperties.CLI_EXPORT_KEY, "true");
        // --no-db
        additionalProps.put(TypedProperties.CLI_NO_DB_KEY, "true");
        // --stdout
        if (toStdout) {
            additionalProps.put(TypedProperties.CLI_STDOUT_KEY, "true");
        }
    }

    
    private File createTemporaryFile() throws MegatronException {
        // TODO delete old temporary files that have not been deleted due to a crash
        
        File result = null;
        try {
            result = File.createTempFile(TMP_PREFIX, TMP_SUFFIX, tmpDir);
            result.deleteOnExit();
        } catch (IOException e) {
            String msg = "Cannot create temporary file.";
            throw new MegatronException(msg, e);
        }
        return result;
    }


}
