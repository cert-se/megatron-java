package se.sitic.megatron.core;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.FileUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Class to extend when exporting log entries.
 */
public abstract class AbstractExporter {
    private static final Logger log = Logger.getLogger(AbstractExporter.class);

    protected JobContext jobContext;
    protected TypedProperties props;

    
    public AbstractExporter(JobContext jobContext) {
        this.jobContext = jobContext;
        this.props = jobContext.getProps();
    }
    
    
    protected abstract String getTemplateDir();

    
    protected abstract String getTimestampFormat();

    
    protected String readTemplate(String filePropertyKey, boolean isMandatory) throws MegatronException {
        return readTemplate(filePropertyKey, null, isMandatory);
    }
    

    protected String readTemplate(String filePropertyKey, String languageCode, boolean isMandatory) throws MegatronException {
        String filename = props.getString(filePropertyKey, null);
        if (StringUtil.isNullOrEmpty(filename)) {
            if (isMandatory) {
                throw new MegatronException("Mandatory property for template file is missing.");
            } 
            return null;
        }
        File templateFile = null;
        if (languageCode != null) {
            templateFile = new File(getTemplateDir(), languageCode.toLowerCase());
            templateFile = new File(templateFile, filename);
            if (!templateFile.canRead()) {
                log.warn("Cannot read template for language '" + languageCode + "'. Using default template instead. File: " + templateFile.getAbsolutePath());
                // fallback to default template
                templateFile = new File(getTemplateDir(), filename);
            }
        } else {
            templateFile = new File(getTemplateDir(), filename);
        }
        try {
            return FileUtil.readFile(templateFile, Constants.UTF8);
        } catch (IOException e) {
            String msg = "Cannot read template file: " + templateFile.getAbsolutePath();
            throw new MegatronException(msg, e);
        }
    }

    
    protected void addString(Map<String, String> map, String key, String value) {
        String str = (value != null) ? value : ""; 
        map.put(key, str);
    }
    
    
    protected void addTimestamp(Map<String, String> map, String key, Long timestampInSecAndUtc) {
        if ((timestampInSecAndUtc != null) && (timestampInSecAndUtc > 0)) {
            Date date = SqlUtil.convertTimestamp(timestampInSecAndUtc);
            String str = DateUtil.formatDateTime(getTimestampFormat(), date);
            map.put(key, str);
        } else {
            map.put(key, "");
        }
    }

}
