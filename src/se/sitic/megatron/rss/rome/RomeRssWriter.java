package se.sitic.megatron.rss.rome;

import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.rss.IRssChannel;
import se.sitic.megatron.rss.IRssWriter;
import se.sitic.megatron.rss.RssException;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;


/**
 * Implements IRssWriter using Rome.
 */
public class RomeRssWriter implements IRssWriter {
    private static final Logger log = Logger.getLogger(RomeRssWriter.class);

    // UNUSED: private TypedProperties props;


    /**
     * Constructor.
     */
    public RomeRssWriter(TypedProperties props) {
        // UNUSED: this.props = props;
    }


    @Override
    public void writeRss(Writer out, IRssChannel rssChannel) throws RssException, IOException {
        SyndFeed syndFeed = ((RomeRssChannel)rssChannel).getSyndFeed();
        SyndFeedOutput syndFeedOutput = new SyndFeedOutput();
        try {
            syndFeedOutput.output(syndFeed, out);
        } catch (FeedException e) {
            String msg = "Cannot write RSS feed.";
            log.error(msg, e);
            throw new RssException(msg, e);
        }
    }

}
