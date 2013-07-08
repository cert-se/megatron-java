package se.sitic.megatron.util;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;


/**
 * JUnit test.
 */
public class FileUtilTest {
    private static final String TMP_DIR = "tmp-junit";
    
        
    @Test
    public void characterEncoding() throws Exception {
        File tmpDir = new File(TMP_DIR);
        tmpDir.mkdir();
        
        String charSet = Constants.ISO8859;
        String writeContent = "Test Line: \u00e5\u00e4\u00f6\u00c5\u00c4\u00d6X";

        File file = new File(tmpDir, "test-" + charSet + ".txt");
        FileUtil.writeFile(file, writeContent, charSet);
        String readContent = FileUtil.readFile(file, charSet);
        readContent = StringUtil.removeLineBreaks(readContent, "");
        Assert.assertEquals(writeContent, readContent);
    }
    
}
