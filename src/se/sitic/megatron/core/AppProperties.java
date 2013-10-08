package se.sitic.megatron.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import se.sitic.megatron.entity.NameValuePair;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.FileUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Handles application properties and parses CLI arguments.
 */
public class AppProperties {
    // -- Keys in properties file --

    // Implicit
    public static final String JOB_TYPE_NAME_KEY = "implicit.jobTypeName";

    // General
    public static final String LOG4J_FILE_KEY = "general.log4jConfigFile";
    public static final String LOG_DIR_KEY = "general.logDir";
    public static final String JOB_TYPE_CONFIG_DIR_KEY = "general.jobTypeConfigDir";
    public static final String SLURP_DIR_KEY = "general.slurpDir";
    public static final String OUTPUT_DIR_KEY = "general.outputDir";
    public static final String TMP_DIR_KEY = "general.tmpDir";
    public static final String INPUT_CHAR_SET_KEY = "general.inputCharSet";
    // Deprecated: geoIp.databaseFile (use geoIp.countryDatabaseFile instead)
    public static final String GEO_IP_DATABASE_FILE_KEY = "geoIp.databaseFile";
    public static final String GEO_IP_COUNTRY_DATABASE_FILE_KEY = "geoIp.countryDatabaseFile";
    public static final String GEO_IP_ASN_DATABASE_FILE_KEY = "geoIp.asnDatabaseFile";
    public static final String GEO_IP_CITY_DATABASE_FILE_KEY = "geoIp.cityDatabaseFile";
    public static final String GEO_IP_USE_CITY_DATABASE_FOR_COUNTRY_LOOKUPS_KEY = "geoIp.useCityDatabaseForCountryLookups";
    public static final String FILENAME_MAPPER_LIST_KEY = "general.filenameMapperList";
    public static final String USE_DNS_JAVA_KEY = "general.useDnsJava";
    public static final String WHOIS_SERVER_KEY = "general.whoisServer";
    public static final String HIGH_PRIORITY_THRESHOLD_KEY = "general.highPriorityNotification.threshold";
    public static final String TIMESTAMP_WARNING_MAX_AGE_KEY = "general.timestampWarning.maxAge";
    public static final String PRINT_PROGRESS_INTERVAL_KEY = "general.printProgressInterval";
    public static final String FILE_ALREADY_PROCESSED_ACTION_KEY = "general.fileAlreadyProcessedAction";
    
    // dnsjava
    public static final String DNS_JAVA_USE_DNS_JAVA_KEY = "dnsJava.useDnsJava";
    public static final String DNS_JAVA_USE_SIMPLE_RESOLVER_KEY = "dnsJava.useSimpleResolver";
    public static final String DNS_JAVA_DNS_SERVERS_KEY = "dnsJava.dnsServers";
    public static final String DNS_JAVA_TIME_OUT_KEY = "dnsJava.timeOut";
    
    // Database
    public static final String DB_USERNAME_KEY = "db.username";
    public static final String DB_PASSWORD_KEY = "db.password";
    public static final String DB_SERVER_KEY = "db.server";
    public static final String DB_PORT_KEY = "db.port";
    public static final String DB_NAME_KEY = "db.name";
    public static final String JDBC_URL_KEY = "db.jdbc.url";
    public static final String JDBC_DRIVER_CLASS_KEY = "db.jdbc.driverClassName";

    // BGP
    public static final String BGP_IMPORT_FILE_KEY = "bgp.importFile";
    public static final String BGP_HARD_CODED_PREFIXES_KEY = "bgp.hardCodedPrefixes";
    
    // Export
    public static final String EXPORT_TEMPLATE_DIR_KEY = "export.templateDir";
    public static final String EXPORT_HEADER_FILE_KEY = "export.headerFile";
    public static final String EXPORT_ROW_FILE_KEY = "export.rowFile";
    public static final String EXPORT_FOOTER_FILE_KEY = "export.footerFile";
    public static final String EXPORT_CHAR_SET_KEY = "export.charSet";
    public static final String EXPORT_TIMESTAMP_FORMAT_KEY = "export.timestampFormat";
    public static final String EXPORT_REWRITERS_KEY = "export.rewriters";

    // Mail
    public static final String MAIL_SMTP_HOST_KEY = "mail.smtpHost";
    public static final String MAIL_DEBUG_KEY = "mail.debug";
    public static final String MAIL_FROM_ADDRESS_KEY = "mail.fromAddress";
    public static final String MAIL_TO_ADDRESSES_KEY = "mail.toAddresses";
    public static final String MAIL_BCC_ADDRESSES_KEY = "mail.bccAddresses";
    public static final String MAIL_ARCHIVE_BCC_ADDRESSES_KEY = "mail.archiveBccAddresses";
    public static final String MAIL_SUMMARY_TO_ADDRESSES_KEY = "mail.mailJobSummary.toAddresses";
    public static final String MAIL_NOTIFICATION_TO_ADDRESSES_KEY = "mail.highPriorityNotification.toAddresses";
    public static final String MAIL_REPLY_TO_ADDRESSES_KEY = "mail.replyToAddresses";
    public static final String MAIL_HTML_MAIL_KEY = "mail.htmlMail";
    public static final String MAIL_IP_QUARANTINE_PERIOD_KEY = "mail.ipQuarantinePeriod";
    
