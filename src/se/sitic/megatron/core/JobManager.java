package se.sitic.megatron.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.decorator.DecoratorManager;
import se.sitic.megatron.decorator.OrganizationMatcherDecorator;
import se.sitic.megatron.entity.Job;
import se.sitic.megatron.entity.JobType;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.entity.OriginalLogEntry;
import se.sitic.megatron.fileprocessor.IFileProcessor;
import se.sitic.megatron.filter.ILineFilter;
import se.sitic.megatron.filter.LogEntryFilterManager;
import se.sitic.megatron.lineprocessor.ILineProcessor;
import se.sitic.megatron.mail.MailSender;
import se.sitic.megatron.parser.IParser;
import se.sitic.megatron.parser.RegExpParser;
import se.sitic.megatron.rss.JobRssFile;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.FileUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Coordinates execution of a log job. Handles the workflow and manage calls 
 * to for example line processor, filters, parser, decorators etc. 
 */
public class JobManager {
    private static final Logger log = Logger.getLogger(JobManager.class);
    private static final int MAX_NO_OF_PARSE_ERROR_TO_CONSOLE = 20;
    
    private TypedProperties props;
    private JobContext jobContext;
    private Job job;
    private boolean removeTrailingSpaces;
    
    private DbManager dbManager;
    private IParser parser;
    private List<IFileProcessor> fileProcessors;
    private ILineProcessor lineProcessor; 
    private List<ILineFilter> preLineProcessorFilters;
    private List<ILineFilter> preParserFilters;
    private LogEntryFilterManager preDecoratorFilterManager;
    private LogEntryFilterManager preStorageFilterManager;
    private LogEntryFilterManager preExportFilterManager;
    private DecoratorManager preStorageDecoratorManager;
    private DecoratorManager preExportDecoratorManager;
    private FileExporter fileExporter;
    private Long oldestLogTimestamp;
    private long printProgressInterval;
    private long lastProgressPrintTime;
    private long lastProgressPrintLineNo;
    
    
    /**
     * Constructor.
     */
    public JobManager(TypedProperties props) {
        this.props = props;
    }
    

