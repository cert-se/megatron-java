package se.sitic.megatron.mail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Maps an URL or filename to a MIME-type.
 */
public class MimeMapper {
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final String DEFAULT_EXTENSION = "bin";

    private static MimeMapper singleton;

    /** Key: extension (e.g. "txt"). Value: MIME type (e.g. "text/plain")*/
    private Map<String, String> extensionMimeTypeMap;


    /**
     * Constructor.
     */
    private MimeMapper() {
        init();
    }


    /**
     * Returns singleton.
     */
    public synchronized static MimeMapper getInstance() {
        if (singleton == null) {
            singleton = new MimeMapper();
        }
        return singleton;
    }


    /**
     * Maps specified url to an MIME-type, e.g "http://www.example.com/pdfs/foo.pdf"
     * returns "application/pdf".
     */
    public String mapUrl(String url) {
        String result = null;
        String extension = mapExtension(url);
        if ((extension != null) && extensionMimeTypeMap.containsKey(extension)) {
            result = extensionMimeTypeMap.get(extension);
        } else {
            result = DEFAULT_MIME_TYPE;
        }

        return result;
    }


    /**
     * @see #mapUrl(String)
     */
    public String mapFilename(File file) {
        return mapUrl(file.getAbsolutePath());
    }


    /**
     * Returns extension for specified url, e.g "http://www.example.com/pdfs/foo.pdf"
     * returns "pdf".
     *
     * @return extension, or "bin" if no match.
     */
    public String mapExtension(String url) {
        String result = null;
        int hitIndex = url.lastIndexOf('.');
        if ((hitIndex != -1) && ((hitIndex + 1) < url.length()) && (hitIndex > url.lastIndexOf('/'))) {
            result = url.substring(hitIndex + 1);
        }

        return ((result != null) && result.trim().length() > 0) ? result : DEFAULT_EXTENSION;
    }


    /**
     * @see #mapExtension(String)
     */
    public String mapExtension(File file) {
        return mapExtension(file.getAbsolutePath());
    }


    private void init() {
        extensionMimeTypeMap = new HashMap<String, String>();

        // TODO Read MIME-types from config file.
        extensionMimeTypeMap.put("txt", "text/plain");
        extensionMimeTypeMap.put("pdf", "application/pdf");
        extensionMimeTypeMap.put("mp3", "audio/mpeg");
        extensionMimeTypeMap.put("zip", "application/zip");
        extensionMimeTypeMap.put("pgp", "application/pgp-encrypted");
        extensionMimeTypeMap.put("p7m", "application/pkcs7-mime");
        extensionMimeTypeMap.put("bin", "application/octet-stream");
    }

}
