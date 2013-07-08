package se.sitic.megatron.mail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.StringUtil;


/**
 * Sends mail with attachment using SMTP.
 */
public class MailSender {
    private static final Logger log = Logger.getLogger(MailSender.class);

    private static final String JAVA_MAIL_SMTP_HOST_KEY = "mail.smtp.host";

    private String[] smtpHosts;
    private int smtpHostIndex;
    private boolean debug;
    private boolean htmlMail;
    private boolean encrypt;
    private boolean sign;

    private String fromAddress;
    private String toAddresses;
    private String bccAddresses;
    private String replyToAddresses;
    private String subject;
    private String body;
    private List<MailAttachment> attachments;


    /**
     * Constructor.
     */
    public MailSender() {
        // empty
    }


    /**
     * Sends mail. All fields are optional and may be read from properties, e.g from-address, to-addresses, subject, and body.
     */
    public void send(TypedProperties props) throws MailException {
        // Init properties
        this.smtpHosts = props.getStringList(AppProperties.MAIL_SMTP_HOST_KEY, null);
        this.debug = props.getBoolean(AppProperties.MAIL_DEBUG_KEY, false);
        this.htmlMail = props.getBoolean(AppProperties.MAIL_HTML_MAIL_KEY, true);
        this.sign = props.getBoolean(AppProperties.MAIL_ENCRYPTION_ENCRYPT_KEY, false);
        this.encrypt = props.getBoolean(AppProperties.MAIL_ENCRYPTION_SIGN_KEY, false);

        // -- Check pre-condition
        if (getSmtpHost() == null) {
            throw new MailException("SMTP host not specified.");
        }

        String from = (fromAddress != null) ? fromAddress : props.getString(AppProperties.MAIL_FROM_ADDRESS_KEY, "sitic@sitic.se");
        String to = (toAddresses != null) ? toAddresses : props.getString(AppProperties.MAIL_TO_ADDRESSES_KEY, null);
        String bcc = (bccAddresses != null) ? bccAddresses : props.getString(AppProperties.MAIL_BCC_ADDRESSES_KEY, null);
        String replyTo = (replyToAddresses != null) ? replyToAddresses : props.getString(AppProperties.MAIL_REPLY_TO_ADDRESSES_KEY, null);

        // -- Logging
        if (log.isDebugEnabled()) {
            log.debug("Sending mail to: " + (toAddresses != null ? to : to + " [default]"));
            log.debug("  BCC-address: " + (bccAddresses != null ? bcc : bcc + " [default]"));
            log.debug("  From-address: " + (fromAddress != null ? from : from + " [default]"));
            log.debug("  ReplyTo-address: " + (replyToAddresses != null ? replyTo : replyTo + " [default]"));
            log.debug("  Subject: " + (subject != null ? subject : "[default]"));
            log.debug("  Body size: " + ((body != null) ? body.length() : 0));
            log.debug("  Attachments: " + ((attachments != null) ? attachments.size() : 0));
        }

        // -- Set properties
        Properties sessionProps = new Properties();
        sessionProps.put(JAVA_MAIL_SMTP_HOST_KEY, getSmtpHost());
        Session session = Session.getDefaultInstance(sessionProps, null);
        session.setDebug(debug);

        try {
            // -- Create message
            MimeMessage message = new MimeMessage(session);
            message.setSentDate(new Date());
            
            // -- Set addresses
            message.setFrom(new InternetAddress(from));
            if (!StringUtil.isNullOrEmpty(replyTo)) {
                message.setReplyTo(parseAddresses(replyTo));
            }
            message.setRecipients(Message.RecipientType.TO, parseAddresses(to));
            if (bcc != null) {
                message.setRecipients(Message.RecipientType.BCC, parseAddresses(bcc));
            }

            // -- Set subject and content
            message.setSubject(StringUtil.getNotNull(subject, "[Empty Subject]"));
            String mimeSubType = htmlMail ? "html" : "plain";
            if ((attachments == null) || (attachments.size() == 0)) {
                message.setText(StringUtil.getNotNull(body, "[Empty Body]"), Constants.UTF8, mimeSubType);
            } else {
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(StringUtil.getNotNull(body, "[Empty Body]"), Constants.UTF8, mimeSubType);

                // body
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);

                // attachments
                for (Iterator<MailAttachment> iterator = attachments.iterator(); iterator.hasNext(); ) {
                    MailAttachment attachment = iterator.next();
                    try {
                        messageBodyPart = new MimeBodyPart();
                        DataSource dataSource = new ByteArrayDataSource(attachment.getContent(), attachment.getMimeType());
                        messageBodyPart.setDataHandler(new DataHandler(dataSource));
                        messageBodyPart.setFileName(attachment.getAttachmentName());
                        multipart.addBodyPart(messageBodyPart);
                    } catch (IOException e) {
                        String msg = "Cannot read content for attachment: " + attachment.getAttachmentName();
                        log.error(msg, e);
                        throw new MailException(msg, e);
                    }
                }

                // add to message
                message.setContent(multipart);

            }

            if (encrypt || sign) {
            	throw new UnsupportedOperationException("Mail signing and encryption not implemented.");

                // TODO Add support for mail signing and encryption
//                try {
//            		String encryptionType = props.getString(AppProperties.MAIL_ENCRYPTION_TYPE, "PGP");
//            		MailEncryptor encryptor = new MailEncryptor (encryptionType);
//
//            		if (encrypt) {
//            			System.out.println("Encrypt");
//            			message = encryptor.encrypt(session, message);
//            		}
//            		if (sign) {
//            			System.out.println("Sign");
//            			message = encryptor.sign(session, message);
//            		}
//            	}
//            	catch (EncryptionException e) {
//                    String msg = "Cannot sign or encrypt the mail: ";
//                    log.error(msg, e);
//                    throw new MailException(msg, e);
//                }
            }

            // -- Send mail (try all SMTP hosts in list if mail cannot be delivered)
            boolean mailSent = false;
            while (!mailSent) {
                try {
                    Transport.send(message);
                    mailSent = true;
                } catch (MessagingException e) {
                    String msg = "Cannot send mail using SMTP host '" + getSmtpHost() + "'.";
                    log.error(msg, e);
                    ++this.smtpHostIndex;
                    if (getSmtpHost() == null) {
                        log.info("SMTP host list exhausted. Giving up; mail not delivered.", e);
                        smtpHostIndex = 0;
                        throw e;
                    }
                    sessionProps.put(JAVA_MAIL_SMTP_HOST_KEY, getSmtpHost());
                    log.info("Trying to send mail with new SMTP host: " + getSmtpHost());
                }
            }
            log.debug("Mail sent.");
        } catch (AddressException e) {
            String msg = "Cannot handle to-, from-, or replyTo-address. Mail not sent. To-address: " + toAddresses + "; BCC-address: " + bccAddresses +
                "; From-address: " + fromAddress + "; ReplyTo-address: " + replyToAddresses;
            log.error(msg, e);
            throw new MailException(msg, e);
        } catch (MessagingException e) {
            String msg = "Cannot send Mail; general error.";
            log.error(msg, e);
            throw new MailException(msg, e);
        }
    }


    /**
     * Clears instance variables.
     */
    public void clear() {
        this.fromAddress = null;
        this.toAddresses = null;
        this.bccAddresses = null;
        this.replyToAddresses = null;
        this.subject = null;
        this.body = null;
        this.attachments = null;
    }


    public void addAttachment(MailAttachment attachment) {
        if (attachments == null) {
            attachments = new ArrayList<MailAttachment>();
        }
        attachments.add(attachment);
    }


    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }


    public void setToAddresses(String toAddresses) {
        this.toAddresses = toAddresses;
    }

    
    public void setBccAddresses(String bccAddresses) {
        this.bccAddresses = bccAddresses;
    }


    public void setReplyToAddresses(String replyToAddresses) {
        this.replyToAddresses = replyToAddresses;
    }


    public void setSubject(String subject) {
        this.subject = subject;
    }


    public void setBody(String body) {
        this.body = body;
    }


    public void setAttachments(List<MailAttachment> attachments) {
        this.attachments = attachments;
    }


    @SuppressWarnings("null")
    private InternetAddress[] parseAddresses(String addressesStr) throws AddressException {
        if (addressesStr == null) {
            throw new AddressException("Address is not assigned.");
        }

        String[] addresses = addressesStr.trim().split(";|,");
        if ((addresses == null) || (addresses.length == 0)) {
            new AddressException("Address is missing.");
        }

        List<InternetAddress> resultList = new ArrayList<InternetAddress>(addresses.length);
        for (int i = 0; i < addresses.length; i++) {
            String address = addresses[i].trim();
            if (address.length() > 0) {
                resultList.add(new InternetAddress(address));
            }
        }

        if (resultList.size() == 0) {
            new AddressException("Address is missing; result list is empty.");
        }

        return resultList.toArray(new InternetAddress[resultList.size()]);
    }


    private String getSmtpHost() {
        if ((smtpHosts == null) || (smtpHostIndex >= smtpHosts.length)) {
            return null;
        }

        return smtpHosts[smtpHostIndex];
    }

}
