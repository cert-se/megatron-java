package se.sitic.megatron.core;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Exports data in the database for a log job to file.
 */
public class FileExportManager extends AbstractExportManager  {
	private final Logger log = Logger.getLogger(FileExportManager.class);
	private final int NO_OF_RECORDS = 1000;

    private FileExporter fileExporter;

    
    /**
     * Constructor.
     */
    public FileExportManager(TypedProperties props) {
        super(props);
	}
    
    
    /**
     * Exports specified job to file.
     */
    public void execute(String jobName) throws MegatronException {
        try {
            this.jobName = jobName;
            log.info("Exporting job to file: " + jobName);
            init();
            
            fileExporter.writeHeader(job);
            
            // -- read and export log entries
            int startIndex = 0;
            List<LogEntry> logEntries = dbManager.listLogEntries(job.getId(), startIndex, NO_OF_RECORDS);
            log.info("No. of log entries fetched: " + logEntries.size());
            while (!logEntries.isEmpty()) {
                for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
                    processLogEntry(iterator.next());
                }
                startIndex += NO_OF_RECORDS;
                logEntries = dbManager.listLogEntries(job.getId(), startIndex, NO_OF_RECORDS);
                log.info("No. of log entries fetched: " + logEntries.size());
            }
            
            fileExporter.writeFooter(job);
        } catch (Exception e) {
            String msg = "File export of job (" + jobName + ") failed: " + e.getMessage();
            // stack-trace is logged elsewhere
            log.error(msg);
            MegatronException newException = (e instanceof MegatronException) ? (MegatronException)e : new MegatronException(msg, e);
            msg = "Job failed: " + e.getMessage();
            throw newException;
        } finally {
            decoratorManager.closeDecorators();
            filterManager.closeFilters();
            try { if (fileExporter != null) fileExporter.close(); } catch (Exception ignored) {}
            try { dbManager.close(); } catch (Exception ignored) {}
        }
        
        // -- write finished message to log and console
        String msg = "Export finished. Name: @jobName@,  Exported Entries: @noOfExportedEntries@, " + 
            "Filtered Entries: @noOfFilteredLines@, Duration: @duration@";
        msg = StringUtil.replace(msg, "@jobName@", "" + jobName);
        msg = StringUtil.replace(msg, "@noOfExportedEntries@", "" + jobContext.getNoOfExportedEntries());
        msg = StringUtil.replace(msg, "@noOfFilteredLines@", "" + jobContext.getNoOfFilteredLines());
        String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - jobContext.getStartedTimestamp());
        msg = StringUtil.replace(msg, "@duration@", durationStr);
        jobContext.writeToConsole(msg);
        log.info(msg);

        if (fileExporter != null) {
            fileExporter.writeFinishedMessageToLog();
        }
    }

    
    @Override
    protected JobContext createJobContext() throws MegatronException {
        JobContext result = new JobContext(props, job); 
        result.setStartedTimestamp(System.currentTimeMillis());
        result.setDbManager(dbManager);

        return result;
    }

    
    @Override
    protected String getPreExportFiltersPropertyKey() {
        return AppProperties.FILTER_PRE_EXPORT_KEY;
    }

    
    @Override
    protected String getPreExportDecoratorsPropertyKey() {
        return AppProperties.DECORATOR_PRE_EXPORT_CLASS_NAMES_KEY;
    }

    
    @Override
    protected void init() throws MegatronException {
        super.init();
        
        // FileExporter
        fileExporter = new FileExporter(jobContext);
    }
    
    
    private void processLogEntry(LogEntry logEntry) throws MegatronException {
        decoratorManager.executeDecorators(logEntry);
        
        if (!filterManager.executeFilters(logEntry)) {
            jobContext.incNoOfFilteredLines(1);
            return;
        }

        fileExporter.writeLogEntry(logEntry);
        jobContext.incNoOfExportedEntries(1);
    }
    
}
