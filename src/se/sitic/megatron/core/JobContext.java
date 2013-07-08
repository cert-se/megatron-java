package se.sitic.megatron.core;

import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.Job;


/**
 * Context for a log job that is shared between processors of a log line, e.g.
 * line processor, filters, parser, decorators etc. This object may be used to
 * share data from one processor to another.
 */
public class JobContext {
    private TypedProperties props;
    private Job job;
    private DbManager dbManager;
    
    /** Time job was started (in ms). */
    private long startedTimestamp;

    /** Total number of lines in file. */
    private long noOfLines = -1L;

    /** Line number in file that is currently processing. */
    private long lineNo = 0L;

    /** As lineNo, but after optional ILineProcessor-step which merge or split lines. */
    private long lineNoAfterProcessor = 0L;
    
    /** No. of lines filtered. If a line processor is used this number may be skewed (filters may exist before and after a merger or splitter). */
    private long noOfFilteredLines = 0L;

    /** No. of parse errors. */
    private long noOfParseExceptions = 0L;

    /** No. of saved log entries. */
    private long noOfSavedEntries = 0L;

    /** No. of log entries connected to high priority organizations. */
    private long noOfHighPriorityEntries = 0L;

    /** No. of exported log entries. */
    private long noOfExportedEntries = 0L;
    
    
    // TODO add map for additional data, e.g. Map<String, Object> data


    /**
     * Constructor.
     */
    public JobContext(TypedProperties props, Job job) {
        this.props = props;
        this.job = job;
    }

    
    /**
     * Displays message to operator. If CLI, the console is used.
     * If web application, the message is appended to a buffer that
     * is displayed at the end of the job.  
     */
    public void writeToConsole(String msg) {
        System.out.println(msg);
    
        // TODO add support for web application
    }

    
    public TypedProperties getProps() {
        return this.props;
    }

    
    public Job getJob() {
        return this.job;
    }

    
    public DbManager getDbManager() {
        return dbManager;
    }


    public void setDbManager(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    
    public long getLineNo() {
        return lineNo;
    }


    public void incLineNo(int incValue) {
        this.lineNo += incValue;
    }

    
    public long getLineNoAfterProcessor() {
        return lineNoAfterProcessor;
    }


    public void incLineNoAfterProcessor(int incValue) {
        this.lineNoAfterProcessor += incValue;
    }

    
    public long getNoOfFilteredLines() {
        return noOfFilteredLines;
    }


    public void incNoOfFilteredLines(int incValue) {
        this.noOfFilteredLines += incValue;
    }


    public long getNoOfParseExceptions() {
        return noOfParseExceptions;
    }


    public void incNoOfParseExceptions(int incValue) {
        this.noOfParseExceptions += incValue;
    }

    
    public long getNoOfSavedEntries() {
        return noOfSavedEntries;
    }


    public void incNoOfSavedEntries(int incValue) {
        this.noOfSavedEntries += incValue;
    }

    
    public long getNoOfHighPriorityEntries() {
        return noOfHighPriorityEntries;
    }


    public void incNoOfHighPriorityEntries(int incValue) {
        this.noOfHighPriorityEntries += incValue;
    }

    
    public long getNoOfExportedEntries() {
        return noOfExportedEntries;
    }


    public void incNoOfExportedEntries(int incValue) {
        this.noOfExportedEntries += incValue;
    }

    
    public long getStartedTimestamp() {
        return startedTimestamp;
    }


    public void setStartedTimestamp(long startedTimestamp) {
        this.startedTimestamp = startedTimestamp;
    }


    public long getNoOfLines() {
        return noOfLines;
    }


    public void setNoOfLines(long noOfLines) {
        this.noOfLines = noOfLines;
    }

}