    public static final String MAIL_SUBJECT_TEMPLATE_KEY = "mail.subjectTemplate";
    public static final String MAIL_JOB_SUMMARY_SUBJECT_TEMPLATE_KEY = "mail.mailJobSummary.subjectTemplate";
    public static final String MAIL_TEMPLATE_DIR_KEY = "mail.templateDir";
    public static final String MAIL_DEFAULT_LANGUAGE_CODE_KEY = "mail.defaultLanguageCode";
    public static final String MAIL_HEADER_FILE_KEY = "mail.headerFile";
    public static final String MAIL_ROW_FILE_KEY = "mail.rowFile";
    public static final String MAIL_FOOTER_FILE_KEY = "mail.footerFile";
    public static final String MAIL_TIMESTAMP_FORMAT_KEY = "mail.timestampFormat";
    public static final String MAIL_RAISE_ERROR_FOR_DEBUG_TEMPLATE_KEY = "mail.raiseErrorForDebugTemplate";
    
    // Mail Encryption
    public static final String MAIL_ENCRYPTION_TYPE_KEY = "mail.encryptionType";
    public static final String MAIL_ENCRYPTION_KEYPATH_KEY = "mail.encryptionKeyPath";
    public static final String MAIL_ENCRYPTION_SMIME_PW_KEY = "mail.enryptionSMIMEpw";
    public static final String MAIL_ENCRYPTION_PGP_PW_KEY = "mail.enryptionPGPpw";
    public static final String MAIL_ENCRYPTION_ENCRYPT_KEY = "mail.encryptMail";
    public static final String MAIL_ENCRYPTION_SIGN_KEY = "mail.signMail";

    // Filters
    public static final String FILTER_PRE_LINE_PROCESSOR_KEY = "filter.preLineProcessor.classNames";
    public static final String FILTER_PRE_PARSER_KEY = "filter.preParser.classNames";
    public static final String FILTER_PRE_DECORATOR_KEY = "filter.preDecorator.classNames";
    public static final String FILTER_PRE_STORAGE_KEY = "filter.preStorage.classNames";
    public static final String FILTER_PRE_EXPORT_KEY = "filter.preExport.classNames";
    public static final String FILTER_PRE_MAIL_KEY = "filter.preMail.classNames";
    public static final String FILTER_EXCLUDE_REG_EXP_KEY = "filter.regExpLineFilter.excludeRegExp";
    public static final String FILTER_INCLUDE_REG_EXP_KEY = "filter.regExpLineFilter.includeRegExp";
    public static final String FILTER_LINE_NUMBER_EXCLUDE_INTERVALS_KEY = "filter.lineNumberFilter.excludeIntervals";
    public static final String FILTER_LINE_NUMBER_INCLUDE_INTERVALS_KEY = "filter.lineNumberFilter.includeIntervals";
    public static final String FILTER_PRIORITY_INCLUDE_INTERVALS_KEY = "filter.priorityFilter.includeIntervals";
    public static final String FILTER_EXCLUDE_COUNTRY_CODES_KEY = "filter.countryCodeFilter.excludeCountryCodes";
    public static final String FILTER_INCLUDE_COUNTRY_CODES_KEY = "filter.countryCodeFilter.includeCountryCodes";
    public static final String FILTER_COUNTRY_CODE_ORGANIZATION_KEY = "filter.countryCodeFilter.organizationToFilter";
    public static final String FILTER_EXCLUDE_AS_NUMBERS_KEY = "filter.asnFilter.excludeAsNumbers";
    public static final String FILTER_INCLUDE_AS_NUMBERS_KEY = "filter.asnFilter.includeAsNumbers";
    public static final String FILTER_ASN_ORGANIZATION_KEY = "filter.asnFilter.organizationToFilter";
    public static final String FILTER_ATTRIBUTE_NAME_KEY = "filter.attributeFilter.attributeName";
    public static final String FILTER_ATTRIBUTE_EXCLUDE_REG_EXP_KEY = "filter.attributeFilter.excludeRegExp";
    public static final String FILTER_ATTRIBUTE_INCLUDE_REG_EXP_KEY = "filter.attributeFilter.includeRegExp";
    public static final String FILTER_OCCURRENCE_ATTRIBUTE_NAMES_KEY = "filter.occurrenceFilter.attributeNames"; 
    public static final String FILTER_OCCURRENCE_EXCLUDE_INTERVALS_KEY = "filter.occurrenceFilter.excludeIntervals";
    public static final String FILTER_OCCURRENCE_INCLUDE_INTERVALS_KEY = "filter.occurrenceFilter.includeIntervals";
    public static final String FILTER_OCCURRENCE_FILE_SORTED_KEY = "filter.occurrenceFilter.fileSorted";
    public static final String FILTER_MATCH_IP_ADDRESS_KEY = "filter.organizationFilter.matchIpAddress";
    public static final String FILTER_MATCH_HOSTNAME_KEY = "filter.organizationFilter.matchHostname";
    public static final String FILTER_MATCH_ASN_KEY = "filter.organizationFilter.matchAsn";
    
