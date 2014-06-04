package se.sitic.megatron.core;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import se.sitic.megatron.entity.NameValuePair;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Properties that can be converted to a data-type, e.g. an integer or a boolean.
 * Supports a hierarchy of properties: if a property is missing in the job-type
 * properties file, then the globals properties file is used.
 * <p>
 * Handles CLI-arguments which can be specified at the command line or by the
 * web application for each job. 
 */
public class TypedProperties {
    // Keys for CLI arguments. 
    public static final String CLI_SLURP_KEY = "cli.slurp";
    public static final String CLI_EXPORT_KEY = "cli.export";
    public static final String CLI_DELETE_KEY = "cli.delete";    
    public static final String CLI_DELETE_ALL_KEY = "cli.deleteAll";    
    public static final String CLI_JOB_KEY = "cli.job";
    public static final String CLI_JOB_TYPE_KEY = "cli.jobType";
    public static final String CLI_OUTPUT_DIR_KEY = "cli.outputDir";
    public static final String CLI_ID_KEY = "cli.id";
    public static final String CLI_PRIO_KEY = "cli.prio";    
    public static final String CLI_LIST_PRIOS_KEY = "cli.listPrios";
    public static final String CLI_LIST_JOBS_KEY = "cli.listJobs";
    public static final String CLI_WHOIS_KEY = "cli.whois";    
    public static final String CLI_DAYS_IN_LIST_JOBS_KEY = "cli.daysInListJobs";
    public static final String CLI_JOB_INFO_KEY = "cli.jobInfo";
    public static final String CLI_NO_DB_KEY = "cli.noDb";
    public static final String CLI_STDOUT_KEY = "cli.stdout";
    public static final String CLI_IMPORT_CONTACTS_KEY = "cli.importContacts";
    public static final String CLI_IMPORT_BGP_KEY = "cli.importBgp";
    public static final String CLI_UPDATE_NETNAME_KEY = "cli.updateNetname";
    public static final String CLI_ADD_ADDRESSES_KEY = "cli.addAddresses";
    public static final String CLI_DELETE_ADDRESSES_KEY = "cli.deleteAddresses";
    public static final String CLI_ADDRESSES_FILE_KEY = "cli.addressFileKey";
    public static final String CLI_CREATE_STATS_RSS_KEY = "cli.createStatsRss";
    public static final String CLI_CREATE_FLASH_XML_KEY = "cli.createFlashXml";
    public static final String CLI_CREATE_REPORTS_KEY = "cli.createReports";
    public static final String CLI_CREATE_REPORT_KEY = "cli.createReport";    
    public static final String CLI_UI_ORGANIZATIONS = "cli.uiOrganizations"; 
    public static final String CLI_MAIL_DRY_RUN_KEY = "cli.mailDryRun";
    public static final String CLI_MAIL_DRY_RUN2_KEY = "cli.mailDryRun2";
    public static final String CLI_MAIL_KEY = "cli.mail";
    public static final String CLI_USE_ORG2_KEY = "cli.useOrg2";

    private static final Logger log = Logger.getLogger(TypedProperties.class);

    /** Strings that is converted to "true" (case insensitive). */
    private static final String[] TRUE_STRINGS = { "1", "true", "on" };

    /** Main properies. */
    private Map<String, String> props;

    /** Properties to look in if value is missing in props. Starts from the end. May be null. */
    private LinkedList<Map<String, String>> additionalPropsList;


