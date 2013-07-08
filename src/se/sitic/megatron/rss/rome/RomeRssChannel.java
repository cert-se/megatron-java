package se.sitic.megatron.rss.rome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.rss.IRssChannel;
import se.sitic.megatron.rss.IRssItem;

import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.feed.synd.SyndPersonImpl;


/**
 * Implements IRssChannel using Rome.
 */
public class RomeRssChannel implements IRssChannel {
    private static final Logger log = Logger.getLogger(RomeRssChannel.class);

    private static final String[] SUPPORTED_FORMATS_ARRAY = { "rss_0.9", "rss_0.91N", "rss_0.91U", "rss_0.92", "rss_0.93", "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3", "atom_1.0" };
    private static final List<String> SUPPORTED_FORMATS = Collections.unmodifiableList(Arrays.asList(SUPPORTED_FORMATS_ARRAY));
    private static final String FORMAT_DEFAULT = "rss_2.0";
    private static final String LANGUAGE_DEFAULT = "en-us";

    private TypedProperties props;
    private SyndFeed syndFeed;


    /**
     * Constructs this feed from specified parsed feed object.
     */
    public RomeRssChannel(TypedProperties props, SyndFeed syndFeed) {
        this.props = props;
        this.syndFeed = syndFeed;
    }


    /**
     * Constructs an empty feed.
     */
    public RomeRssChannel(TypedProperties props) {
        this(props, new SyndFeedImpl());

        // -- Init
        String rssFormat = props.getString(AppProperties.RSS_FORMAT_KEY, FORMAT_DEFAULT);
        setRssFormat(rssFormat);
    }


    /**
     * Returns wrapped SyndFeed object.
     */
    public SyndFeed getSyndFeed() {
        return this.syndFeed;
    }


    public List<IRssItem> getItems() {
        List<IRssItem> result = new ArrayList<IRssItem>(syndFeed.getEntries().size());

        for (Iterator<?> iterator = syndFeed.getEntries().iterator(); iterator.hasNext(); ) {
            IRssItem rssItem = new RomeRssItem(props, this, (SyndEntry)iterator.next());
            result.add(rssItem);
        }

        return result;
    }


    public void setItems(List<IRssItem> items) {
        if (items == null) {
            syndFeed.setEntries(null);
            return;
        }

        List<SyndEntry> syndEntries = new ArrayList<SyndEntry>(items.size());
        for (Iterator<IRssItem> iterator = items.iterator(); iterator.hasNext(); ) {
            RomeRssItem rssItem = (RomeRssItem)iterator.next();
            syndEntries.add(rssItem.getSyndEntry());
        }

        syndFeed.setEntries(syndEntries);
    }


    public boolean removeItem(IRssItem item) {
        if (item == null) {
            return false;
        }

        List<IRssItem> items = getItems();
        boolean result = items.remove(item);
        setItems(items);
        return result;
    }


    public String getTitle() {
        return syndFeed.getTitle();
    }


    public void setTitle(String title) {
        syndFeed.setTitle(title);
    }


    public String getDescription() {
        return syndFeed.getDescription();
    }


    public void setDescription(String description) {
        syndFeed.setDescription(description);
    }


    public List<String> getLinks() {
        // Rome does not support multiple links.
        // SyndFeed.setLinks sets the *entry* links.

        List<String> result = new ArrayList<String>();
        if (syndFeed.getLink() != null) {
            result.add(syndFeed.getLink());
        }

        return result;
    }


    public void setLinks(List<String> links) {
        // Rome does not support multiple links.
        // SyndFeed.setLinks sets the *entry* links.

        if ((links == null) || (links.size() == 0)) {
            syndFeed.setLink(null);
        } else {
            if (links.size() > 1) {
                log.warn("Rome does not support multiple feed links. Using the first link.");
            }
            syndFeed.setLink(links.get(0));
        }
    }


    public List<String> getCategories() {
        List<String> result = new ArrayList<String>();
        List<?> categories = syndFeed.getCategories();
        if (categories != null) {
            for (Iterator<?> iterator = categories.iterator(); iterator.hasNext(); ) {
                SyndCategoryImpl syndCategory = (SyndCategoryImpl)iterator.next();
                result.add(syndCategory.getName());
            }
        }
        return result;
    }


    public void setCategories(List<String> categories) {
        if (categories == null) {
            syndFeed.setCategories(null);
            return;
        }

        List<SyndCategoryImpl> syndCategories = new ArrayList<SyndCategoryImpl>();
        for (Iterator<String> iterator = categories.iterator(); iterator.hasNext(); ) {
            SyndCategoryImpl syndCategory = new SyndCategoryImpl();
            syndCategory.setName(iterator.next());
            syndCategories.add(syndCategory);
        }

        syndFeed.setCategories(syndCategories);
    }


    public List<String> getSupportedRssFormats() {
        return SUPPORTED_FORMATS;
    }


    public String getRssFormat() {
        return syndFeed.getFeedType();
    }


    public void setRssFormat(String rssFormat) {
        if ((rssFormat == null) || !SUPPORTED_FORMATS.contains(rssFormat)) {
            String msg = "RSS format not supported: '" + rssFormat + "'. Using default format.";
            log.error(msg);
            syndFeed.setFeedType(FORMAT_DEFAULT);
        } else {
            syndFeed.setFeedType(rssFormat);
            // -- Assign mandatory defaults
            if (rssFormat.equals("rss_0.91N")) {
                syndFeed.setLanguage(LANGUAGE_DEFAULT);
            }
        }
    }


    public Date getPublicationDate() {
        return syndFeed.getPublishedDate();
    }


    public void setPublicationDate(Date date) {
        syndFeed.setPublishedDate(date);
    }


    public List<String> getAuthors() {
        List<String> result = new ArrayList<String>();

        List<?> authors = syndFeed.getAuthors();
        if (authors != null) {
            for (Iterator<?> iterator = authors.iterator(); iterator.hasNext(); ) {
                SyndPerson syndPerson = (SyndPerson)iterator.next();
                result.add(syndPerson.getName());
            }
        }

        List<?> contributors = syndFeed.getContributors();
        if (contributors != null) {
            for (Iterator<?> iterator = contributors.iterator(); iterator.hasNext(); ) {
                SyndPerson syndPerson = (SyndPerson)iterator.next();
                result.add(syndPerson.getName());
            }
        }

        if ((result.size() == 0) && (syndFeed.getAuthor() != null)) {
            result.add(syndFeed.getAuthor());
        }

        return result;
    }


    public void setAuthors(List<String> authors) {
        if ((authors == null) || (authors.size() == 0)) {
            syndFeed.setAuthor(null);
        } else if ((authors.size() > 1) && "atom_1.0".equals(getRssFormat())) {
            // Only Atom 1.0 in Rome supports multiple channel authors
            List<SyndPerson> syndPersons = new ArrayList<SyndPerson>();
            for (Iterator<String> iterator = authors.iterator(); iterator.hasNext(); ) {
                String author = iterator.next();
                SyndPerson syndPerson = new SyndPersonImpl();
                syndPerson.setName(author);
                syndPersons.add(syndPerson);
            }
            syndFeed.setAuthors(syndPersons);
        } else {
            if (authors.size() > 1) {
                log.warn("Rome does not support multiple channel authors. Using the first author.");
            }
            syndFeed.setAuthor(authors.get(0));
        }
    }


    public String getCopyright() {
        return syndFeed.getCopyright();
    }


    public void setCopyright(String copyright) {
        syndFeed.setCopyright(copyright);
    }

}