    // File Processor
    public static final String FILE_PROCESSOR_CLASS_NAME_KEY = "fileProcessor.className";
    public static final String FILE_PROCESSOR_CLASS_NAMES_KEY = "fileProcessor.classNames";
    public static final String FILE_PROCESSOR_DELETE_TMP_FILES_KEY = "fileProcessor.deleteTmpFiles";
    public static final String FILE_PROCESSOR_OS_COMMAND_KEY = "fileProcessor.osCommandProcessor.command";
    public static final String FILE_PROCESSOR_DIFF_COMMAND_KEY = "fileProcessor.diffProcessor.command";
    public static final String FILE_PROCESSOR_DIFF_OLD_FILES_DIR_KEY = "fileProcessor.diffProcessor.oldFilesDir";
    public static final String FILE_PROCESSOR_DIFF_NO_OF_BACKUPS_TO_KEEP_KEY = "fileProcessor.diffProcessor.noOfBackupsToKeep";
    public static final String FILE_PROCESSOR_XML_TO_ROW_START_ELEMENT_KEY = "fileProcessor.xmlToRowFileProcessor.startElement";
    public static final String FILE_PROCESSOR_XML_TO_ROW_ELEMENTS_TO_SAVE_KEY = "fileProcessor.xmlToRowFileProcessor.elementsToSave";
    public static final String FILE_PROCESSOR_XML_TO_ROW_OUTPUT_SEPARATOR_KEY = "fileProcessor.xmlToRowFileProcessor.outputSeparator";
    // Deprecated: fileProcessor.xmlToRowFileProcessor.deleteOutputFile (use fileProcessor.deleteTmpFiles instead)
    public static final String FILE_PROCESSOR_XML_TO_ROW_DELETE_OUTPUT_FILE_KEY = "fileProcessor.xmlToRowFileProcessor.deleteOutputFile";
    
    // Line Processor
    public static final String LINE_PROCESSOR_CLASS_NAME_KEY = "lineProcessor.className";
    public static final String LINE_MERGER_START_REG_EXP_KEY = "lineProcessor.merger.startRegExp";
    public static final String LINE_MERGER_END_REG_EXP_KEY = "lineProcessor.merger.endRegExp";
    public static final String LINE_MERGER_RESTART_IF_START_FOUND_KEY = "lineProcessor.merger.restartIfStartFound";
    public static final String LINE_MERGER_SEPARATOR_KEY = "lineProcessor.merger.separator";
    public static final String LINE_SPLITTER_SEPARATOR_REG_EXP_KEY = "lineProcessor.splitter.separatorRegExp";
    public static final String LINE_SPLITTER_ITEM_REG_EXP_KEY = "lineProcessor.splitter.itemRegExp";
    public static final String LINE_SPLITTER_APPEND_ORIGINAL_LOG_ROW_KEY = "lineProcessor.splitter.appendOriginalLogRow";
    
    // Decorators
    public static final String DECORATOR_CLASS_NAMES_KEY = "decorator.classNames";
    public static final String DECORATOR_PRE_EXPORT_CLASS_NAMES_KEY = "decorator.preExport.classNames";
    public static final String DECORATOR_PRE_MAIL_CLASS_NAMES_KEY = "decorator.preMail.classNames";
    public static final String DECORATOR_COMBINED_DECORATOR_CLASS_NAMES_KEY = "decorator.combinedDecorator.classNames";
    public static final String DECORATOR_USE_ORGANIZATION_MATCHER_KEY = "decorator.useOrganizationMatcher";
    public static final String DECORATOR_MATCH_IP_ADDRESS_KEY = "decorator.organizationMatcher.matchIpAddress";
    public static final String DECORATOR_MATCH_HOSTNAME_KEY = "decorator.organizationMatcher.matchHostname";
    public static final String DECORATOR_MATCH_ASN_KEY = "decorator.organizationMatcher.matchAsn";
    public static final String DECORATOR_COUNTRY_CODES_TO_ADD_KEY = "decorator.countryCodeFromHostnameDecorator.countryCodesToAdd";
    public static final String DECORATOR_USE_ASN_IN_LOG_ENTRY_KEY = "decorator.asnGeoIpDecorator.useAsnInLogEntry";
    public static final String DECORATOR_ADD_AS_NAME_KEY = "decorator.asnGeoIpDecorator.addAsName";
    public static final String DECORATOR_URL_TO_HOSTNAME_USE_PRIMARY_ORG_KEY = "decorator.urlToHostnameDecorator.usePrimaryOrg";
    public static final String DECORATOR_GEOLOCATION_FIELDS_TO_ADD_KEY = "decorator.geolocationDecorator.fieldsToAdd";
    
