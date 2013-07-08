package se.sitic.megatron.rss;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.rss.rome.RomeRssFactory;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.DateUtil;
import se.sitic.megatron.util.FileUtil;


/**
 * Main class for handling RSS feeds.<p>
 *
 * Parses, builds, and writes RSS feeds, as well as Atom feeds.<p>
 *
 * The rss package is designed to support multiple RSS class libraries. The
 * actual implementation is located in a rrs sub-package, e.g.
 * 'se.sitic.megatron.rss.rome' which uses Rome. Client code should never use
 * the implementation directly, only classes in this package.
 */
public class RssManager {
    private static final Logger log = Logger.getLogger(RssManager.class);

    private TypedProperties props;
    private IRssFactory cachedFactory;


    /**
     * Constructor.
     */
    public RssManager(TypedProperties props) {
        this.props = props;
    }


    /**
     * Factory for IRssFactory objects.
     */
    public IRssFactory createRssFactory() {
        IRssFactory result = null;
        String className = props.getString(AppProperties.RSS_FACTORY_CLASS_NAME_KEY, RomeRssFactory.class.getName());
        try {
            Class<?> c = Class.forName(className);
            Constructor<?> constructor = c.getConstructor(TypedProperties.class);
            result = (IRssFactory)constructor.newInstance(props);
        } catch (Exception e) {
            // ClassNotFoundException, InstantiationException, IllegalAccessException
            String msg = "Cannot instantiate RSS factory: '" + className + "'. Using default factory.";
            log.error(msg, e);
            result = new RomeRssFactory(props);
        }

        return result;
    }


    /**
     * Parses specified RSS feed.
     *
     * @param file file to read.
     * @param createEmptyChannelIfFileMissing if true, an empty IRssChannel
     *      object is created if file not found. If false and file is not
     *      found, an FileNotFoundException is thrown.
     */
    public IRssChannel readRss(File file, boolean createEmptyChannelIfFileMissing) throws RssException, IOException {
        IRssChannel result = null;

        if (!file.canRead() && createEmptyChannelIfFileMissing) {
            log.info("Creating an empty RSS feed; cannot read RSS file: " + file.getAbsolutePath());
            result = getFactory().createRssChannel();
        } else {
            IRssParser rssParser = getFactory().createRssParser();
            InputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(file));
                result = rssParser.parseRss(in, null);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }

        return result;
    }


    /**
     * Creates an IRssItem-object using the internal factory.
     */
    public IRssItem createRssItem(IRssChannel parentChannel) {
        return getFactory().createRssItem(parentChannel);
    }


    /**
     * Writes specified RSS channel to file in UTF-8 format.
     *
     * @param file file to write. If parent directory does not exist, it's created.
     */
    public void writeRss(File file, IRssChannel rssChannel) throws RssException, IOException {
        // -- Ensure parent dir
        File dir = file.getParentFile();
        if ((dir != null) && !dir.isDirectory()) {
            log.info("Creating missing parent directory for RSS file: " + file.getAbsolutePath());
            FileUtil.ensureDir(dir);
        }

        // -- Write file
        IRssWriter rssWriter = getFactory().createRssWriter();
        Writer out = null;
        try {
            log.debug("Writing RSS file: " + file.getAbsolutePath());
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Constants.UTF8));
            rssWriter.writeRss(out, rssChannel);
        } catch (UnsupportedEncodingException e) {
            String msg = "Cannot write RSS feed; UTF-8 not supported (should never happen).";
            log.error(msg, e);
            throw new IOException(msg);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }


    /**
     * Removes items from list whose publication time is later than specified
     * expire time.
     *
     * @param rssItems list of items sorted by time (newest first).
     */
    public void removeExpiredItems(List<IRssItem> rssItems, int expireTimeInMinutes) {
        if (expireTimeInMinutes <= 0) {
            return;
        }

        Date now = new Date();

        // -- Find first expired item
        int index = 0;
        int hitIndex = -1;
        for (Iterator<IRssItem> iterator = rssItems.iterator(); iterator.hasNext(); ) {
            Date publicationDate = iterator.next().getPublicationDate();
            if (publicationDate != null) {
                // Is item expired?
                if ((publicationDate.getTime() + 60*1000L*expireTimeInMinutes) < now.getTime()) {
                    if (log.isDebugEnabled()) {
                        String timeStr = DateUtil.formatDateTime(DateUtil.DATE_TIME_FORMAT_WITH_T_CHAR_AND_TZ, publicationDate);
                        log.debug("Found first expired RSS item; publication date: " + timeStr);
                    }
                    hitIndex = index;
                    break;
                }
            }
            ++index;
        }

        // -- Remove from found item
        if (hitIndex != -1) {
            log.debug("Expired items found in RSS. Removing " + (rssItems.size() - hitIndex) + " items.");
            rssItems.subList(hitIndex, rssItems.size()).clear();
        }
    }


    /**
     * Removes items in specified list that is above the max limit.
     *
     * @param rssItems list of items sorted by time (newest first).
     */
    public void removeItemsAboveMaxLimit(List<IRssItem> rssItems, int maxNoOfItems) {
        if ((rssItems.size() <= maxNoOfItems) || (maxNoOfItems <= 0)) {
            return;
        }

        log.debug("Max limit reached for RSS. Removing " + (rssItems.size() - maxNoOfItems) + " items.");
        rssItems.subList(maxNoOfItems, rssItems.size()).clear();
    }


    private IRssFactory getFactory() {
        if (cachedFactory == null) {
            cachedFactory = createRssFactory();
        }

        return cachedFactory;
    }

}
