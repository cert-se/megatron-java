package se.sitic.megatron.fileprocessor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.JobContext;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.FileUtil;
import se.sitic.megatron.util.StringUtil;


/**
 * Converts an XML-file using a SAX-parser to a row oriented file.
 */
public class XmlToRowFileProcessor implements IFileProcessor {
    protected static final Logger log = Logger.getLogger(XmlToRowFileProcessor.class);

    private static final String TMP_PREFIX = "megatron-XmlToRowFileProcessor-";
    private static final String TMP_SUFFIX = ".txt";

// Example:
//    protected String startElement = "entry";
//    protected String[] elementsToSave = { "id", "first", "last", "md5", "virusname", "url", "recent", "response", "ip", "as", "review", "domain", "country", "email", "inetnum", "netname", "descr", "ns1", "ns2" };
    protected String startElement;
    protected String[] elementsToSave;
    protected String separator;
    protected boolean deleteOutputFile;
    protected String charSet;
    protected File tmpDir;
    protected BufferedWriter out;

    
    public XmlToRowFileProcessor() {
        // empty
    }

    
    public void init(JobContext jobContext) throws MegatronException {
        TypedProperties props = jobContext.getProps();
        
        // read props
        startElement = props.getString(AppProperties.FILE_PROCESSOR_XML_TO_ROW_START_ELEMENT_KEY, null);
        elementsToSave = props.getStringListFromCommaSeparatedValue(AppProperties.FILE_PROCESSOR_XML_TO_ROW_ELEMENTS_TO_SAVE_KEY, null, true);
        separator = props.getString(AppProperties.FILE_PROCESSOR_XML_TO_ROW_OUTPUT_SEPARATOR_KEY, "\\t");
        separator = StringUtil.replace(separator, "\\t", "\t");
        if (props.existsProperty(AppProperties.FILE_PROCESSOR_DELETE_TMP_FILES_KEY)) {
            deleteOutputFile = props.getBoolean(AppProperties.FILE_PROCESSOR_DELETE_TMP_FILES_KEY, true);
        } else {
            // read deprecated property 
            deleteOutputFile = props.getBoolean(AppProperties.FILE_PROCESSOR_XML_TO_ROW_DELETE_OUTPUT_FILE_KEY, true);
        }
        charSet = props.getString(AppProperties.INPUT_CHAR_SET_KEY, Constants.UTF8);

        // validate props
        if (StringUtil.isNullOrEmpty(startElement)) {
            throw new MegatronException("Mandatory property undefined: " + AppProperties.FILE_PROCESSOR_XML_TO_ROW_START_ELEMENT_KEY);
        }
        if ((elementsToSave == null) || (elementsToSave.length == 0)) {
            throw new MegatronException("Mandatory property undefined: " + AppProperties.FILE_PROCESSOR_XML_TO_ROW_ELEMENTS_TO_SAVE_KEY);
        }
        
        // convert element names to lower-case
        startElement = startElement.toLowerCase();
        for (int i = 0; i < elementsToSave.length; i++) {
            elementsToSave[i] = elementsToSave[i].toLowerCase();
        }

        // setup tmp-dir
        tmpDir = new File(props.getString(AppProperties.TMP_DIR_KEY, "tmp"));
        try {
            FileUtil.ensureDir(tmpDir);
        } catch (IOException e) {
            String msg = "Cannot create directory for temporary files: " + tmpDir.getAbsolutePath();
            throw new MegatronException(msg, e);
        }
    } 
    

    public File execute(File inputFile) throws MegatronException {
        File result = createTemporaryFile(TMP_PREFIX);

        SAXParser saxParser = null;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            saxParser = factory.newSAXParser();
        } catch (Exception e) {
            // ParserConfigurationException, SAXException
            throw new MegatronException("Cannot initialize XML-parser.", e);
        }
        