    // Parser
    public static final String PARSER_CLASS_NAME_KEY = "parser.className";
    public static final String PARSER_PARSE_ERROR_THRESHOLD_KEY = "parser.parseErrorThreshold";
    public static final String PARSER_MAX_NO_OF_PARSE_ERRORS = "parser.maxNoOfParseErrors";
    public static final String PARSER_TIME_STAMP_FORMAT_KEY = "parser.timestampFormat";
    public static final String PARSER_ADD_CURRENT_DATE_TO_TIMESTAMP_KEY = "parser.addCurrentDateToTimestamp";
    public static final String PARSER_DEFAULT_TIME_ZONE_KEY = "parser.defaultTimeZone";
    public static final String PARSER_CHECK_UNUSED_VARIABLES_KEY = "parser.checkUnusedVariables";
    public static final String PARSER_LINE_REG_EXP_KEY = "parser.lineRegExp";
    public static final String PARSER_ITEM_PREFIX = "parser.item.";
    public static final String PARSER_ADDITIONAL_ITEM_PREFIX = "parser.item.additionalItem.";
    public static final String PARSER_FREE_TEXT_KEY = "parser.item.freeText";
    public static final String PARSER_TRIM_VALUE_KEY = "parser.trimValue";
    public static final String PARSER_REMOVE_ENCLOSING_CHARS_FROM_VALUE_KEY = "parser.removeEnclosingCharsFromValue";
    public static final String PARSER_REWRITERS_KEY = "parser.rewriters";
    public static final String PARSER_REMOVE_TRAILING_SPACES_KEY = "parser.removeTrailingSpaces";
    public static final String PARSER_EXPAND_IP_RANGE_WITH_ZERO_OCTETS_KEY = "parser.expandIpRangeWithZeroOctets";
    
    // RSS
    public static final String RSS_FACTORY_CLASS_NAME_KEY = "rss.factoryClassName";
    public static final String RSS_FORMAT_KEY = "rss.format";
    // Job RSS
    public static final String RSS_JOB_ENABLED_KEY = "rss.job.enabled";
    public static final String RSS_JOB_FILE_KEY = "rss.job.file";
    public static final String RSS_JOB_CONTENT_TITLE_KEY = "rss.job.content.title";
    public static final String RSS_JOB_CONTENT_LINK_KEY = "rss.job.content.link";
    public static final String RSS_JOB_CONTENT_DESCRIPTION_KEY = "rss.job.content.description";
    public static final String RSS_JOB_CONTENT_AUTHOR_KEY = "rss.job.content.author";
    public static final String RSS_JOB_CONTENT_COPYRIGHT_KEY = "rss.job.content.copyright";
    public static final String RSS_JOB_MAX_NO_OF_ITEMS_KEY = "rss.job.maxNoOfItems";
    public static final String RSS_JOB_ITEM_EXPIRE_TIME_IN_MINUTES_KEY = "rss.job.itemExpireTimeInMinutes";
    // Stats RSS
    public static final String RSS_STATS_FORMAT_KEY = "rss.stats.format";
    public static final String RSS_STATS_FILE_KEY = "rss.stats.file";
    public static final String RSS_STATS_CONTENT_TITLE_KEY = "rss.stats.content.title";
    public static final String RSS_STATS_CONTENT_LINK_KEY = "rss.stats.content.link";
    public static final String RSS_STATS_CONTENT_DESCRIPTION_KEY = "rss.stats.content.description";
    public static final String RSS_STATS_CONTENT_AUTHOR_KEY = "rss.stats.content.author";
    public static final String RSS_STATS_CONTENT_COPYRIGHT_KEY = "rss.stats.content.copyright";
    public static final String RSS_STATS_MAX_NO_OF_ITEMS_KEY = "rss.stats.maxNoOfItems";
    public static final String RSS_STATS_ITEM_EXPIRE_TIME_IN_MINUTES_KEY = "rss.stats.itemExpireTimeInMinutes";