    /**
     * Constructor.
     *
     * @param props Properties (not null).
     * @param additionalPropsList Properties to look in if value is missing in props. Starts from the end. May be null.
     */
    public TypedProperties(Map<String, String> props, List<Map<String, String>> additionalPropsList) {
        if (props == null) {
            throw new NullPointerException("Argument is null: props");
        }

        this.props = props;
        if (additionalPropsList != null) {
            this.additionalPropsList = new LinkedList<Map<String, String>>(additionalPropsList);
        }
    }

    
    /**
     * Adds specified additional properties, which will override already 
     * defined properties. Use this method to add or modified properties. 
     */
    public void addAdditionalProps(Map<String, String> props) {
        this.additionalPropsList.add(props);
    }
    
    
    /**
     * Returns property value for specified key. If property is not found in
     * properties, or in additional properties, specifed default value is
     * returned.
     *
     * @param key name of property.
     * @param defaultValue value returned if property was not found, or in
     *      case of exception.
     */
    public String getString(String key, String defaultValue) {
        String result = props.get(key);

        if ((result == null) && (additionalPropsList != null)) {
            for (ListIterator<Map<String, String>> iterator = additionalPropsList.listIterator(additionalPropsList.size()); iterator.hasPrevious(); ) {
                Map<String, String> map = iterator.previous();
                result = map.get(key);
                if (result != null) {
                    break;
                }
            }
        }

        return (result != null) ? result : defaultValue;
    }


    /**
     * Returns string array for specified key.<p>
     * Format in properties file:<pre>
     * name.0=value0
     * name.1=value1
     * [...]
     * </pre>
     *
     * @param key name of property.
     * @param defaultValue value returned if property was not found.
     */
    public String[] getStringList(String key, String[] defaultValue) {
        String[] result = null;

        // find map that contains list
        Map<String, String> map = null;
        if (props.containsKey(key + ".0") || props.containsKey(key + ".1")) {
            map = props;
        } else if (additionalPropsList != null) {
            for (ListIterator<Map<String, String>> iterator = additionalPropsList.listIterator(additionalPropsList.size()); iterator.hasPrevious(); ) {
                Map<String, String> candidateMap = iterator.previous();
                if (candidateMap.containsKey(key + ".0") || candidateMap.containsKey(key + ".1")) {
                    map = candidateMap;
                    break;
                }
            }
        }

        // create result from list in map
        if (map != null) {
            List<String> resultList = new ArrayList<String>();
            int i = map.containsKey(key + ".0") ? 0 : 1;
            while (true) {
                String value = map.get(key + "." + Integer.toString(i++));
                if (value != null) {
                    resultList.add(value);
                } else {
                    break;
                }
            }
            result = resultList.toArray(new String[resultList.size()]);
        }

        return (result != null) ? result : defaultValue;
    }

    
    /**
     * Returns string array for property value that is comma separated.
     */
    public String[] getStringListFromCommaSeparatedValue(String key, String[] defaultValue, boolean trim) {
        String str = getString(key, null);
        if (str == null) {
            return defaultValue;
        }

        String[] result = str.split(",");
        if (result != null) {
            for (int i = 0; i < result.length; i++) {
                result[i] = result[i].trim();
            }
        } else {
            result = defaultValue;
        }
        
        return result;
    }

    
    /**
     * @see #getStringList(String, String[])
     */
    public List<NameValuePair> getNameValuePairList(String key, List<NameValuePair> defaultList) {
        List<NameValuePair> result = null;

        final String delim = "=";
        String[] strList = getStringList(key, null);
        if (strList != null) {
            result = new ArrayList<NameValuePair>(strList.length);
            for (int i = 0; i < strList.length; i++) {
                String[] headTail = StringUtil.splitHeadTail(strList[i], delim, false);
                if (StringUtil.isNullOrEmpty(headTail[0]) || StringUtil.isNullOrEmpty(headTail[1])) {
                    String msg = "Cannot parse NameValuePair-list property: " + key + ", value: " + Arrays.toString(strList);
                    log.error(msg);
                    result = defaultList;
                    break;
                }
                result.add(new NameValuePair(headTail[0], headTail[1]));
            }
        }

        return result;
    }


    /**
     * @see #getString(String, String)
     */
    public long getLong(String key, long defaultValue) {
        String longStr = getString(key, null);

        long result = defaultValue;
        if (longStr != null) {
            try {
                result = Long.parseLong(longStr.trim());
            } catch (NumberFormatException e) {
                String msg = "Cannot parse integer property: " + key + ", value: " + longStr;
                log.error(msg, e);
                result = defaultValue;
            }
        }

        return result;
    }


