package se.sitic.megatron.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * JUnit 4 test-case.
 */
public class StringUtilTest {


    public StringUtilTest() {
        // empty
    }


    @Test
    public void removePrefixTest() {
        assertEquals("config", StringUtil.removePrefix("foobar.config", "foobar."));
        assertEquals("foobar.config", StringUtil.removePrefix("foobar.config", "bar."));
        assertEquals("foobar.config", StringUtil.removePrefix("foobar.config", "config"));
        assertEquals("", StringUtil.removePrefix("foobar.config", "foobar.config"));
        assertEquals("X", StringUtil.removePrefix("foobar.configX", "foobar.config"));
        assertEquals(null, StringUtil.removePrefix(null, "foobar.config"));
    }

    
    @Test
    public void removeSuffixTest() {
        assertEquals("foobar", StringUtil.removeSuffix("foobar.config", ".config"));
        assertEquals("foobar.config", StringUtil.removeSuffix("foobar.config", "bar."));
        assertEquals("foobar.config", StringUtil.removeSuffix("foobar.config", "foobar"));
        assertEquals("", StringUtil.removeSuffix("foobar.config", "foobar.config"));
        assertEquals("X", StringUtil.removeSuffix("Xfoobar.config", "foobar.config"));
        assertEquals(null, StringUtil.removeSuffix(null, "foobar.config"));
    }

    
    @Test
    public void removeEnclosingCharsTest() {
        final String[][] testArray = {
                { null, null },
                { "", "" },
                { "x", "x" },
                { "'foo'", "foo" },
                { "'foo", "'foo" },
                { "foo'", "foo'" },
                { "''foo''", "'foo'" },
                { "''", "" },
        };

        String enclosingChars = "'";
        for (int i = 0; i < testArray.length; i++) {
            String str = testArray[i][0];
            String expected = testArray[i][1];
            assertEquals(expected, StringUtil.removeEnclosingChars(str, enclosingChars));
        }

        assertEquals("foobar", StringUtil.removeEnclosingChars("##foobar##", "##"));
    }
    
    
    @Test
    public void replaceTest() {
        assertEquals("name=hubba", StringUtil.replace("name=@name@", "@name@", "hubba"));
        assertEquals("X X-bubba X", StringUtil.replace("hubba hubba-bubba hubba", "hubba", "X"));
        assertEquals("X hubba-bubba hubba", StringUtil.replaceFirst("hubba hubba-bubba hubba", "hubba", "X"));
        assertEquals("hubba", StringUtil.replaceFirst("hubba", "Hubba", "X"));
        assertEquals("hubba", StringUtil.replace("hubba", "Hubba", "X"));
        assertEquals("", StringUtil.replaceFirst("", "Hubba", "X"));
        assertEquals("", StringUtil.replace("", "Hubba", "X"));
    }

}