    // Report
    // Deprecated: flash.outputDir (use report.outputDir instead)
    public static final String FLASH_NO_OF_WEEKS_KEY = "flash.noOfWeeks";
    // Deprecated: flash.noOfWeeks (use report.statistics.noOfWeeks instead)
    public static final String FLASH_OUTPUT_DIR_KEY = "flash.outputDir";
    public static final String REPORT_CLASS_NAMES_KEY = "report.classNames";
    public static final String REPORT_OUTPUT_DIR_KEY = "report.outputDir";
    public static final String REPORT_TEMPLATE_DIR_KEY = "report.templateDir";
    public static final String REPORT_STATISTICS_NO_OF_WEEKS_KEY = "report.statistics.noOfWeeks";
    public static final String REPORT_GEOLOCATION_NO_OF_WEEKS_KEY = "report.geolocation.noOfWeeks";
    public static final String REPORT_GEOLOCATION_GENERATE_INTERNAL_REPORT_KEY = "report.geolocation.generateInternalReport";
    public static final String REPORT_GEOLOCATION_NO_OF_ENTRIES_IN_CITY_REPORT_KEY = "report.geolocation.noOfEntriesInCityReport";
    public static final String REPORT_GEOLOCATION_JOB_TYPE_KILL_LIST_KEY = "report.geolocation.jobTypeKillList";
    public static final String REPORT_GEOLOCATION_ORGANIZATION_TYPE_KILL_LIST_KEY = "report.geolocation.organizationTypeKillList";
    public static final String REPORT_GEOLOCATION_ORGANIZATION_TYPE_NAME_MAPPER_KEY = "report.geolocation.organizationTypeNameMapper";
    
    // Import
    public static final String CONTACTS_IMPORT_FILE_KEY = "import.dataFile";

    /** Filename for Megatron global properties. */
    public static final String GLOBALS_PROPS_FILE = "/etc/megatron/megatron-globals.properties";

    /** Key in system properties for global properties file. */
    public static final String MEGATRON_CONFIG_FILE_KEY = "megatron.configfile";

    // Cannot use logging becuse it's not yet initialized.
    // private static final Logger log = Logger.getLogger(GlobalProperties.class);

    private static final String TRUE = "true";

    private static AppProperties singleton;

    private Map<String, String> globalProps;
    private TypedProperties globalTypedProps;
    private List<File> jobTypeFiles;
    private List<String> jobTypeNames;
    private List<String> inputFiles;


    /**
     * Returns singleton.
     */
    public static synchronized AppProperties getInstance() {
        if (singleton == null) {
            singleton = new AppProperties();
        }
        return singleton;
    }


    /**
     * Constructor. Private due to singleton.
     */
    private AppProperties() {
        // empty
    }


    /**
     * Loads properties file and parses specified command line arguments.
     */
    public void init(String[] args) throws MegatronException, CommandLineParseException {
        // -- Hard-coded properties
        // Use US as default locale. Dates with names, e.g. 04/Jul/2009, may otherwise be incorrect.
        Locale.setDefault(Locale.US);
        
        // Use UTC as default time-zone. All time-stamps in db are in UTC-time.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // -- Read global properties and parse command line
        this.globalProps = loadGlobalProperties();
        parseCommandLine(args);
        this.globalTypedProps = new TypedProperties(globalProps, null);

        // -- init jobTypeFiles and jobTypeNames
        this.jobTypeFiles = listJobTypeFiles();
        this.jobTypeNames = new ArrayList<String>();
        for (Iterator<File> iterator = jobTypeFiles.iterator(); iterator.hasNext(); ) {
            String filename = iterator.next().getName();
            this.jobTypeNames.add(StringUtil.removeSuffix(filename, ".properties"));
        }
    }


    /**
     * Returns global properties.<p>
     * Note: This method should be used sparsely. Use TypedProperties-object created by
     * createTypedPropertiesForCli or createTypedPropertiesForWeb instead.
     */
    public TypedProperties getGlobalProperties() {
        return globalTypedProps;
    }

    
    /**
     * Maps specified filename (without path) to a job-type.
     * 
     * @return found job-type, or null if not found.
     */
    public String mapFilenameToJobType(String filename, boolean ignoreCliArgument) {
        String result = null;
        Logger log = Logger.getLogger(getClass());

        if (!ignoreCliArgument) { 
            result = globalTypedProps.getString(TypedProperties.CLI_JOB_TYPE_KEY, null);
            if (result != null) {
                log.debug("Using job-type specified in CLI-argument: " + result);
                return result;
            }
        }
    
        List<NameValuePair> nvList = globalTypedProps.getNameValuePairList(FILENAME_MAPPER_LIST_KEY, null);
        if (nvList != null) {
            for (Iterator<NameValuePair> iterator = nvList.iterator(); (result == null) && iterator.hasNext(); ) {
                NameValuePair nv = iterator.next();
                try {
                    if (filename.matches(nv.getName())) {
                        log.debug("Job-type found: " + nv.getValue() + ". Filename '" + filename + "' matches reg-exp '" + nv.getName() + "'.");
                        result = nv.getValue();
                    }
                } catch(PatternSyntaxException e) {
                    log.error("Invalid reg-exp in filename mapper: " + nv.getName());
                }
            }
        } else {
            log.error("Filename mapper not defined in config. Property name: " + FILENAME_MAPPER_LIST_KEY);
        }
        
        return result;
    }

    
    /**
     * Creates properties for specified job-type, which includes CLI-arguments and global properties.
     * Use this method in the CLI application.
     */
    public TypedProperties createTypedPropertiesForCli(String jobType) throws MegatronException {
        try {
            Map<String, String> jobTypeProps = loadJobTypeProperties(jobType);
            return new TypedProperties(jobTypeProps, Collections.singletonList(globalProps));
        } catch (MegatronException e) {
            throw new MegatronException("Invalid job-type name: " + jobType, e);
        }        
    }

    
    /**
     * Creates properties for specified job-type, which includes specifed CLI-arguments and global properties.
     * CLI-properties are used in the web application, but assigned for each run.
     * Use this method in the web application.
     * 
     * @param jobType job-type name, e.g. "shadowserver-ddos".
     * @param cliArgs CLI-arguments. Key: CLI-consts, e.g. CLI_NO_DB. Value: argument value, e.g. "true". 
     */
    public TypedProperties createTypedPropertiesForWeb(String jobType, Map<String, String> cliArgs) throws MegatronException {
        try {
            Map<String, String> jobTypeProps = loadJobTypeProperties(jobType);
            for (Iterator<String> iterator = cliArgs.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                String value = cliArgs.get(key);
                jobTypeProps.put(key, value);
            }
            return new TypedProperties(jobTypeProps, Collections.singletonList(globalProps));
        } catch (MegatronException e) {
            throw new MegatronException("Invalid job-type name: " + jobType, e);
        }        
    }


