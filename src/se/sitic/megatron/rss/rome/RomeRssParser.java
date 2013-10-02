package se.sitic.megatron.rss.rome;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.rss.IRssChannel;
import se.sitic.megatron.rss.IRssParser;
import se.sitic.megatron.rss.RssParseException;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;


/**
 * Implements IRssParser using Rome.
 */
public class RomeRssParser implements IRssParser {
    private static final Logger log = Logger.getLogger(RomeRssParser.class);

    private TypedProperties props;


    /**
     * Constructor.
     */
    public RomeRssParser(TypedProperties props) {
        this.props = props;
    }


    @Override
    public IRssChannel parseRss(InputStream in, String encoding) throws RssParseException, IOException {
        try {
            Reader reader = null;
            if (encoding == null) {
                // Let Rome figure out the encoding.
                reader = new XmlReader(in);
            } else {
                // Specify encoding hard.
                reader = new InputStreamReader(in, encoding);
            }

            SyndFeedInput syndFeedInput = new SyndFeedInput();
            SyndFeed syndFeed = syndFeedInput.build(reader);

            return new RomeRssChannel(props, syndFeed);
        } catch (FeedException e) {
            String msg = "Cannot parse RSS feed.";
            log.error(msg, e);
            throw new RssParseException(msg, e);
        }
    }

}
