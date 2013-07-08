package se.sitic.megatron.rss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.util.FileUtil;


/**
 * JUnit 4 test-case.<p>
 *
 * Will create a couple of RSS files in different formats.
 */
public class RssManagerTest {
    private static final String OUTPUT_DIR = "tmp/rss";
    private static final String RSS_FILE_EXT= ".xml";
    private static final String LOG4J_FILENAME = "conf/dev/log4j.properties";

    private TypedProperties props;


    /**
     * Constructor.
     */
    public RssManagerTest() {
        // empty
    }


    @Before
    public void init() throws Exception {
        PropertyConfigurator.configure(LOG4J_FILENAME);

        Map<String, String> propMap = new HashMap<String, String>();
        props = new TypedProperties(propMap, null);

        // create output dir
        FileUtil.ensureDir(OUTPUT_DIR);
        // delete old files
//        File outputDir = new File(OUTPUT_DIR);
//        File[] filesToDelete = outputDir.listFiles(new FilenameFilter() {
//                public boolean accept(File dir, String name) {
//                    return name.endsWith(RSS_FILE_EXT);
//                }
//            });
//        for (int i = 0; i < filesToDelete.length; i++) {
//            File file = filesToDelete[i];
//            if (!file.delete()) {
//                throw new IOException("Cannot delete file: " + file.getAbsolutePath());
//            }
//        }
    }


    @Test
    public void createAndParseMinimalRss() throws Exception {
        RssManager rssManager = new RssManager(props);
        IRssFactory rssFactory = rssManager.createRssFactory();
        IRssChannel rssChannel = rssFactory.createRssChannel();
        for (Iterator<String> iterator = rssChannel.getSupportedRssFormats().iterator(); iterator.hasNext(); ) {
            createAndParseMinimalRss(iterator.next());
        }
    }


    @Test
    public void createAndParseIntermediateRss() throws Exception {
        RssManager rssManager = new RssManager(props);
        IRssFactory rssFactory = rssManager.createRssFactory();
        IRssChannel rssChannel = rssFactory.createRssChannel();
        for (Iterator<String> iterator = rssChannel.getSupportedRssFormats().iterator(); iterator.hasNext(); ) {
            createAndParseIntermediateRss(iterator.next());
        }
    }


    private void createAndParseMinimalRss(String rssFormat) throws Exception {
        if (rssFormat.equals("rss_0.9") || rssFormat.equals("rss_0.91N") || rssFormat.equals("rss_0.91U")) {
            // skip; require items
            return;
        }

        final String filename = OUTPUT_DIR + "/minimal-" + rssFormat + RSS_FILE_EXT;
        final String title = "Test Title";
        final String description = "Test Description";
        final String channelLink = "http://www.example.com/rss-foo.xml";

        RssManager rssManager = new RssManager(props);
        IRssFactory rssFactory = rssManager.createRssFactory();
        IRssChannel rssChannel = rssFactory.createRssChannel();

        // assign
        rssChannel.setRssFormat(rssFormat);
        rssChannel.setTitle(title);
        rssChannel.setDescription(description);
        rssChannel.setLinks(Collections.singletonList(channelLink));

        // write
//        IRssWriter rssWriter = rssFactory.createRssWriter();
//        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), Constants.UTF8));
//        rssWriter.writeRss(out, rssChannel);
//        out.close();
        rssManager.writeRss(new File(filename), rssChannel);

        // read
//        IRssParser rssParser = rssFactory.createRssParser();
//        InputStream in = new BufferedInputStream(new FileInputStream(filename));
//        rssChannel = rssParser.parseRss(in, null);
        rssManager.readRss(new File(filename), false);

        // check empty channel
        assertNotNull(rssChannel);
        assertEquals(title, rssChannel.getTitle());
        assertEquals(description, rssChannel.getDescription());
        assertEquals(Collections.singletonList(channelLink), rssChannel.getLinks());
        assertEquals(0, rssChannel.getItems().size());
    }