    /**
     * Returns input filenames specified at the command line.
     */
    public List<String> getInputFiles() {
        return this.inputFiles;
    }

    
    public List<File> getJobTypeFiles() {
        return this.jobTypeFiles;
    }


    public List<String> getJobTypeNames() {
        return this.jobTypeNames;
    }


    public String getJobTypeConfigDir() {
        return globalTypedProps.getString(JOB_TYPE_CONFIG_DIR_KEY, null);
    }
    
    
    public String getJdbcUrl() {
        // Format: jdbc:mysql://{db.server}:{db.port}/{db.name}
        String result = globalTypedProps.getString(JDBC_URL_KEY, "jdbc:mysql://{db.server}:{db.port}/{db.name}");
        String dbServer = globalTypedProps.getString(DB_SERVER_KEY, "127.0.0.1");
        String dbPort = globalTypedProps.getString(DB_PORT_KEY, "3306");
        String dbName = globalTypedProps.getString(DB_NAME_KEY, "ossec");

        result = StringUtil.replace(result, "{db.server}", dbServer);
        result = StringUtil.replace(result, "{db.port}", dbPort);
        result = StringUtil.replace(result, "{db.name}", dbName);

        return result;
    }

    
    /**
     * Loads global properties.
     *
     * @throws MegatronException if global properties cannot be read.
     */
    private Map<String, String> loadGlobalProperties() throws MegatronException {
        String filename = System.getProperty(MEGATRON_CONFIG_FILE_KEY);
        filename = (filename != null) ? filename : GLOBALS_PROPS_FILE;
        return loadPropertiesFile(filename);
    }


    /**
     * Loads job-type properties.
     *
     * @throws IOException if properties cannot be read.
     */
    private Map<String, String> loadJobTypeProperties(String jobTypeName) throws MegatronException {
        String filename = FileUtil.concatPath(getJobTypeConfigDir(), jobTypeName + ".properties");
        Map<String, String> result = loadPropertiesFile(filename); 
        result.put(JOB_TYPE_NAME_KEY, jobTypeName);
        return result;
    }


