package se.sitic.megatron.rss.rome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.rss.IRssChannel;
import se.sitic.megatron.rss.IRssItem;
import se.sitic.megatron.util.ObjectStringSorter;

import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndLinkImpl;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.feed.synd.SyndPersonImpl;


/**
 * Implements IRssItem using Rome.
 */
public class RomeRssItem implements IRssItem {
    private static final Logger log = Logger.getLogger(RomeRssItem.class);

    private static final String DESCRIPTION_TYPE = "text/html";

    // UNSUSED: private TypedProperties props;
    private RomeRssChannel parentChannel;
    private SyndEntry syndEntry;


    /**
     * Constructs an item using specified SyndEntry-object.
     */
    public RomeRssItem(TypedProperties props, RomeRssChannel parentChannel, SyndEntry syndEntry) {
        // UNSUSED: this.props = props;
        this.parentChannel = parentChannel;
        this.syndEntry = syndEntry;
    }


    /**
     * Constructs an empty item.
     */
    public RomeRssItem(TypedProperties props, RomeRssChannel parentChannel) {
        this(props, parentChannel, new SyndEntryImpl());
    }


    /**
     * Returns wrapped SyndEntry object.
     */
    public SyndEntry getSyndEntry() {
        return this.syndEntry;
    }


    @Override
    public IRssChannel getParentChannel() {
        return this.parentChannel;
    }


    @Override
    public String getTitle() {
        return syndEntry.getTitle();
    }


    @Override
    public void setTitle(String title) {
        syndEntry.setTitle(title);
    }


    @Override
    public String getDescription() {
        String result = null;
        SyndContent syndContent = syndEntry.getDescription();
        if (syndContent != null) {
            result = syndContent.getValue();
        }

        return result;
    }


    @Override
    public void setDescription(String description) {
        SyndContent syndContent = syndEntry.getDescription();

        if (syndContent == null) {
            syndContent = new SyndContentImpl();
            syndEntry.setDescription(syndContent);
        }

        syndContent.setType(DESCRIPTION_TYPE);
        syndContent.setValue(description);
    }


    @Override
    public List<String> getLinks() {
        List<String> result = new ArrayList<String>();

        if (syndEntry.getLinks() != null) {
            for (Iterator<?> iterator = syndEntry.getLinks().iterator(); iterator.hasNext(); ) {
                SyndLinkImpl syndLink = (SyndLinkImpl)iterator.next();
                result.add(syndLink.getHref());
            }
        }

        if ((result.size() == 0) && (syndEntry.getLink() != null)) {
            result.add(syndEntry.getLink());
        }

        return result;
    }


    @Override
    public void setLinks(List<String> links) {
        // Rome does not seem to support multiple links, despite
        // setLinks(List) exists. Does not work for rss_1.0 at least.

        if ((links == null) || (links.size() == 0)) {
            syndEntry.setLink(null);
        } else {
            if (links.size() > 1) {
                log.warn("Rome does not support multiple item links. Using the first link.");
            }
            syndEntry.setLink(links.get(0));
        }
    }


    @Override
    public List<String> getCategories() {
        List<String> result = new ArrayList<String>();
        List<?> categories = syndEntry.getCategories();
        if (categories != null) {
            for (Iterator<?> iterator = categories.iterator(); iterator.hasNext(); ) {
                SyndCategoryImpl syndCategory = (SyndCategoryImpl)iterator.next();
                result.add(syndCategory.getName());
            }
        }

        // Rome does not preserve order from XML; sort by name.
        Collections.sort(result, ObjectStringSorter.createDefaultSorter());

        return result;
    }


    @Override
    public void setCategories(List<String> categories) {
        if (categories == null) {
            syndEntry.setCategories(null);
            return;
        }

        List<SyndCategoryImpl> syndCategories = new ArrayList<SyndCategoryImpl>();
        for (Iterator<String> iterator = categories.iterator(); iterator.hasNext(); ) {
            SyndCategoryImpl syndCategory = new SyndCategoryImpl();
            syndCategory.setName(iterator.next());
            syndCategories.add(syndCategory);
        }

        syndEntry.setCategories(syndCategories);
    }


    @Override
    public Date getPublicationDate() {
        return syndEntry.getPublishedDate();
    }


    @Override
    public void setPublicationDate(Date date) {
        syndEntry.setPublishedDate(date);
    }


    @Override
    public Date getUpdatedDate() {
        return syndEntry.getUpdatedDate();
    }


    @Override
    public void setUpdatedDate(Date date) {
        syndEntry.setUpdatedDate(date);
    }


    @Override
    public List<String> getAuthors() {
        List<String> result = new ArrayList<String>();

        List<?> authors = syndEntry.getAuthors();
        if (authors != null) {
            for (Iterator<?> iterator = authors.iterator(); iterator.hasNext(); ) {
                SyndPerson syndPerson = (SyndPerson)iterator.next();
                result.add(syndPerson.getName());
            }
        }

        List<?> contributors = syndEntry.getContributors();
        if (contributors != null) {
            for (Iterator<?> iterator = contributors.iterator(); iterator.hasNext(); ) {
                SyndPerson syndPerson = (SyndPerson)iterator.next();
                result.add(syndPerson.getName());
            }
        }

        if ((result.size() == 0) && (syndEntry.getAuthor() != null)) {
            result.add(syndEntry.getAuthor());
        }

        return result;
    }


    @Override
    public void setAuthors(List<String> authors) {
        if ((authors == null) || (authors.size() == 0)) {
            syndEntry.setAuthor(null);
        } else if ((authors.size() > 1) && "atom_1.0".equals(getRssFormat())) {
            // Only Atom 1.0 in Rome supports multiple item authors
            List<SyndPerson> syndPersons = new ArrayList<SyndPerson>();
            for (Iterator<String> iterator = authors.iterator(); iterator.hasNext(); ) {
                String author = iterator.next();
                SyndPerson syndPerson = new SyndPersonImpl();
                syndPerson.setName(author);
                syndPersons.add(syndPerson);
            }
            syndEntry.setAuthors(syndPersons);
        } else {
            if (authors.size() > 1) {
                log.warn("Rome does not support multiple item authors. Using the first author.");
            }
            syndEntry.setAuthor(authors.get(0));
        }
    }


    private String getRssFormat() {
        return parentChannel.getRssFormat();
    }

}