    /**
     * Process specified file; parses file, filters, decorates, save to db,
     * and export data.
     */
    public void execute(File file) throws MegatronException {
        try {
            log.info("Starting job for file: " + file.getAbsolutePath());
            
            // -- init
            dbManager = DbManager.createDbManager(props);
            jobContext = createJobContext(file);
            job = jobContext.getJob();
            log.info("Job name: " + job.getName()); 
            init();

            // -- check if file already processed
            if (!props.isNoDb() && dbManager.existsFileHash(job.getFileHash())) {
            	String action = props.getString(AppProperties.FILE_ALREADY_PROCESSED_ACTION_KEY, "error");
            	if (action.equalsIgnoreCase("error")) {
                    String msg = "File have already been processed; file hash (" + job.getFileHash() + ") exists in the database. " + 
                    "Use --delete or --delete-all to rerun file.";
                    throw new MegatronException(msg);
            	} else if (action.equalsIgnoreCase("skip")) {
            	    String msg = "File is skipped because it have already been processed; file hash (" + job.getFileHash() + ") exists in the database.";
            	    log.info(msg);
            	    jobContext.writeToConsole(msg);
            	    return;
            	} else if (action.equalsIgnoreCase("rerun")) {
                    String msg = "Rerunning file; file hash (" + job.getFileHash() + ") exists in the database.";
                    log.info(msg);
            	} else {
                    String msg = "Invalid property value for " + AppProperties.FILE_ALREADY_PROCESSED_ACTION_KEY + ": " + action;
            	    throw new MegatronException(msg);
            	}
            }
            
            // -- process job
            dbManager.addLogJob(job);
            
            // -- process file
            if ((fileProcessors != null) && (fileProcessors.size() > 0)){
                for (Iterator<IFileProcessor> iterator = fileProcessors.iterator(); iterator.hasNext(); ) {
                    IFileProcessor fileProcessor = iterator.next();
                    log.debug("Executing file processor: " + fileProcessor.getClass().getName());
                    file = fileProcessor.execute(file);    
                    log.debug("File processor finished. New input filename: " + file.getAbsolutePath());
                }
                // re-calc no. of lines for new file
                long noOfLines = countNoOfLines(file);
                log.info("File processor executed. No. of lines before and after: " + jobContext.getNoOfLines() + " -- " + noOfLines);
                jobContext.setNoOfLines(noOfLines);
            }
            
            // -- process line by line
            BufferedReader in = null;
            // init attributes for printProgress 
            lastProgressPrintTime = System.currentTimeMillis(); 
            lastProgressPrintLineNo = 0L;
            try {
                String charSet = props.getString(AppProperties.INPUT_CHAR_SET_KEY, Constants.UTF8);
                in = new BufferedReader(new InputStreamReader(new FileInputStream(file), charSet));
                String line = null;
                while ((line = in.readLine()) != null) {
                    processLine(line);
                }
            } catch (IOException e) {
                String msg = "Cannot read file: " + file.getAbsolutePath();
                throw new MegatronException(msg, e);
            } finally {
                try { if (in != null) in.close(); } catch (Exception ignored) {}
            }
            
            // -- finishing job
            closeFileProcessors(fileProcessors, true);
            fileProcessors = null;
            
            job.setProcessedLines(jobContext.getLineNoAfterProcessor());
            dbManager.finishLogJob(job, null);
            
            if (fileExporter != null) {
                fileExporter.writeFooter(job);
            }
            
            // -- write finished message to log and console
            writeFinishedMessage();
            if (fileExporter != null) {
                // close() will flush file; file size will now be reported correct 
                fileExporter.close();
                fileExporter.writeFinishedMessageToLog();
            }
        
            // -- send email if high priority organizations exists
            OrganizationMatcherDecorator organizationMatcher = preStorageDecoratorManager.getOrganizationMatcher();
            if (!props.isNoDb() && (organizationMatcher != null)) {
                List<Organization> organizations = organizationMatcher.getHighPriorityOrganizations();
                if (organizations.size() > 0) {
                    sendNotificationEmail(organizations);    
                } else {
                    log.info("No notification email sent; no high priority organizations found in job.");
                }
            }
        } catch (Exception e) {
        	String jobName = (job != null) ? job.getName() : "[job name is null]";
        	String msg = "Job (" + jobName + ") failed: " + e.getMessage();
            // stack-trace is logged elsewhere
            log.error(msg);
            MegatronException newException = (e instanceof MegatronException) ? (MegatronException)e : new MegatronException(msg, e);
            msg = "Job failed: " + e.getMessage();
            if ((job != null) && (job.getId() != null) && (job.getId() > 0L)) {
                try {
                    if (jobContext != null) {
                        job.setProcessedLines(jobContext.getLineNoAfterProcessor());
                    }
                    dbManager.finishLogJob(job, msg);
                } catch (MegatronException e2) {
                    String msg2 = "Cannot write error message to job; finishLogJob failed.";
                	log.error(msg2, e2);
                }
            }
            throw newException;
        } finally {
            closeFileProcessors(fileProcessors, false);
            closeLineFilters(preLineProcessorFilters);
            closeLineFilters(preParserFilters);
            if (preDecoratorFilterManager != null) preDecoratorFilterManager.closeFilters();
            if (preStorageFilterManager != null) preStorageFilterManager.closeFilters();
            if (preExportFilterManager != null) preExportFilterManager.closeFilters();
            if (preStorageDecoratorManager != null) preStorageDecoratorManager.closeDecorators();
            if (preExportDecoratorManager != null) preExportDecoratorManager.closeDecorators();
            try { if (parser != null) parser.close(); } catch (Exception ignored) {}
            try { if (lineProcessor != null) lineProcessor.close(); } catch (Exception ignored) {}
            try { if (fileExporter != null) fileExporter.close(); } catch (Exception ignored) {}
            try { dbManager.close(); } catch (Exception ignored) {}
        }
    }

    
    private JobContext createJobContext(File file) throws MegatronException {
        long startedTimestamp = System.currentTimeMillis();
        String jobName = getJobName(startedTimestamp);
        String fileHash = null;
        log.debug("Calculating hash for file: " + file.getAbsolutePath());
        try {
            fileHash = FileUtil.hashFile(file);
        } catch (IOException e) {
            String msg = "Cannot calculate hash for file: " + file.getAbsolutePath();
            throw new MegatronException(msg, e);
        }
        
        Job job = new Job(0L, jobName, file.getName(), fileHash, file.length(), SqlUtil.convertTimestampToSec(startedTimestamp));
        job.setJobType(getJobType());
        JobContext result = new JobContext(props, job); 
        result.setStartedTimestamp(startedTimestamp);
        result.setDbManager(dbManager);
        result.setNoOfLines(countNoOfLines(file));
        
        return result;
    }

    
    private long countNoOfLines(File file) throws MegatronException {
        log.debug("Calculating no. of lines in file: " + file.getAbsolutePath());
        try {
            String charSet = props.getString(AppProperties.INPUT_CHAR_SET_KEY, Constants.UTF8);
            return FileUtil.countLines(file, charSet);
        } catch (IOException e) {
            String msg = "Cannot count no. of lines in file: " + file.getAbsolutePath();
            throw new MegatronException(msg, e);
        }
    }

    
    private String getJobName(long timestamp) {
        String jobTypeName = props.getString(AppProperties.JOB_TYPE_NAME_KEY, null);
        String dateStr = DateUtil.formatDateTime("yyyy-MM-dd_HHmmss", new Date(timestamp));
        return jobTypeName + "_" + dateStr;
    }

    
    private JobType getJobType() throws MegatronException {
        String jobTypeName = props.getString(AppProperties.JOB_TYPE_NAME_KEY, null);
        JobType result = dbManager.searchJobType(jobTypeName);
        if (result == null) {
            log.info("Cannot find job type name in db: " + jobTypeName + ". Using default job type instead.");
            result = dbManager.searchJobType(Constants.DEFAULT_JOB_TYPE);
            if (result == null) {
                String msg = "Cannot find default job type (should always exist): " + Constants.DEFAULT_JOB_TYPE;
                throw new MegatronException(msg);
            }
        }
        return result;
    }

    
    private void init() throws MegatronException {
        String className = null;
        
        // read props
        removeTrailingSpaces = props.getBoolean(AppProperties.PARSER_REMOVE_TRAILING_SPACES_KEY, false);
        printProgressInterval = 1000L*props.getLong(AppProperties.PRINT_PROGRESS_INTERVAL_KEY, 15L);
        
        // init parser
        className = props.getString(AppProperties.PARSER_CLASS_NAME_KEY, RegExpParser.class.getName());
        try {
            log.debug("Using parser: " + className);
            Class<?> clazz = Class.forName(className);
            parser = (IParser)clazz.newInstance();
            parser.init(jobContext);
        } catch (Exception e) {
            // ClassNotFoundException, InstantiationException, IllegalAccessException, MegatronException
            String msg = "Cannot instantiate parser class: " + className;
            throw new MegatronException(msg, e);
        }

        // init fileProcessors
        fileProcessors = createFileProcessors();

        // init lineProcessor
        className = props.getString(AppProperties.LINE_PROCESSOR_CLASS_NAME_KEY, null);
        if (className != null) {
            try {
                log.debug("Using line processor: " + className);
                Class<?> clazz = Class.forName(className);
                lineProcessor = (ILineProcessor)clazz.newInstance();
                lineProcessor.init(jobContext);
            } catch (Exception e) {
                // ClassNotFoundException, InstantiationException, IllegalAccessException
                String msg = "Cannot instantiate line processor class: " + className;
                throw new MegatronException(msg, e);
            }
        }

        // init filters
        preLineProcessorFilters = createLineFilters(AppProperties.FILTER_PRE_LINE_PROCESSOR_KEY);
        preParserFilters = createLineFilters(AppProperties.FILTER_PRE_PARSER_KEY);
        preDecoratorFilterManager = new LogEntryFilterManager(jobContext);
        preDecoratorFilterManager.init(AppProperties.FILTER_PRE_DECORATOR_KEY);
        preStorageFilterManager = new LogEntryFilterManager(jobContext);
        preStorageFilterManager.init(AppProperties.FILTER_PRE_STORAGE_KEY);
        preExportFilterManager = new LogEntryFilterManager(jobContext);
        preExportFilterManager.init(AppProperties.FILTER_PRE_EXPORT_KEY);
        
        // decorators
        preStorageDecoratorManager = new DecoratorManager(jobContext);
        boolean useOrganizationMatcher = props.getBoolean(AppProperties.DECORATOR_USE_ORGANIZATION_MATCHER_KEY, true);
        preStorageDecoratorManager.init(AppProperties.DECORATOR_CLASS_NAMES_KEY, useOrganizationMatcher);
        preExportDecoratorManager = new DecoratorManager(jobContext);
        useOrganizationMatcher = false;
        preExportDecoratorManager.init(AppProperties.DECORATOR_PRE_EXPORT_CLASS_NAMES_KEY, useOrganizationMatcher);
        // FileExporter
        if (props.isExport()) {
            fileExporter = new FileExporter(jobContext);
            fileExporter.writeHeader(job);
        }
    }
    
    
    private List<IFileProcessor> createFileProcessors() throws MegatronException {
        List<IFileProcessor> result = new ArrayList<IFileProcessor>();
        String[] classNames = props.getStringList(AppProperties.FILE_PROCESSOR_CLASS_NAMES_KEY, new String[0]);
        if (classNames.length == 0) {
            // Use "fileProcessor.className" to be backward compatible.
            String className = props.getString(AppProperties.FILE_PROCESSOR_CLASS_NAME_KEY, null);
            if (className != null) {
                classNames = new String[1];
                classNames[0] = className;
            }
        }
        if (classNames.length > 0) {
            log.debug("Using file processors: " + Arrays.asList(classNames));
        }
        for (int i = 0; i < classNames.length; i++) {
            String className =  classNames[i];
            if (className.trim().length() == 0) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(className);
                IFileProcessor fileProcessor = (IFileProcessor)clazz.newInstance();
                fileProcessor.init(jobContext);
                result.add(fileProcessor);
            } catch (MegatronException e) {
                String msg = "Cannot initialize file processor class: " + className;
                throw new MegatronException(msg, e);
            } catch (Exception e) {
                // ClassNotFoundException, InstantiationException, IllegalAccessException
                String msg = "Cannot instantiate file processor class: " + className;
                throw new MegatronException(msg, e);
            }
        }
        return result;
    }

    
    private List<ILineFilter> createLineFilters(String propKey) throws MegatronException {
        List<ILineFilter> result = new ArrayList<ILineFilter>();
        String[] classNames = props.getStringList(propKey, new String[0]);
        if (classNames.length > 0) {
            log.debug("Using line filters: " + Arrays.asList(classNames));
        }
        for (int i = 0; i < classNames.length; i++) {
            String className =  classNames[i];
            if (className.trim().length() == 0) {
                continue;
            }
            try {
                Class<?> clazz = Class.forName(className);
                ILineFilter filter = (ILineFilter)clazz.newInstance();
                filter.init(jobContext);
                result.add(filter);
            } catch (MegatronException e) {
                String msg = "Cannot initialize filter class: " + className;
                throw new MegatronException(msg, e);
            } catch (Exception e) {
                // ClassNotFoundException, InstantiationException, IllegalAccessException
                String msg = "Cannot instantiate filter class: " + className;
                throw new MegatronException(msg, e);
            }
        }
        return result;
    }

    
    private void processLine(String lineInFile) throws MegatronException {
        jobContext.incLineNo(1);
        printProgress();
        
        if (removeTrailingSpaces) {
            lineInFile = StringUtil.removeTrailingSpaces(lineInFile);
        }
        
        if (!executeLineFilters(preLineProcessorFilters, lineInFile)) {
            jobContext.incNoOfFilteredLines(1);
            return;
        }
        
        List<String> lines = null;
        if (lineProcessor != null) {
            lines = lineProcessor.execute(lineInFile);
            if (lines == null) {
                return;
            }
        } else {
            lines = Collections.singletonList(lineInFile);
        }

        for (Iterator<String> iterator = lines.iterator(); iterator.hasNext(); ) {
            jobContext.incLineNoAfterProcessor(1);
            String line = iterator.next();

            if (!executeLineFilters(preParserFilters, line)) {
                jobContext.incNoOfFilteredLines(1);
                continue;
            }
            
            // skip empty lines
            if (line.trim().length() == 0) {
                jobContext.incNoOfFilteredLines(1);
                continue;
            }
            
            LogEntry logEntry = null;
            try {
                logEntry = parser.parse(line);
                assignLogEntry(logEntry, job, line);
            } catch (MegatronException e) {
                jobContext.incNoOfParseExceptions(1);
                double parseErrorThreshold = props.getDouble(AppProperties.PARSER_PARSE_ERROR_THRESHOLD_KEY, 0.20d);
                long noOfParseErrors = jobContext.getNoOfParseExceptions(); 
                double errorRatio = noOfParseErrors / (double)jobContext.getNoOfLines();
                long maxNoOfParseErrors = props.getLong(AppProperties.PARSER_MAX_NO_OF_PARSE_ERRORS, 5);
                if ((parseErrorThreshold >= 0) && (errorRatio >= parseErrorThreshold) && (maxNoOfParseErrors >= 0) && (noOfParseErrors >= maxNoOfParseErrors)) {
                    String msg = "Number of parse errors have exceeded threshold: " + jobContext.getNoOfParseExceptions() + " errors.";
                    throw new MegatronException(msg);
                }
                String msg = "Cannot parse line at " + jobContext.getLineNo() + ": " + line;
                if (jobContext.getNoOfParseExceptions() <= MAX_NO_OF_PARSE_ERROR_TO_CONSOLE) {
                    log.warn(msg);
                    jobContext.writeToConsole(msg);
                }
                log.debug(msg, e);
                continue;
            }
    
            if (!preDecoratorFilterManager.executeFilters(logEntry)) {
                jobContext.incNoOfFilteredLines(1);
                continue;
            }
            
            preStorageDecoratorManager.executeDecorators(logEntry);
    
            if (!preStorageFilterManager.executeFilters(logEntry)) {
                jobContext.incNoOfFilteredLines(1);
                continue;
            }
    
            if (!props.isNoDb()) {
                dbManager.addLogEntry(logEntry);
                jobContext.incNoOfSavedEntries(1);
            }
    
            preExportDecoratorManager.executeDecorators(logEntry);
            
            if (!preExportFilterManager.executeFilters(logEntry)) {
                jobContext.incNoOfFilteredLines(1);
                continue;
            }
    
            if (fileExporter != null) {
                fileExporter.writeLogEntry(logEntry);
                jobContext.incNoOfExportedEntries(1);
            }
        }
    }

    
    private void printProgress() {
        long now = System.currentTimeMillis();
        if ((printProgressInterval > 0L) && ((lastProgressPrintTime + printProgressInterval) < now)) {
            long lineNo = jobContext.getLineNo();
            long noOfLines = jobContext.getNoOfLines();
            double progress = 100d*((double)lineNo / (double)noOfLines); 
            double linesPerSecond = ((double)lineNo - (double)lastProgressPrintLineNo) / (((double)now - (double)lastProgressPrintTime) / 1000d);
            DecimalFormat format = new DecimalFormat("0.00");
            String progressStr = format.format(progress);
            String lineInfoStr = lineNo + " of " + noOfLines + " lines";
            String linesPerSecondStr = format.format(linesPerSecond);
            String msg = progressStr + "% (" + lineInfoStr + ", " + linesPerSecondStr + " lines/second)";
            if (props.isStdout()) {
                log.info(msg);
            } else {
                jobContext.writeToConsole(msg);
            }
            lastProgressPrintLineNo = lineNo;
            lastProgressPrintTime = now;
        }
    }

    
    private boolean executeLineFilters(List<ILineFilter> filters, String line) throws MegatronException {
        if (filters.isEmpty()) {
            return true;
        }
        
        boolean result = true;
        for (Iterator<ILineFilter> iterator = filters.iterator(); result && iterator.hasNext(); ) {
            result &= iterator.next().accept(line);
        }
        return result;
    }

    
    private void assignLogEntry(LogEntry logEntry, Job job, String line) {
        long now = SqlUtil.convertTimestampToSec(System.currentTimeMillis());

        // assign defaults 
        if ((logEntry.getCreated() == null) || (logEntry.getCreated() == 0)) {
            logEntry.setCreated(now);
        }
        if ((logEntry.getLogTimestamp() == null) || (logEntry.getLogTimestamp() == 0)) {
            logEntry.setLogTimestamp(SqlUtil.convertTimestampToSec(jobContext.getStartedTimestamp()));
        }
        
        logEntry.setJob(job);
        
        // assign OriginalLogEntry
        OriginalLogEntry originalLogEntry = new OriginalLogEntry();
        originalLogEntry.setCreated(now);
        originalLogEntry.setEntry(line);
        logEntry.setOriginalLogEntry(originalLogEntry);
        
        // assign oldestLogTimestamp
        if ((oldestLogTimestamp == null) || (oldestLogTimestamp > logEntry.getLogTimestamp())) {
            oldestLogTimestamp = logEntry.getLogTimestamp();
        }
    }
    
    
    private void closeFileProcessors(List<IFileProcessor> fileProcessors, boolean jobSuccessful) {
        if (fileProcessors == null) {
            return;
        }
        for (Iterator<IFileProcessor> iterator = fileProcessors.iterator(); iterator.hasNext(); ) {
            try {
                iterator.next().close(jobSuccessful);
            } catch (Exception e) {
                log.error("Cannot close file processor.", e);
            }
        }
    }

    
    private void closeLineFilters(List<ILineFilter> lineFilters) {
        if (lineFilters == null) {
            return;
        }
        for (Iterator<ILineFilter> iterator = lineFilters.iterator(); iterator.hasNext(); ) {
            try {
                iterator.next().close();
            } catch (Exception e) {
                log.error("Cannot close line filter.", e);
            }
        }
    }

    
    private boolean issueTimestampWarning() {
        long maxAge = props.getLong(AppProperties.TIMESTAMP_WARNING_MAX_AGE_KEY, 7*24*60*60L);
        return (maxAge > 0L) && (oldestLogTimestamp != null) && ((System.currentTimeMillis()/1000L - maxAge) > oldestLogTimestamp);
    }

    
    private void writeFinishedMessage() throws MegatronException {
        // -- write finished message to log and console
        String template = "Job finished. Name: @jobName@,  Lines: @noOfLines@, @linesAfterProcessorStr@Saved Entries: @noOfSavedEntries@, High Priority Entries: " +
            "@noOfHighPriorityEntries@, Exported Entries: @noOfExportedEntries@, Filtered Lines: @noOfFilteredLines@, Parse errors: @noOfParseException@, Duration: @duration@";
        String linesAfterProcessorTemplate = "Lines (after @action@): @lineNoAfterProcessor@, ";
        String msg = createFinishedMessage(template, linesAfterProcessorTemplate);
        if (!props.isStdout() || (jobContext.getNoOfParseExceptions() > 0)) {
            jobContext.writeToConsole(msg);
        }
        log.info(msg);
        // -- issue warning that file contains old timestamps?
        if (issueTimestampWarning()) {
            Date date = SqlUtil.convertTimestamp(oldestLogTimestamp);
            String dateStr = DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, date);
            msg = "Old timestamps exists in input file. Wrong file? Oldest timestamp: " + dateStr;
            jobContext.writeToConsole("WARNING! " + msg);
            log.warn(msg);
        }

        // -- write finished message to RSS
        boolean isJobRssEnabled = props.getBoolean(AppProperties.RSS_JOB_ENABLED_KEY, true);
        if (isJobRssEnabled && !props.isNoDb()) {
            template = "Lines: @noOfLines@<br>@linesAfterProcessorStr@Saved Entries: @noOfSavedEntries@<br>High Priority Entries: @noOfHighPriorityEntries@<br>" +
                "Exported Entries: @noOfExportedEntries@<br>Filtered Lines: @noOfFilteredLines@<br>Parse errors: @noOfParseException@<br>" +
                "Filename: @jobFilename@<br>Duration: @duration@";
            linesAfterProcessorTemplate = "Lines (after @action@): @lineNoAfterProcessor@<br>";
            String titleTemplate = "Log Job: @jobName@. High Priority Entries: @noOfHighPriorityEntries@";
            String title = createFinishedMessage(titleTemplate, linesAfterProcessorTemplate);
            String description = createFinishedMessage(template, linesAfterProcessorTemplate);
            JobRssFile rssFile = new JobRssFile(props);
            rssFile.addItem(title, description);
        } else if (!isJobRssEnabled && !props.isNoDb()) {
            log.info("Writing to job RSS feed disabled due to property: " + AppProperties.RSS_JOB_ENABLED_KEY);
        }
    }

    
    private String createFinishedMessage(String template, String linesAfterProcessorTemplate) {
        String result = template;
        String jobName = (job != null) ? job.getName() : "[job name is null]";
        result = StringUtil.replace(result, "@jobName@", jobName);
        String jobFilename = (job != null) ? job.getFilename() : "[job filename is null]";
        result = StringUtil.replace(result, "@jobFilename@", jobFilename);
        result = StringUtil.replace(result, "@noOfLines@", "" + jobContext.getNoOfLines());
        result = StringUtil.replace(result, "@noOfSavedEntries@", "" + jobContext.getNoOfSavedEntries());
        result = StringUtil.replace(result, "@noOfHighPriorityEntries@", "" + jobContext.getNoOfHighPriorityEntries());
        result = StringUtil.replace(result, "@noOfExportedEntries@", "" + jobContext.getNoOfExportedEntries());
        result = StringUtil.replace(result, "@noOfFilteredLines@", "" + jobContext.getNoOfFilteredLines());
        result = StringUtil.replace(result, "@noOfParseException@", "" + jobContext.getNoOfParseExceptions());
        String durationStr = DateUtil.formatDuration(System.currentTimeMillis() - jobContext.getStartedTimestamp());
        result = StringUtil.replace(result, "@duration@", durationStr);
        String linesAfterProcessorStr = "";
        if (lineProcessor != null) {
            linesAfterProcessorStr = linesAfterProcessorTemplate;
            String action = (jobContext.getLineNo() > jobContext.getLineNoAfterProcessor()) ? "merge" : "split";
            linesAfterProcessorStr = StringUtil.replace(linesAfterProcessorStr, "@action@", action);
            linesAfterProcessorStr = StringUtil.replace(linesAfterProcessorStr, "@lineNoAfterProcessor@", "" + jobContext.getLineNoAfterProcessor());
        }
        result = StringUtil.replace(result, "@linesAfterProcessorStr@", linesAfterProcessorStr);
    
        return result;
    }
    
    
    private void sendNotificationEmail(List<Organization> organizations) throws MegatronException {
        String toAddresses = props.getString(AppProperties.MAIL_NOTIFICATION_TO_ADDRESSES_KEY, "");
        if (toAddresses.trim().length() == 0) {
            String msg = "Sending high priority notification email skipped; no to-addresses defined in config.";
            log.info(msg);
            return;
        }
        log.info("Sending high priority notification email.");
        
        final String subjectTemplate = "Megatron job finished: $jobName, Prioritized entries: $noOfHighPriorityEntries (highest: $maxPriority), File: $jobFilename";
        final String bodyTemplate = 
            "Job Name: $jobName" + Constants.LINE_BREAK +
            "Filename: $jobFilename ($jobFileSize bytes, $jobProcessedLines lines)" + Constants.LINE_BREAK +
            "Highest Priority: $maxPriority ($maxPriorityName)" + Constants.LINE_BREAK +
            "Priority Threshold: $priority" + Constants.LINE_BREAK + 
            "No. of High Priority Organizations: $noOfHighPriorityOrganizations" + Constants.LINE_BREAK +
            "No. of Quarantined Log Entries: $noOfQuarantinedEntries of $noOfHighPriorityEntries" + Constants.LINE_BREAK +
            "No. of Unique Quarantined IPs: $noOfQuarantinedIps of $noOfHighPriorityIps" + Constants.LINE_BREAK + Constants.LINE_BREAK +
            "High priority organizations in job:" + Constants.LINE_BREAK + Constants.LINE_BREAK +
            "$rows" + Constants.LINE_BREAK +
            "$moreLogEntriesExistsMsg";

        String timestampWarning = null;
        if (issueTimestampWarning()) {
            Date date = SqlUtil.convertTimestamp(oldestLogTimestamp);
            String dateStr = DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_SECONDS, date);
            timestampWarning = "WARNING! Old timestamps exists in input file. Wrong file? Oldest timestamp: " + dateStr + Constants.LINE_BREAK + Constants.LINE_BREAK;
        }

        JobInfoWriter writer = new JobInfoWriter(props, dbManager, job);
        writer.execute();
        String subject = writer.createContent(subjectTemplate);
        String body = writer.createContent(bodyTemplate);
        if (timestampWarning != null) {
            body = timestampWarning + body;
        }
            
        MailSender mailSender = new MailSender();
        mailSender.setToAddresses(toAddresses);
        mailSender.setSubject(subject);
        mailSender.setBody(body);
        mailSender.send(props);
    }

}