    private void createAndParseIntermediateRss(String rssFormat) throws Exception {
        if (rssFormat.equals("rss_0.9") || rssFormat.equals("rss_0.91N") || rssFormat.equals("rss_0.91U")) {
            // skip; require items
            return;
        }

        // DEBUG: System.out.println("rssFormat=" + rssFormat);

        // Current time without ms part
        final long now = 1000L * (System.currentTimeMillis() / 1000L);

        final String filename = OUTPUT_DIR + "/intermediate-" + rssFormat + RSS_FILE_EXT;
        final String title = "Test Title. Swedish characters: Â‰ˆ≈ƒ÷";
        final String description = "Test Description. Swedish characters: Â‰ˆ≈ƒ÷";
        final String[] channelLinks = { "http://www.example.com/rss-foo.xml" };
        final String[] categories = { "Category 1", "Category 2" };
        final String channelCopyright = "Channel Copyright";
        final Date publicationDate = new Date(now - 10*24*60*60*1000L);
        final String[] singleChannelAuthor = { "Channel Author" };
        final String[] multipleChannelAuthors = { "Channel Author 1", "Channel Author 2" };
        final String[] channelAuthors = rssFormat.equals("atom_1.0") ? multipleChannelAuthors : singleChannelAuthor;

        final String item1Title = "Item 1 Title";
        final String item1Description = "Description for item 1<br>New line.";
        final Date item1PublicationDate = new Date(now - 2*60*60*1000L);
        final Date item1UpdatedDate = new Date(now - 1*60*60*1000L);
        final String[] item1Categories = { "Item 1 Category 1", "Item 1 Category 2" };
        final String[] item1SingleAuthor = { "Item 1 Author" };
        final String[] item1MultipleAuthors = { "Item 1 Author 1", "Item 1 Author 2" };
        final String[] item1Authors =  rssFormat.equals("atom_1.0") ? item1MultipleAuthors : item1SingleAuthor;
        final String[] item1Links = { "http://www.example.com/rss-foo-item1.xml" };

        final String item2Title = "Item 2 Title";
        final String item2Description = "Description for item 2.";
        final Date item2PublicationDate = new Date(now - 4*60*60*1000L);
        final Date item2UpdatedDate = new Date(now - 2*60*60*1000L);
        final String[] item2Authors = { "Item 2 Author" };
        final String[] item2Links = { "http://www.example.com/rss-foo-item2.xml" };

        final String item3Title = "Item 3 Title";
        final String item3Description = "Description for item 3.";
        final Date item3PublicationDate = new Date(now - 5*60*60*1000L);
        final String[] item3Links = { "http://www.example.com/rss-foo-item3.xml" };

        final String item4Title = "Item 4 Title";
        final String item4Description = "Description for item 4.";
        final Date item4PublicationDate = new Date(now - 9*60*60*1000L);
        final String[] item4Links = { "http://www.example.com/rss-foo-item4.xml" };

        RssManager rssManager = new RssManager(props);
        IRssFactory rssFactory = rssManager.createRssFactory();
        IRssChannel rssChannel = rssFactory.createRssChannel();
        IRssItem item1 = rssFactory.createRssItem(rssChannel);
        IRssItem item2 = rssFactory.createRssItem(rssChannel);
        IRssItem item3 = rssFactory.createRssItem(rssChannel);
        IRssItem item4 = rssFactory.createRssItem(rssChannel);

        // assign
        rssChannel.setRssFormat(rssFormat);
        rssChannel.setTitle(title);
        rssChannel.setDescription(description);
        rssChannel.setLinks(Arrays.asList(channelLinks));
        rssChannel.setCategories(Arrays.asList(categories));
        rssChannel.setCopyright(channelCopyright);
        rssChannel.setPublicationDate(publicationDate);
        rssChannel.setAuthors(Arrays.asList(channelAuthors));

        item1.setTitle(item1Title);
        item1.setDescription(item1Description);
        item1.setPublicationDate(item1PublicationDate);
        item1.setUpdatedDate(item1UpdatedDate);
        item1.setCategories(Arrays.asList(item1Categories));
        item1.setAuthors(Arrays.asList(item1Authors));
        item1.setLinks(Arrays.asList(item1Links));

        item2.setTitle(item2Title);
        item2.setDescription(item2Description);
        item2.setPublicationDate(item2PublicationDate);
        item2.setUpdatedDate(item2UpdatedDate);
        item2.setAuthors(Arrays.asList(item2Authors));
        item2.setLinks(Arrays.asList(item2Links));

        item3.setTitle(item3Title);
        item3.setDescription(item3Description);
        item3.setPublicationDate(item3PublicationDate);
        item3.setLinks(Arrays.asList(item3Links));

        item4.setTitle(item4Title);
        item4.setDescription(item4Description);
        item4.setPublicationDate(item4PublicationDate);
        item4.setLinks(Arrays.asList(item4Links));

        List<IRssItem> items = new ArrayList<IRssItem>();
        items.add(item1);
        items.add(item2);
        items.add(item3);
        items.add(item4);
        rssChannel.setItems(items);

        // write
//        IRssWriter rssWriter = rssFactory.createRssWriter();
//        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), Constants.UTF8));
//        rssWriter.writeRss(out, rssChannel);
//        out.close();
        rssManager.writeRss(new File(filename), rssChannel);

        // read
//        IRssParser rssParser = rssFactory.createRssParser();
//        InputStream in = new BufferedInputStream(new FileInputStream(filename));
//        rssChannel = rssParser.parseRss(in, null);
        rssManager.readRss(new File(filename), false);

        // check channel
        assertNotNull(rssChannel);
        assertEquals(title, rssChannel.getTitle());
        assertEquals(description, rssChannel.getDescription());
        assertEquals(Arrays.asList(channelLinks), rssChannel.getLinks());
        if (!rssFormat.equals("rss_0.92") && !rssFormat.equals("rss_0.93") && !rssFormat.equals("rss_0.94")) {
            assertEquals(Arrays.asList(categories), rssChannel.getCategories());
        }
        assertEquals(channelCopyright, rssChannel.getCopyright());
        assertEquals(publicationDate, rssChannel.getPublicationDate());
        if (!rssFormat.equals("rss_0.92") && !rssFormat.equals("rss_0.93") && !rssFormat.equals("rss_0.94")) {
            assertEquals(Arrays.asList(channelAuthors), rssChannel.getAuthors());
        }

        items = rssChannel.getItems();
        item1 = items.get(0);
        item2 = items.get(1);
        item3 = items.get(2);
        item4 = items.get(3);

        // check item1
        assertEquals(item1Title, item1.getTitle());
        assertEquals(item1Description, item1.getDescription());
        if (!rssFormat.equals("rss_0.92")) {
            assertEquals(item1PublicationDate, item1.getPublicationDate());
        }
        if (!rssFormat.equals("rss_0.92") && !rssFormat.equals("rss_0.93") && !rssFormat.equals("rss_0.94") && !rssFormat.equals("rss_1.0") && !rssFormat.equals("rss_2.0") &&
                !rssFormat.equals("atom_0.3")) {
            assertEquals(item1UpdatedDate, item1.getUpdatedDate());
        }
        if (!rssFormat.equals("atom_0.3") && !rssFormat.equals("rss_1.0")) {
            assertEquals(Arrays.asList(item1Categories), item1.getCategories());
        }
        if (!rssFormat.equals("rss_0.92") && !rssFormat.equals("rss_0.93") && !rssFormat.equals("rss_0.94")) {
            assertEquals(Arrays.asList(item1Authors), item1.getAuthors());
        }
        assertEquals(Arrays.asList(item1Links), item1.getLinks());

        // check item2
        assertEquals(item2Title, item2.getTitle());
        assertEquals(item2Description, item2.getDescription());
        if (!rssFormat.equals("rss_0.92")) {
            assertEquals(item2PublicationDate, item2.getPublicationDate());
        }
        // Tested above: assertEquals(item2UpdatedDate, item2.getUpdatedDate());
        assertEquals(0, item2.getCategories().size());
        if (!rssFormat.equals("rss_0.92") && !rssFormat.equals("rss_0.93") && !rssFormat.equals("rss_0.94")) {
            assertEquals(Arrays.asList(item2Authors), item2.getAuthors());
        }
        assertEquals(Arrays.asList(item2Links), item2.getLinks());

        // check item3
        assertEquals(item3Title, item3.getTitle());
        assertEquals(item3Description, item3.getDescription());
        if (!rssFormat.equals("rss_0.92")) {
            assertEquals(item3PublicationDate, item3.getPublicationDate());
        }
        assertEquals(Arrays.asList(item3Links), item3.getLinks());

        // check item4
        assertEquals(item4Title, item4.getTitle());
        assertEquals(item4Description, item4.getDescription());
        if (!rssFormat.equals("rss_0.92")) {
            assertEquals(item4PublicationDate, item4.getPublicationDate());
        }
        assertEquals(Arrays.asList(item4Links), item4.getLinks());
    }


}