        try {
            log.debug("Creating temporary file to write flattened XML-records to: " + result.getAbsolutePath());
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(result), charSet));
        } catch (Exception e) {
            // UnsupportedEncodingException, FileNotFoundException
            throw new MegatronException("Cannot create temporary file: " + result.getAbsolutePath(), e);
        }
        
        SaxHandler handler = createHandler();
        Reader reader = null;
        try {
            reader = new StripInvalidXmlCharactersReader(new InputStreamReader(new FileInputStream(inputFile), charSet));
            saxParser.parse(new InputSource(reader), handler);
        } catch (SAXException e) {
            throw new MegatronException("Parse error around line " + handler.getLineNumber() + " in XML-file: " + inputFile.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new MegatronException("Cannot read XML-file: " + inputFile.getAbsolutePath(), e);
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception ignored) { }
            try { out.close(); } catch (Exception ignored) { }
        }
        
        log.info("XML-file with " + handler.getLineNumber() + " lines flattened to text file with " + handler.getNoOfSavedLines() + " lines. Output filename: " + result.getAbsolutePath());
        
        return result;
    }

    
    public void close(boolean jobSuccessful) throws MegatronException {
        // empty
    }
    
    
    protected File createTemporaryFile(String prefix) throws MegatronException {
        File result = null;
        try {
            result = File.createTempFile(prefix, TMP_SUFFIX, tmpDir);
            if (deleteOutputFile) {
                result.deleteOnExit();
            }
        } catch (IOException e) {
            String msg = "Cannot create temporary file.";
            throw new MegatronException(msg, e);
        }
        return result;
    }
    
    
    protected SaxHandler createHandler() {
        return new SaxHandler();
    }

    
    /**
     * Strip invalid XML-characters from a reader.
     */
    protected class StripInvalidXmlCharactersReader extends Reader {
        private Reader in;

        
        public StripInvalidXmlCharactersReader(Reader in) {
            this.in = in;
        }
        
        
        @Override
        public boolean markSupported() {
            return false;
        }

        
        @Override
        public void mark(int readLimit) {
            throw new UnsupportedOperationException("Mark not supported.");
        }


        @Override
        public void reset() {
            throw new UnsupportedOperationException("Reset not supported.");
        }

        
        @Override
        public int read() throws IOException {
            int next;
            do {
                next = in.read();
            } while(!((next == -1) || isValidXmlCharacter(next)));

            return next; 
        }

        
        @Override
        public void close() throws IOException {
            in.close();
        }

        
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int result = 0;
            int next = 0;
            for (result = 0; result < len; result++) {
                next = read();
                if (next == -1) {
                    break;
                }
                cbuf[off + result] = (char)next;
            }
            if ((result == 0) && (next == -1)) {
                return -1;
            }
            return result;
        }

        
        @Override
        public int read(char[] cbuf) throws IOException {
            return read(cbuf, 0, cbuf.length);
        }
        
        
        protected boolean isValidXmlCharacter(int ch) { 
            if ((ch == 0x9) || (ch == 0xA) || (ch == 0xD) || ((ch >= 0x20) && (ch <= 0xD7FF)) || ((ch >= 0xE000) && (ch <= 0xFFFD)) || ((ch >= 0x10000) && (ch <= 0x10FFFF))) {
                return true;
            }
            log.info("XML-document contains invalid character: " + ch + " (decimal value). Character is ignored.");
            return false;
        }
    
    }

    
    /**
     * Implements a SAX2 event handler.
     */
    protected class SaxHandler extends DefaultHandler {
        private Set<String> elementsToSaveLookup;
        private Locator locator;
        private int lineNumber; 
        private int noOfSavedLines;
        private String currentElement;
        private Map<String, String> elementValues;
        private Set<String> closedElements;
        

        public SaxHandler() {
            this.elementsToSaveLookup = new HashSet<String>(Arrays.asList(elementsToSave));
            this.elementValues = new HashMap<String, String>();
            this.closedElements = new HashSet<String>();
        }

        
        public int getLineNumber() {
            return this.lineNumber;
        }

        
        public int getNoOfSavedLines() {
            return this.noOfSavedLines;
        }

        
        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }
        

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            lineNumber = locator.getLineNumber();
            currentElement = qName.toLowerCase();
        }
        
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (startElement.equalsIgnoreCase(qName)) {
                String line = createLine();
                elementValues.clear();
                closedElements.clear();
                try {
                    writeToFile(line);
                } catch (IOException e) {
                    throw new SAXException("Cannot write line to output text file.", e);
                }
            } else if ((currentElement != null) && currentElement.equalsIgnoreCase(qName)) {
                closedElements.add(qName.toLowerCase());
            }
            currentElement = null;
        }
        
        
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if ((currentElement != null) && (elementsToSaveLookup.contains(currentElement))) {
                // This method may be called several times for the same element
                String newValue = null;
                String prevValue = elementValues.get(currentElement);
                if (prevValue != null) {
                    if (closedElements.contains(currentElement)) {
                        log.info("Duplicates of the XML-element '" + currentElement + "' exists. Ignoring previous value: " + prevValue);
                        newValue = new String(ch, start, length);
                    } else {
                        StringBuilder sb = new StringBuilder(prevValue.length() + length + 16);
                        sb.append(prevValue).append(ch, start, length);
                        newValue = sb.toString();
                    }
                } else {
                    newValue = new String(ch, start, length);
                }
                elementValues.put(currentElement, newValue);
            }
        }
        
        
        private String createLine() {
            StringBuilder result = new StringBuilder(512);
            for (int i = 0; i < elementsToSave.length; i++) {
                String value = elementValues.get(elementsToSave[i]);
                value = (value != null) ? value : "";
                if (result.length() > 0) {
                    result.append(separator);
                }
                result.append(value);
            }
            return result.toString();
        }
        
        
        private void writeToFile(String line) throws IOException {
            line = StringUtil.removeLineBreaks(line, "");
            out.write(line);
            out.write(Constants.LINE_BREAK);
            ++noOfSavedLines;
        }
        
    }

}
