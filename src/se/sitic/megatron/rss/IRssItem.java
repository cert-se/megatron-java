package se.sitic.megatron.rss;

import java.util.Date;
import java.util.List;


/**
 * Represents an item-tag in a RSS file (or entry-tag in an Atom file).
 */
public interface IRssItem {

    public IRssChannel getParentChannel();

    public String getTitle();

    public void setTitle(String title);

    public String getDescription();

    public void setDescription(String description);

    public List<String> getLinks();

    public void setLinks(List<String> links);

    public List<String> getCategories();

    public void setCategories(List<String> categories);

    public Date getPublicationDate();

    public void setPublicationDate(Date date);

    public Date getUpdatedDate();

    public void setUpdatedDate(Date date);

    public List<String> getAuthors();

    public void setAuthors(List<String> authors);

    // TODO Support for enclosures. Add the wrapper interface IRssEnclosure
    //     public List<IRssEnclosure> getEnclosures();
    //     public void setEnclosures(List<IRssEnclosure> enclosures);

}