    /**
     * @see #getString(String, String)
     */
    public int getInt(String key, int defaultValue) {
        return (int)getLong(key, defaultValue);
    }


    /**
     * @see #getString(String, String)
     */
    public double getDouble(String key, double defaultValue) {
        String doubleStr = getString(key, null);

        double result = defaultValue;
        if (doubleStr != null) {
            try {
                result = Double.parseDouble(doubleStr.trim());
            } catch (NumberFormatException e) {
                String msg = "Cannot parse decimal number property: " + key + ", value: " + doubleStr;
                log.error(msg, e);
                result = defaultValue;
            }
        }

        return result;
    }


    /**
     * @see #getString(String, String)
     */
    public float getFloat(String key, float defaultValue) {
        return (float)getDouble(key, defaultValue);
    }


    /**
     * @see #getString(String, String)
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String booleanStr = getString(key, null);

        boolean result = false;
        if (booleanStr != null) {
            booleanStr = booleanStr.trim();
            for (int i = 0; i < TRUE_STRINGS.length; i++) {
                if (TRUE_STRINGS[i].equalsIgnoreCase(booleanStr)) {
                    result = true;
                    break;
                }
            }
        } else {
            result = defaultValue;
        }

        return result;
    }


    /**
     * @see #getString(String, String)
     * @see se.sitic.megatron.util.DateUtil#DATE_TIME_FORMAT_WITH_SECONDS
     */
    public Date getDate(String key, String format, Date defaultValue) {
        String dateStr = getString(key, null);

        Date result = defaultValue;
        if (dateStr != null) {
            try {
                result = DateUtil.parseDateTime(format, dateStr);
            } catch (ParseException e) {
                String msg = "Cannot parse date property: " + key + ", value: " + dateStr;
                log.error(msg, e);
                result = defaultValue;
            }
        }

        return result;
    }


