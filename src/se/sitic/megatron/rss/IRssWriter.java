package se.sitic.megatron.rss;

import java.io.IOException;
import java.io.Writer;


/**
 * Writer for RSS feeds.
 */
public interface IRssWriter {

    /**
     * Saves specified RSS feed.
     *
     * @param rssChannel feed to write.
     */
    public void writeRss(Writer out, IRssChannel rssChannel) throws RssException, IOException;

}
