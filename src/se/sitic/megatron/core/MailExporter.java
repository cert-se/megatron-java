package se.sitic.megatron.core;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import se.sitic.megatron.db.DbManager;
import se.sitic.megatron.entity.LogEntry;
import se.sitic.megatron.entity.MailJob;
import se.sitic.megatron.entity.NameValuePair;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.mail.MailAttachment;
import se.sitic.megatron.mail.MailSender;
import se.sitic.megatron.parser.LogEntryMapper;
import se.sitic.megatron.util.AppUtil;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.IpAddressUtil;
import se.sitic.megatron.util.SqlUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Sends mail from log entries in the database. 
 */
public class MailExporter extends AbstractExporter {
    // protected due to acess from innner class 
    protected static final Logger log = Logger.getLogger(MailExporter.class);
    
    private static final String JOB_INFO_HEADER = "---- Job Info ----" + Constants.LINE_BREAK;
    private static final String MESSAGE_HEADER = "---- Sample Message ----" + Constants.LINE_BREAK;
    private static final String RECIPIENTS_HEADER = "---- Recipients ----" + Constants.LINE_BREAK;
    private static final String SUMMARY_SUBJECT_TEMPLATE = "Megatron: Mail job #$mailJobId with RTIR ID #$rtirId finished";
    private static final String JOB_INFO_TEMPLATE = 
        JOB_INFO_HEADER +
        "No. of Sent Emails: $noOfSentMails" + Constants.LINE_BREAK +
        "No. of Sent Log Entries: $noOfSentLogEntries" + Constants.LINE_BREAK +
        "No. of Quarantine Log Entries: $noOfQuarantineLogEntries" + Constants.LINE_BREAK +
        "Mail Job ID: $mailJobId" + Constants.LINE_BREAK +
        "Mail Job Started: $mailJobStarted" + Constants.LINE_BREAK +
        "Job Name: $jobName" + Constants.LINE_BREAK +
        "Filename: $jobFilename" + Constants.LINE_BREAK +
        "RTIR ID: $rtirId" + Constants.LINE_BREAK +
        "Use Secondary Org.: $useOrg2" + Constants.LINE_BREAK +
        "Comment:" + Constants.LINE_BREAK + "$mailJobComment" + Constants.LINE_BREAK + Constants.LINE_BREAK;
    
    // Variables in subject and body
    private static final String MAIL_JOB_ID = "mailJobId";
    private static final String MAIL_JOB_STARTED = "mailJobStarted";
    private static final String MAIL_JOB_COMMENT = "mailJobComment";
    private static final String JOB_STARTED = "jobStarted";
    private static final String JOB_NAME = "jobName";
    private static final String JOB_FILENAME = "jobFilename";
    private static final String RTIR_ID = "rtirId";  
    private static final String ORGANIZATION_NAME = "organizationName";
    private static final String EMAIL_ADDRESSES = "emailAddresses";
    private static final String JOB_TYPE_NAME = "jobTypeName";
    
    private MailJobContext mailJobContext;
    private MailJob mailJob;
    private DbManager dbManager;
    private MailSender mailSender;
    private StringBuilder summaryBody;
    private Map<String, String> globalAttributeMap;

    
    /**
     * Constructor.
     */
    public MailExporter(MailJobContext mailJobContext) throws MegatronException {
        super(mailJobContext);
        this.mailJobContext = mailJobContext;
        this.props = mailJobContext.getProps();
        this.mailJob = mailJobContext.getMailJob();
        init();
    }

    
    /**
     * Add, or replace, a global attribute that can be used as a variable 
     * in a template. 
     */
    public void addAdditionalGlobalAttribute(String key, String value) {
        if (globalAttributeMap == null) {
            globalAttributeMap = new HashMap<String, String>();
        }
        globalAttributeMap.put(key, value);
    }


    /**
     * Sends one mail with specified log entries. Note: All log entries must 
     * belong to the same organization.
     */
    public void sendMail(List<LogEntry> logEntries) throws MegatronException {
        // -- check params
        if (logEntries.size() == 0) {
            String msg = "Assertion error: List of log entries is empty.";
            throw new MegatronException(msg);
        }
        Organization organization = null;
        Organization prevOrganization = null;
        for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
            organization = getOrganization(iterator.next());
            if ((prevOrganization != null) && !prevOrganization.equals(organization)) {
                String msg = "Assertion error: All log entries in the same mail must belong to the same organization.";
                throw new MegatronException(msg);
            }
        }

