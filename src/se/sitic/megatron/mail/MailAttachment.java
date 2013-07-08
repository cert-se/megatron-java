package se.sitic.megatron.mail;

import java.io.File;
import java.io.IOException;

import se.sitic.megatron.util.FileUtil;


/**
 * Entity class for an email attachment.
 */
public class MailAttachment {
    private byte[] content;
    private File file;
    private String mimeType;
    private String attachmentName;


    /**
     * Constructor.
     *
     * @param content content to attach.
     * @param mimeType MIME-type for content.
     * @param attachmentName filename for attachment in the mail.
     */
    public MailAttachment(byte[] content, String mimeType, String attachmentName) {
        this.content = content;
        this.mimeType = mimeType;
        this.attachmentName = attachmentName;
    }


    /**
     * Constructor.
     *
     * @param file file to attach.
     * @param attachmentName filename for attachment in the mail.
     */
    public MailAttachment(File file, String attachmentName) {
        this.file = file;
        this.attachmentName = attachmentName;
        this.mimeType = MimeMapper.getInstance().mapFilename(file);
    }


    /**
     * Constructor.
     *
     * @param file file to attach.
     */
    public MailAttachment(File file) {
        this(file, file.getName());
    }


    public String getAttachmentName() {
        return attachmentName;
    }


    public byte[] getContent() throws IOException {
        byte[] attachmentContent = null;
        if (file != null) {
            attachmentContent = FileUtil.getBytesFromFile(file);
        } else {
            attachmentContent = content;
        }

        return attachmentContent;
    }


    public String getMimeType() {
        return mimeType;
    }

}