    /**
     * Returns true if specified property exists.
     *
     * @param key name of property.
     */
    public boolean existsProperty(String key) {
        return (getString(key, null) != null);
    }

    
    /**
     * Returns all property keys. 
     */
    public Set<String> keySet() {
        Set<String> result = new HashSet<String>();

        result.addAll(props.keySet());
        for (Iterator<Map<String, String>> iterator = additionalPropsList.iterator(); iterator.hasNext(); ) {
            result.addAll(iterator.next().keySet());
        }
        return result;
    }

    
    /**
     * Returns CLI switch: --slurp
     */
    public boolean isSlurp() {
        return getBoolean(CLI_SLURP_KEY, false);
    }

    
    /**
     * Returns CLI switch: --export
     */
    public boolean isExport() {
        return getBoolean(CLI_EXPORT_KEY, false);
    }

    
    /**
     * Returns CLI switch: --delete
     */
    public boolean isDelete() {
        return getBoolean(CLI_DELETE_KEY, false);
    }

    
    /**
     * Returns CLI switch: --delete-all
     */
    public boolean isDeleteAll() {
        return getBoolean(CLI_DELETE_ALL_KEY, false);
    }

    
    /**
     * Returns CLI switch: --job
     */
    public String getJob() {
        return getString(CLI_JOB_KEY, "");
    }

    
    /**
     * Returns CLI switch: --job-type
     */
    public String getJobType() {
        return getString(CLI_JOB_TYPE_KEY, "");
    }

    
    /**
     * Returns CLI switch: --output-dir
     */
    public String getOutputDir() {
        String result = getString(CLI_OUTPUT_DIR_KEY, "");
        if (result.length() == 0) {
            result = getString(AppProperties.OUTPUT_DIR_KEY, "tmp/export");
        }
        return result;
    }

    
    /**
     * Returns CLI switch: --id
     */
    public String getId() {
        return getString(CLI_ID_KEY, "");
    }

    
    /**
     * Returns CLI switch: --prio
     */
    public String getPrio() {
        return getString(CLI_PRIO_KEY, "");
    }

    
    /**
     * Returns CLI switch: --list-prios
     */
    public boolean isListPrios() {
        return getBoolean(CLI_LIST_PRIOS_KEY, false);
    }

    
    /**
     * Returns CLI switch: --list-jobs
     */
    public boolean isListJobs() {
        return getBoolean(CLI_LIST_JOBS_KEY, false);
    }

    
    /**
     * Returns no of days (if specified) for --list-jobs. 
     */
    public int getDaysInListJobs() {
        int defaultVal = 2;
        String val = getString(CLI_DAYS_IN_LIST_JOBS_KEY, defaultVal + "");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.error("Cannot not parse --list-jobs integer value: " + val);
            return defaultVal;
        }
    }

    
    /**
     * Returns CLI switch: --job-info
     */
    public boolean isJobInfo() {
        return getBoolean(CLI_JOB_INFO_KEY, false);
    }

    
    /**
     * Returns CLI switch: --whois
     */
    public boolean isWhois() {
        return getBoolean(CLI_WHOIS_KEY, false);
    }

    
    /**
     * Returns CLI switch: --no-db
     */
    public boolean isNoDb() {
        return getBoolean(CLI_NO_DB_KEY, false);
    }

    
    /**
     * Returns CLI switch: --stdout
     */
    public boolean isStdout() {
        return getBoolean(CLI_STDOUT_KEY, false);
    }

    
    /**
     * Returns CLI switch: --import-contacts
     */
    public boolean isImportContacts() {
        return getBoolean(CLI_IMPORT_CONTACTS_KEY, false);
    }

    
    /**
     * Returns CLI switch: --import-bgp
     */
    public boolean isImportBgp() {
        return getBoolean(CLI_IMPORT_BGP_KEY, false);
    }

    
    /**
     * Returns CLI switch: --update-netname
     */
    public boolean isUpdateNetname() {
        return getBoolean(CLI_UPDATE_NETNAME_KEY, false);
    }

    
    /**
     * Returns CLI switch: --add-addresses
     */
    public boolean isAddAddresses() {
        return getBoolean(CLI_ADD_ADDRESSES_KEY, false);
    }

    
    /**
     * Returns CLI switch: --delete-addresses
     */
    public boolean isDeleteAddresses() {
        return getBoolean(CLI_DELETE_ADDRESSES_KEY, false);
    }

    
    /**
     * Returns address file for CLI switch --add-addresses or --delete-addresses. 
     */
    public String getAddressesFile() {
        return getString(CLI_ADDRESSES_FILE_KEY, "");
    }

    
    /**
     * Returns CLI switch: --create-rss
     */
    public boolean isCreateStatsRss() {
        return getBoolean(CLI_CREATE_STATS_RSS_KEY, false);
    }

    
    /**
     * Returns CLI switch: --create-xml
     * <p>
     * --create-xml is deprecated. Use --create-reports instead.
     */
    public boolean isCreateFlashXml() {
        return getBoolean(CLI_CREATE_FLASH_XML_KEY, false);
    }

    
    /**
     * Returns CLI switch: --create-reports
     */
    public boolean isCreateReports() {
        return getBoolean(CLI_CREATE_REPORTS_KEY, false);
    }

    
    /**
     * Returns CLI switch: --create-report
     */
    public String getCreateReport() {
        return getString(CLI_CREATE_REPORT_KEY, null);
    }

    
    /**
     * Returns CLI switch: --ui-org
     */
    public boolean isUiOrg() {
        return getBoolean(CLI_UI_ORGANIZATIONS, false);
    }


    /**
     * Returns CLI switch: --mail-dry-run
     */
    public boolean isMailDryRun() {
        return getBoolean(CLI_MAIL_DRY_RUN_KEY, false);
    }

    
    /**
     * Returns CLI switch: --mail-dry-run2
     */
    public boolean isMailDryRun2() {
        return getBoolean(CLI_MAIL_DRY_RUN2_KEY, false);
    }

    
    /**
     * Returns CLI switch: --mail
     */
    public boolean isMail() {
        return getBoolean(CLI_MAIL_KEY, false);
    }

    
    /**
     * Returns CLI switch: --use-org2
     */
    public boolean isUseOrg2() {
        return getBoolean(CLI_USE_ORG2_KEY, false);
    }

}