        // -- exists email addresses? no addresses == organization in kill list
        organization = getOrganization(logEntries.get(0));
        if (StringUtil.isNullOrEmpty(organization.getEmailAddresses())) {
            String msg = "Email address is missing; will not send email to organization#" +  organization.getId() + ": " + organization.getName();
            log.info(msg);
            return;
        }
        
        // -- valid email addresses?
        if (!AppUtil.isEmailAddressesValid(organization.getEmailAddresses())) {
            String msg = "Mail not sent; invalid email addresses (" + organization.getEmailAddresses() + ") for organization (" + organization.getName() + ").";
            log.error(msg);
            mailJobContext.writeToConsole(msg);
            return;
        }
        
        // -- remove quarantined log entries
        List<LogEntry> logEntriesToSend = null;
        int periodInSecs = props.getInt(AppProperties.MAIL_IP_QUARANTINE_PERIOD_KEY, 7*24*60*60);
        if (periodInSecs == 0) {
            // 0 to turn off quarantine
            logEntriesToSend = logEntries;
        } else {
            logEntriesToSend = new ArrayList<LogEntry>();
            for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
                LogEntry logEntry = iterator.next();
                Long ip = mailJob.isUsePrimaryOrg() ? logEntry.getIpAddress() : logEntry.getIpAddress2(); 
                boolean exists = (ip != null) && dbManager.existsMailForIp(ip.longValue(), organization, periodInSecs, mailJob.isUsePrimaryOrg());
                if (exists) {
                    @SuppressWarnings("null")
                    String msg = "Ignoring log entry#" + logEntry.getId() + ", because IP is quarantined. An email have already been sent for this IP: " + 
                        IpAddressUtil.convertIpAddress(ip, false);
                    log.info(msg);
                    mailJobContext.writeToConsole(msg);
                    mailJobContext.incNoOfQuarantineLogEntries(1);
                } else {
                    logEntriesToSend.add(logEntry);
                }
            }
        }
        
        // -- all log entries removed?
        if (logEntriesToSend.isEmpty()) {
            String msg = "All log entries quarantined; will not send email to organization#" +  organization.getId() + ": " + organization.getName();
            log.info(msg);
            mailJobContext.writeToConsole(msg);
            return;
        }
        
        // -- send mail (or write to the console if --dry-run2)
        MessageData messageData = createMessageData(logEntriesToSend);
        if (props.isMailDryRun2()) {
            mailJobContext.writeToConsole("To: " + organization.getEmailAddresses());
            mailJobContext.writeToConsole("Subject: " + messageData.getSubject());
            String body = messageData.getBody();
            mailJobContext.writeToConsole("---- begin body (" + body.length() + " characters) ----");
            mailJobContext.writeToConsole(body);
            mailJobContext.writeToConsole("---- end body ----");
            mailJobContext.writeToConsole("");
            mailJobContext.incNoOfMailsTobeSent(1);
        } else if (props.isMailDryRun()) {
            addToSummary(organization, messageData);
            mailJobContext.incNoOfMailsTobeSent(1);
        } else {
            addToSummary(organization, messageData);
            log.info("Sending abuse mail: " + organization.getEmailAddresses());
            mailSender.clear();
            mailSender.setToAddresses(organization.getEmailAddresses("To", true));
            mailSender.setCcAddresses(organization.getEmailAddresses("Cc", true));
            // Add BCC addresses plus any mail archive BCC addresses
            mailSender.setBccAddresses(props.getString(organization.getEmailAddresses("Bcc", true) + " ," + AppProperties.MAIL_ARCHIVE_BCC_ADDRESSES_KEY, null) );
            mailSender.setSubject(messageData.getSubject());
            mailSender.setBody(messageData.getBody());
            if (messageData.getAttachment() != null) {
                mailSender.addAttachment(messageData.getAttachment());
            }
            mailSender.send(props);
            mailJobContext.incNoOfSentMails(1);
            mailJobContext.incNoOfSentLogEntries(logEntriesToSend.size());
            for (Iterator<LogEntry> iterator = logEntriesToSend.iterator(); iterator.hasNext(); ) {
                LogEntry logEntry = iterator.next();
                log.debug("Adding log entry to mail job. LogEntry#" + logEntry.getId());
                mailJob.addToLogEntries(logEntry);
            }
            // Removing the line below due to deprication of the update method in DbManager
            // The method is depricated since there have been problems with Hibernate persisting the MailJob object
            // properly after a LogEntry has been added to the LogEntry collection.
            // dbManager.updateMailJob(mailJob);
        }
    }

    
    /**
     * Sends summary email, or writes summary to the console if --dry-runX is enabled.
     */
    public void sendSummaryMail() throws MegatronException {
        if (props.isMailDryRun2()) {
            return;
        }
        if (summaryBody == null) {
            summaryBody = new StringBuilder(512);
            summaryBody.append("Mail Job is empty. It contains no emails.").append(Constants.LINE_BREAK).append(Constants.LINE_BREAK);
            // init globalAttributeMap
            createAttributeMap(null);
        }
        
        String jobInfo = JOB_INFO_TEMPLATE;
        jobInfo = StringUtil.replace(jobInfo, "$noOfSentMails", mailJobContext.getNoOfSentMails() + "");
        jobInfo = StringUtil.replace(jobInfo, "$noOfSentLogEntries", mailJobContext.getNoOfSentLogEntries() + "");
        jobInfo = StringUtil.replace(jobInfo, "$noOfQuarantineLogEntries", mailJobContext.getNoOfQuarantineLogEntries() + "");
        jobInfo = StringUtil.replace(jobInfo, "$useOrg2", !mailJob.isUsePrimaryOrg() + "");
        jobInfo = AppUtil.replaceVariables(jobInfo, globalAttributeMap, false, "JOB_INFO_TEMPLATE (hardcoded)");
        summaryBody.insert(0, jobInfo);
        
        if (props.isMailDryRun()) {
            mailJobContext.writeToConsole(summaryBody.toString());
        } else {
            String toAddresses = props.getString(AppProperties.MAIL_SUMMARY_TO_ADDRESSES_KEY, "");
            if (toAddresses.trim().length() == 0) {
                String msg = "Sending mail job summary email skipped; no to-addresses defined in config.";
                log.info(msg);
            } else {
                log.info("Sending summary mail: " + toAddresses);
                String subject = props.getString(AppProperties.MAIL_JOB_SUMMARY_SUBJECT_TEMPLATE_KEY, SUMMARY_SUBJECT_TEMPLATE);
                subject = AppUtil.replaceVariables(subject, globalAttributeMap, false, AppProperties.MAIL_JOB_SUMMARY_SUBJECT_TEMPLATE_KEY);
                mailSender.clear();
                mailSender.setToAddresses(toAddresses);
                mailSender.setSubject(subject);
                mailSender.setBody(summaryBody.toString());
                mailSender.send(props);
            }
        }
    }

    
    public void close() throws MegatronException {
        dbManager.close();
    }

    
    @Override
    protected String getTemplateDir() {
        return props.getString(AppProperties.MAIL_TEMPLATE_DIR_KEY, "conf/template/mail");
    }

    
    @Override
    protected String getTimestampFormat() {
        return props.getString(AppProperties.MAIL_TIMESTAMP_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss z");
    }

    
    @Override
    protected void init() throws MegatronException {
        super.init();
        
        this.dbManager = mailJobContext.getDbManager();
        this.mailSender = new MailSender();
    }
    
    
    private MessageData createMessageData(List<LogEntry> logEntries) throws MegatronException {
        String templateName = null;
        String template = null;
        Organization organization = getOrganization(logEntries.get(0));
        Map<String, String> attributeMap = createAttributeMap(organization);
        String langCode = null;
        String defaultLangCode = props.getString(AppProperties.MAIL_DEFAULT_LANGUAGE_CODE_KEY, null);
        if ((organization != null) && (organization.getLanguageCode() != null) && (defaultLangCode != null) && !organization.getLanguageCode().equalsIgnoreCase(defaultLangCode)) {
            langCode = organization.getLanguageCode();
        }
        
        // -- subject
        if (langCode != null) {
            templateName = AppProperties.MAIL_SUBJECT_TEMPLATE_KEY + "." + langCode.toLowerCase();
            template = props.getString(templateName, null);
        }
        if (template == null) {
            templateName = AppProperties.MAIL_SUBJECT_TEMPLATE_KEY;
            template = props.getString(templateName, null);
            if (template == null) {
                throw new MegatronException("Mandatory property not defined: " + AppProperties.MAIL_SUBJECT_TEMPLATE_KEY);
            }
        }
        String subject = AppUtil.replaceVariables(template, attributeMap, false, templateName);

        // -- body and attachment
        StringBuilder body = new StringBuilder(2*1024);
        StringBuilder attachment = new StringBuilder(2*1024);
        // header
        templateName = props.getString(AppProperties.MAIL_HEADER_FILE_KEY, null);
        template = readTemplate(AppProperties.MAIL_HEADER_FILE_KEY, langCode, false);
        if (template != null) {
            body.append(AppUtil.replaceVariables(template, attributeMap, false, templateName));
        }
        // rows
        templateName = props.getString(AppProperties.MAIL_ROW_FILE_KEY, null);
        boolean raiseErrorForDebugTemplate = props.getBoolean(AppProperties.MAIL_RAISE_ERROR_FOR_DEBUG_TEMPLATE_KEY, true);
        if (raiseErrorForDebugTemplate && ((templateName == null) || templateName.equals("debug_row.txt"))) {
            String msg = "Mail templates are not specified or debug templates are used. Probably, no templates exists for this job type.";
            throw new MegatronException(msg);
        }
        template = readTemplate(AppProperties.MAIL_ROW_FILE_KEY, langCode, false);
        String attachmentTemplateName = props.getString(AppProperties.MAIL_ATTACHMENT_ROW_FILE_KEY, null);
        String attachmentTemplate = readTemplate(AppProperties.MAIL_ATTACHMENT_ROW_FILE_KEY, langCode, false);
        if ((template != null) || (attachmentTemplate != null)) {
            for (Iterator<LogEntry> iterator = logEntries.iterator(); iterator.hasNext(); ) {
                LogEntry logEntry = iterator.next();
                LogEntryMapper mapper = new LogEntryMapper(props, rewriter, logEntry);
                if (template != null) {
                    String template2 = replaceJobTypeVariables(template, logEntry);
                    body.append(mapper.replaceVariables(template2, false, templateName));
                }
                if (attachmentTemplate != null) {
                    String template2 = replaceJobTypeVariables(attachmentTemplate, logEntry);
                    attachment.append(mapper.replaceVariables(template2, false, attachmentTemplateName));
                }
            }
        }
        // attachment header and footer
        if (attachment.length() > 0) {
            templateName = props.getString(AppProperties.MAIL_ATTACHMENT_HEADER_FILE_KEY, null);
            template = readTemplate(AppProperties.MAIL_ATTACHMENT_HEADER_FILE_KEY, langCode, false);
            if (template != null) {
                attachment.insert(0, AppUtil.replaceVariables(template, attributeMap, false, templateName));
            }
            templateName = props.getString(AppProperties.MAIL_ATTACHMENT_FOOTER_FILE_KEY, null);
            template = readTemplate(AppProperties.MAIL_ATTACHMENT_FOOTER_FILE_KEY, langCode, false);
            if (template != null) {
                attachment.append(AppUtil.replaceVariables(template, attributeMap, false, templateName));
            }
        }
        // footer
        templateName = props.getString(AppProperties.MAIL_FOOTER_FILE_KEY, null);
        template = readTemplate(AppProperties.MAIL_FOOTER_FILE_KEY, langCode, false);
        if (template != null) {
            body.append(AppUtil.replaceVariables(template, attributeMap, false, templateName));
        }

        return new MessageData(subject, body.toString(), attachment.toString());
    }

    
    private Map<String, String> createAttributeMap(Organization organization) {
        if (globalAttributeMap == null) {
            globalAttributeMap = new HashMap<String, String>();
            String mailJobId = (mailJob.getId() != null) ? mailJob.getId().toString() : "-"; 
            addString(globalAttributeMap, MAIL_JOB_ID, mailJobId);
            addTimestamp(globalAttributeMap, MAIL_JOB_STARTED, SqlUtil.convertTimestampToSec(mailJobContext.getStartedTimestamp()));
            addString(globalAttributeMap, MAIL_JOB_COMMENT, mailJob.getComment());
            addTimestamp(globalAttributeMap, JOB_STARTED, SqlUtil.convertTimestampToSec(mailJobContext.getJob().getStarted()));
            addString(globalAttributeMap, JOB_NAME, mailJob.getJob().getName());
            addString(globalAttributeMap, JOB_FILENAME, mailJob.getJob().getFilename());
            String rtirId = StringUtil.isNullOrEmpty(props.getId()) ? "-" : props.getId(); 
            addString(globalAttributeMap, RTIR_ID, rtirId);
        }
        
        Map<String, String> result = new HashMap<String, String>();
        result.putAll(globalAttributeMap);
        if (organization != null) {
            addString(result, ORGANIZATION_NAME, organization.getName());
            addString(result, EMAIL_ADDRESSES, organization.getEmailAddresses());
        }
        
        return result;
    }

    
    private Organization getOrganization(LogEntry logEntry) {
        return mailJob.isUsePrimaryOrg() ? logEntry.getOrganization() : logEntry.getOrganization2();
    }
    
    
    private void addToSummary(Organization organization, MessageData messageData) {
        if (summaryBody == null) {
            summaryBody = new StringBuilder(1024);
            
            summaryBody.append(MESSAGE_HEADER);
            summaryBody.append("Subject: " + messageData.getSubject()).append(Constants.LINE_BREAK);
            summaryBody.append(messageData.getBody()).append(Constants.LINE_BREAK);
            summaryBody.append(RECIPIENTS_HEADER);
        }

        String str = "@organizationName@: @emailAddresses@" + Constants.LINE_BREAK;
        str = StringUtil.replace(str, "@organizationName@", organization.getName());
        str = StringUtil.replace(str, "@emailAddresses@", organization.getEmailAddresses());
        summaryBody.append(str);
    }

    
    private String replaceJobTypeVariables(String template, LogEntry logEntry) {
        String result = template;
        String jobTypeNameVar = LogEntryMapper.VARIABLE_PREFIX + JOB_TYPE_NAME; 
        if (template.indexOf(jobTypeNameVar) != -1) {
            // -- fetch jobType.name
            String replaceWith = null;
            try {
                replaceWith = logEntry.getJob().getJobType().getName();
            } catch (RuntimeException e) {
                replaceWith = "[Undefined]";
                log.error("Cannot get job_type.name for log_entry.", e);
            }
            // -- replace if value is in mapping list
            List<NameValuePair> mappingList = props.getNameValuePairList(AppProperties.EXPORT_JOB_TYPE_NAME_MAPPER_KEY, null);
            if ((mappingList != null) && (mappingList.size() > 0)) {
                for (Iterator<NameValuePair> iterator = mappingList.iterator(); iterator.hasNext(); ) {
                    NameValuePair nameValuePair = iterator.next();
                    if (nameValuePair.getName().equals(replaceWith)) {
                        replaceWith = nameValuePair.getValue();
                    }
                }
            }
            result = StringUtil.replace(template, jobTypeNameVar, replaceWith);
        }
        return result;
    }


    /**
     * Entity class for the content in an email.
     */
    private class MessageData {
        private String subject;
        private String body;
        private MailAttachment attachment;
        
        public MessageData(String subject, String body, String attachment) {
            this.subject = subject;
            this.body = body;
            
            if (!StringUtil.isNullOrEmpty(attachment)) {
                String charSet = props.getString(AppProperties.EXPORT_CHAR_SET_KEY, Constants.UTF8);
                String attachmentName = props.getString(AppProperties.MAIL_ATTACHMENT_NAME_KEY, "abuse-report.csv");
                try {
                    this.attachment = new MailAttachment(attachment.getBytes(charSet), Constants.MIME_TEXT_PLAIN, attachmentName);
                } catch (UnsupportedEncodingException e) {
                    log.error("Cannot create attachment due to invalid character set: " + charSet, e);
                }
            }
        }

        
        public String getSubject() {
            return subject;
        }


        public String getBody() {
            return body;
        }
        
        
        public MailAttachment getAttachment() {
            return attachment;
        }
    }

}