    /**
     * Loads specified properties file.
     *
     * @throws MegatronException if global properties cannot be read.
     */
    private Map<String, String> loadPropertiesFile(String filename) throws MegatronException {
        // The class Properties cannot be used:
        //   1. Back-slashes in regular expressions are removed.
        //   2. Order is not preserved.

        // LinkedHashMap preserves insertion order.
        Map<String, String> result = new LinkedHashMap<String, String>();

        final String nameValueSeparator = "=";

        File file = new File(filename);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

            int lineNo = 0;
            String line = null;
            while ((line = in.readLine()) != null) {
                ++lineNo;
                String trimmedLine = line.trim();
                if ((trimmedLine.length() == 0) || trimmedLine.startsWith(Constants.CONFIG_COMMENT_PREFIX)) {
                    continue;
                }

                // split line into name-value
                int index = line.indexOf(nameValueSeparator);
                if ((index == -1) || (index == 0)) {
                    String msg = "Parse error at line " + lineNo + ": Separator not found.";
                    throw new IOException(msg);
                }
                String name = line.substring(0, index);
                String value = ((index + 1) < line.length()) ? line.substring(index + 1, line.length()) : "";
                result.put(name, value);
            }
        } catch (IOException e) {
            String msg = "Cannot read properties file: " + file.getAbsolutePath();
            System.err.println(msg);
            throw new MegatronException(msg, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    String msg = "Cannot close properties file: " + file.getAbsolutePath();
                    System.err.println(msg);
                    throw new MegatronException(msg, e);
                }
            }
        }

        return result;
    }


    /**
     * Parsers specified command line arguments and adds them to properties map.
     */
    private void parseCommandLine(String[] args) throws MegatronException, CommandLineParseException {
        if (args == null) {
            return;
        }

        inputFiles = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();

            if (arg.startsWith("-")) {
                if (arg.equals("--version") || arg.equals("-v")) {
                    throw new CommandLineParseException(CommandLineParseException.SHOW_VERSION_ACTION);
                } else if (arg.equals("--help") || arg.equals("-h")) {
                    throw new CommandLineParseException(CommandLineParseException.SHOW_USAGE_ACTION);
                } else if (arg.equals("--slurp") || arg.equals("-s")) {
                    globalProps.put(TypedProperties.CLI_SLURP_KEY, TRUE);
                } else if (arg.equals("--export") || arg.equals("-e")) {
                    globalProps.put(TypedProperties.CLI_EXPORT_KEY, TRUE);
                } else if (arg.equals("--delete") || arg.equals("-d") || arg.equals("--delete-all") || arg.equals("-D")) {
                    // job specified after delete switch? 
                    int nextIndex = i + 1;
                    if ((nextIndex < args.length) && !args[nextIndex].startsWith("-")) {
                        i = nextIndex;
                        globalProps.put(TypedProperties.CLI_JOB_KEY, args[i]);
                    } else {
                        // otherwise --job must be present
                        List<String> argList = new ArrayList<String>(Arrays.asList(args));
                        if (!(argList.contains("--job") || argList.contains("--j"))) {
                            throw new CommandLineParseException("Job name not specified.");
                        }
                    }
                    
                    if (arg.equals("--delete") || arg.equals("-d")) {
                        globalProps.put(TypedProperties.CLI_DELETE_KEY, TRUE);
                    } else {
                        globalProps.put(TypedProperties.CLI_DELETE_ALL_KEY, TRUE);    
                    }
                } else if (arg.equals("--job") || arg.equals("-j")) {
                    ++i;
                    if (i == args.length) {
                        throw new CommandLineParseException("Missing argument to " + arg);
                    }
                    globalProps.put(TypedProperties.CLI_JOB_KEY, args[i]);
                } else if (arg.equals("--job-type") || arg.equals("-t")) {
                    ++i;
                    if (i == args.length) {
                        throw new CommandLineParseException("Missing argument to " + arg);
                    }
                    globalProps.put(TypedProperties.CLI_JOB_TYPE_KEY, args[i]);
                } else if (arg.equals("--output-dir") || arg.equals("-o")) {
                    ++i;
                    if (i == args.length) {
                        throw new CommandLineParseException("Missing argument to " + arg);
                    }
                    String outputDirStr = args[i];
                    File outputDir = new File(outputDirStr);
                    if (!outputDir.isDirectory()) {
                        throw new MegatronException("Output directory not found: " + outputDir.getAbsolutePath());
                    }
                    globalProps.put(TypedProperties.CLI_OUTPUT_DIR_KEY, outputDirStr);
                } else if (arg.equals("--id") || arg.equals("-i")) {
                    ++i;
                    if (i == args.length) {
                        throw new CommandLineParseException("Missing argument to " + arg);
                    }
                    String idStr = args[i];
                    try {
                        long id = Long.parseLong(idStr);
                        if (id <= 0L) {
                            throw new NumberFormatException("Id is zero or negative.");    
                        }
                    } catch (NumberFormatException e) {
                        throw new CommandLineParseException("Invalid RTIR id (--id): " + idStr, e);
                    }
                    globalProps.put(TypedProperties.CLI_ID_KEY, idStr);
                } else if (arg.equals("--prio") || arg.equals("-p")) {
                    ++i;
                    if (i == args.length) {
                        throw new CommandLineParseException("Missing argument to " + arg);
                    }
                    String prioStr = args[i];
// Skip check. OK with a list of intervals, e.g. --prio 55-60,70-75
//                    try {
//                        long prio = Long.parseLong(prioStr);
//                        if ((prio < 0L) || (prio > 100L)) {
//                            throw new NumberFormatException("Prio is not in range.");    
//                        }
//                    } catch (NumberFormatException e) {
//                        String msg = "Invalid prio (--prio): " + prioStr + ". Use --list-prios to list priorities.";
//                        throw new CommandLineParseException(msg, e);
//                    }
                    globalProps.put(TypedProperties.CLI_PRIO_KEY, prioStr);
                } else if (arg.equals("--list-prios") || arg.equals("-P")) {
                    globalProps.put(TypedProperties.CLI_LIST_PRIOS_KEY, TRUE);
                } else if (arg.equals("--list-jobs") || arg.equals("-l")) {
                    globalProps.put(TypedProperties.CLI_LIST_JOBS_KEY, TRUE);
                    // days specified after switch?
                    int nextIndex = i + 1;
                    if ((nextIndex < args.length) && !args[nextIndex].startsWith("-")) {
                        i = nextIndex;
                        String val = args[i];
                        try {
                            int days = Integer.parseInt(val);
                            if ((days <= 0) || (days > 1000)) {
                                throw new NumberFormatException("No. of days must be in the range 1-1000.");    
                            }
                        } catch (NumberFormatException e) {
                            throw new CommandLineParseException("Invalid no. of days (--list-jobs): " + val, e);
                        }
                        globalProps.put(TypedProperties.CLI_DAYS_IN_LIST_JOBS_KEY, val);
                    }
                }  else if (arg.equals("--job-info") || arg.equals("-I")) {
                    // job specified after switch? 
                    int nextIndex = i + 1;
                    if ((nextIndex < args.length) && !args[nextIndex].startsWith("-")) {
                        i = nextIndex;
                        globalProps.put(TypedProperties.CLI_JOB_KEY, args[i]);
                    } else {
                        // otherwise --job must be present
                        List<String> argList = new ArrayList<String>(Arrays.asList(args));
                        if (!(argList.contains("--job") || argList.contains("--j"))) {
                            throw new CommandLineParseException("Job name not specified.");
                        }
                    }
                    globalProps.put(TypedProperties.CLI_JOB_INFO_KEY, TRUE);
                } else if (arg.equals("--no-db") || arg.equals("-n")) {
                    globalProps.put(TypedProperties.CLI_NO_DB_KEY, TRUE);
                } else if (arg.equals("--import-contacts")) {
                    globalProps.put(TypedProperties.CLI_IMPORT_CONTACTS_KEY, TRUE);
                } else if (arg.equals("--import-bgp")) {
                    globalProps.put(TypedProperties.CLI_IMPORT_BGP_KEY, TRUE);
                } else if (arg.equals("--update-netname")) {
                    globalProps.put(TypedProperties.CLI_UPDATE_NETNAME_KEY, TRUE);
                } else if (arg.equals("--add-addresses") || arg.equals("--delete-addresses")) {
                    if (arg.equals("--delete-addresses")) {
                        globalProps.put(TypedProperties.CLI_DELETE_ADDRESSES_KEY, TRUE);
                    } else {
                        globalProps.put(TypedProperties.CLI_ADD_ADDRESSES_KEY, TRUE);
                    }
                    ++i;
                    if ((i < args.length) && !args[i].startsWith("-")) {
                        globalProps.put(TypedProperties.CLI_ADDRESSES_FILE_KEY, args[i]);
                    } else {
                        throw new CommandLineParseException("Missing address file to argument " + arg);
                    }
                } else if (arg.equals("--create-rss")) {
                    globalProps.put(TypedProperties.CLI_CREATE_STATS_RSS_KEY, TRUE);
                } else if (arg.equals("--create-xml")) {
                    globalProps.put(TypedProperties.CLI_CREATE_FLASH_XML_KEY, TRUE);
                } else if (arg.equals("--ui-org")) {
                    globalProps.put(TypedProperties.CLI_UI_ORGANIZATIONS, TRUE);                    
                } else if (arg.equals("--mail-dry-run") || arg.equals("-1")) {
                    globalProps.put(TypedProperties.CLI_MAIL_DRY_RUN_KEY, TRUE);
                } else if (arg.equals("--mail-dry-run2") || arg.equals("-2")) {
                    globalProps.put(TypedProperties.CLI_MAIL_DRY_RUN2_KEY, TRUE);
                } else if (arg.equals("--mail") || arg.equals("-m")) {
                    globalProps.put(TypedProperties.CLI_MAIL_KEY, TRUE);
                } else if (arg.equals("--use-org2") || arg.equals("-b")) {
                    globalProps.put(TypedProperties.CLI_USE_ORG2_KEY, TRUE);
                } else {
                    throw new CommandLineParseException("Unrecognized option: " + arg);
                }
            } else {
                File file = new File(arg);
                if (!file.canRead()) {
                    throw new MegatronException("Cannot read input file: " + file.getAbsolutePath());
                }
                inputFiles.add(arg);
            }
        }
    }


    private List<File> listJobTypeFiles() throws MegatronException {
        String dirStr = globalProps.get(JOB_TYPE_CONFIG_DIR_KEY);
        if (StringUtil.isNullOrEmpty(dirStr)) {
            throw new MegatronException("Job-type config dir not specified in global properties.");
        }
        File file = new File(dirStr);
        File[] files = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".properties");
            }
        });
        if (files == null) {
            throw new MegatronException("Cannot list job-type config files in directory: " + file.getAbsolutePath());
        }

        List<File> result = new ArrayList<File>(Arrays.asList(files));
        return result;
    }

}
