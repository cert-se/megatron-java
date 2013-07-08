package se.sitic.megatron.rss;

import java.io.IOException;
import java.io.InputStream;


/**
 * Parser for RSS feeds.
 */
public interface IRssParser {

    /**
     * Parses specified RSS feed.
     *
     * @param in input stream to parse.
     * @param encoding encoding for stream. May be null.
     *
     * @return parsed RSS feed.
     */
    public IRssChannel parseRss(InputStream in, String encoding) throws RssParseException, IOException;

}
