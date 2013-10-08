package se.sitic.megatron.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.parser.LogEntryMapper;
import se.sitic.megatron.util.AppUtil;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Exports log entries to a file.
 */
public class FileExporter extends AbstractExporter {
    private static final Logger log = Logger.getLogger(FileExporter.class);
    
    // Variables in header and footer
    private static final String JOB_NAME = "jobName";
    private static final String FILENAME = "filename";
    private static final String FILE_HASH = "fileHash";
    // UNUSED: private static final String FILE_SIZE = "fileSize";
    private static final String JOB_STARTED = "jobStarted";

    private static final String EXPORT_STARTED = "exportStarted";
    private static final String EXPORT_FILENAME = "exportFilename";
    private static final String EXPORT_FULL_FILENAME = "exportFullFilename";
    
    private File file;
    private boolean xmlFormat = false;
    private Map<String, String> headerMap;
    private Map<String, String> footerMap;
    private String[][] replaceArray;
    private String separator;
    private int noOfLogEntriesWritten;
    private BufferedWriter out;
    
    
    public FileExporter(JobContext jobContext) throws MegatronException {
        super(jobContext);
        init();
    }

    
    public void setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
    }


    public void setFooterMap(Map<String, String> footerMap) {
        this.footerMap = footerMap;
    }

    
    public void setReplaceArray(String[][] replaceArray) {
        this.replaceArray = replaceArray;
    }

    
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    
    public void writeHeader(Job job) throws MegatronException {
        log.info("Writing export file: " + file.getAbsolutePath());
        openFile();
        String template = readTemplate(AppProperties.EXPORT_HEADER_FILE_KEY, false);
        if (template != null) {
            String templateName = props.getString(AppProperties.EXPORT_HEADER_FILE_KEY, null);
            Map<String, String> attrMap = createAttributeMap(job);
            if (headerMap != null) {
                attrMap.putAll(headerMap);
            }
            template = AppUtil.replaceVariables(template, attrMap, xmlFormat, templateName);
            writeString(template);
        }
    }

    
    public void writeLogEntry(LogEntry logEntry) throws MegatronException {
        String template = readTemplate(AppProperties.EXPORT_ROW_FILE_KEY, true);
        LogEntryMapper mapper = new LogEntryMapper(props, rewriter, logEntry);
        String templateName = props.getString(AppProperties.EXPORT_ROW_FILE_KEY, null);
        template = mapper.replaceVariables(template, xmlFormat, templateName);
        if ((separator != null) && (noOfLogEntriesWritten > 0)) {
            template = separator + template;
        }
        writeString(template);
        ++noOfLogEntriesWritten;
    }

    
    public void writeFooter(Job job) throws MegatronException {
        String template = readTemplate(AppProperties.EXPORT_FOOTER_FILE_KEY, false);
        if (template != null) {
            String templateName = props.getString(AppProperties.EXPORT_FOOTER_FILE_KEY, null);
            Map<String, String> attrMap = createAttributeMap(job);
            if (footerMap != null) {
                attrMap.putAll(footerMap);
            }
            template = AppUtil.replaceVariables(template, attrMap, xmlFormat, templateName);
            writeString(template);
        }
    }

    
    public void close() throws MegatronException {
        try {
            if (out != null) {
                out.close();
                out = null;
            }
        } catch (IOException e) {
            throw new MegatronException("Cannot close export file: " + file.getAbsolutePath());
        }
    }
    
    
    public void writeFinishedMessageToLog() {
        String msg = "Export file created: @filename@ (@size@ bytes).";
        msg = StringUtil.replace(msg, "@filename@", "" + file.getAbsolutePath());
        msg = StringUtil.replace(msg, "@size@", "" + file.length());
        jobContext.writeToConsole(msg);
        log.info(msg);
    }
    
    
    @Override
    protected String getTemplateDir() {
        return props.getString(AppProperties.EXPORT_TEMPLATE_DIR_KEY, "conf/template/export");
    }
    
    
    @Override
    protected String getTimestampFormat() {
        return props.getString(AppProperties.EXPORT_TIMESTAMP_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss z");
    }


    @Override    
    protected void init() throws MegatronException {
        super.init();

        String dirName = props.getString(AppProperties.OUTPUT_DIR_KEY, "tmp/export");
        String rowFilename = props.getString(AppProperties.EXPORT_ROW_FILE_KEY, ".txt");
        String ext = rowFilename.endsWith(".xml") ? ".xml" : ".txt";
        String filename = jobContext.getJob().getName();
        if (filename.indexOf('.') == -1) {
            filename = filename + ext;
        }
        this.file = new File(dirName, filename);
        this.xmlFormat = file.getName().toLowerCase().endsWith(".xml");
    }

    
    private void openFile() throws MegatronException {
        try {
            String charSet = props.getString(AppProperties.EXPORT_CHAR_SET_KEY, Constants.UTF8);
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charSet));
        } catch (IOException e) {
            String msg = "Cannot open export file: " + file.getAbsolutePath();
            throw new MegatronException(msg, e);
        }
    }

    
    private Map<String, String> createAttributeMap(Job job) {
        Map<String, String> result = new HashMap<String, String>();
        
        addString(result, JOB_NAME, job.getName());
        addString(result, FILENAME, job.getFilename());
        addString(result, FILE_HASH, job.getFileHash());        
        addTimestamp(result, JOB_STARTED, job.getStarted());        
        addTimestamp(result, EXPORT_STARTED, SqlUtil.convertTimestampToSec(jobContext.getStartedTimestamp()));        
        addString(result, EXPORT_FILENAME, file.getName());        
        addString(result, EXPORT_FULL_FILENAME, file.getAbsolutePath());        

        return result;
    }

    
    private void writeString(String str) throws MegatronException {
        // replace
        if (replaceArray != null) {
            for (int i = 0; i < replaceArray.length; i++) {
                String from = replaceArray[i][0];
                String to = replaceArray[i][1];
                str = StringUtil.replace(str, from, to);
            }
        }
        
        try {
            out.write(str);
        } catch (IOException e) {
            String msg = "Cannot write to export file: " + file.getAbsolutePath();
            throw new MegatronException(msg, e);
        }
    }

}
