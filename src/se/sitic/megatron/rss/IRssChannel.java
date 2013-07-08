package se.sitic.megatron.rss;

import java.util.Date;
import java.util.List;


/**
 * Represents an channel-tag in a RSS file (or feed-tag in an Atom file).
 * This is the top-level object for a feed file.
 */
public interface IRssChannel {

    public List<IRssItem> getItems();

    public void setItems(List<IRssItem> items);

    public boolean removeItem(IRssItem item);

    public String getTitle();

    public void setTitle(String title);

    public String getDescription();

    public void setDescription(String description);

    public List<String> getLinks();

    public void setLinks(List<String> links);

    public List<String> getCategories();

    public void setCategories(List<String> categories);

    public List<String> getSupportedRssFormats();

    public String getRssFormat();

    public void setRssFormat(String rssFormat);

    public Date getPublicationDate();

    public void setPublicationDate(Date date);

    public List<String> getAuthors();

    public void setAuthors(List<String> authors);

    public String getCopyright();

    public void setCopyright(String copyright);

}
