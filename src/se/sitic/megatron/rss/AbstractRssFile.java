package se.sitic.megatron.rss;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;


/**
 * Handles a specific RSS file. 
 */
public abstract class AbstractRssFile {
    private static final Logger log = Logger.getLogger(AbstractRssFile.class);

    protected TypedProperties props;
    protected RssManager rssManager;    
    protected Calendar startTime;

    
    /**
     * Constructor.
     */
    public AbstractRssFile(TypedProperties props) {
        this.props = props;
    }

    
    /**
     * @see #addItems(List, List) 
     */
    public void addItem(String title, String description) throws MegatronException {
        addItems(Collections.singletonList(title), Collections.singletonList(description));
    }

    
    /**
     * Opens RSS file, adds specified items, removes items above threshold, and
     * closes file.
     */
    public void addItems(List<String> titles, List<String> descriptions) throws MegatronException {
        // param check
        if (titles.size() != descriptions.size()) {
            throw new MegatronException("RSS titles and descriptions not equals.");
        }
        if (titles.size() == 0) {
            return;
        }

        init();
        IRssFactory rssFactory = rssManager.createRssFactory();
        
        File file = new File(props.getString(getFileKey(), "rss-default.xml"));
        try {
            // -- Read or crete RSS
            IRssChannel rssChannel = null;
            if (isReadingExistingRssFile()) {
                log.debug("Reading RSS: " + file.getAbsoluteFile());
                rssChannel = rssManager.readRss(file, true);
                log.info("No. of existing RSS items: " + rssChannel.getItems().size());
            } else {
                log.debug("Creating new RSS file.");
                rssChannel = rssFactory.createRssChannel();
            }
            initRss(rssChannel);
        
            // -- Create new RSS items
            List<IRssItem> rssItems = new ArrayList<IRssItem>();
            Iterator<String> titleIterator = titles.iterator();
            Iterator<String> descriptionIterator = descriptions.iterator();
            for (int i = 0; i < titles.size(); i++) {
                IRssItem rssItem = rssFactory.createRssItem(rssChannel);
                rssItem.setTitle(titleIterator.next());
                rssItem.setDescription(descriptionIterator.next());
                rssItem.setPublicationDate(nextPublicationDate());
                rssItems.add(rssItem);
            }
            
            // -- Add RSS items and remove old items
            log.info("Adding " + rssItems.size() + " items to RSS file: " + file.getAbsolutePath());
            int expireTimeInMinutes = props.getInt(getItemExpireTimeInMinutesKey(), -1);
            int maxNoOfItems = props.getInt(getMaxNoOfItemsKey(), 40);
            List<IRssItem> allItems = new LinkedList<IRssItem>();
            allItems.addAll(rssItems);
            allItems.addAll(rssChannel.getItems());
            rssManager.removeExpiredItems(allItems, expireTimeInMinutes);
            rssManager.removeItemsAboveMaxLimit(allItems, maxNoOfItems);
            rssChannel.setItems(allItems);

            // -- Write RSS
            log.info("Writing " + rssChannel.getItems().size() + " items to RSS-file: " + file.getAbsolutePath()); 
            rssManager.writeRss(file, rssChannel);
        } catch (Exception e) {
            // RssException, IOException
            String msg = "Cannot create RSS: " + file.getAbsolutePath();
            throw new MegatronException(msg, e);
        }
    }

    
    protected abstract boolean isReadingExistingRssFile();

    
    protected abstract String getFileKey();

    
    protected abstract String getContentTitleKey();

    
    protected abstract String getContentLinkKey();

    
    protected abstract String getContentDescriptionKey();
    
    
    protected abstract String getContentAuthorKey();

    
    protected abstract String getContentCopyrightKey();

    
    protected abstract String getMaxNoOfItemsKey();

    
    protected abstract String getItemExpireTimeInMinutesKey();


    protected void init() {
        this.rssManager = new RssManager(props);
        this.startTime = Calendar.getInstance();
    }

 
    /**
     * Returns publication date. One second diff in case of several items;
     * keeps sort order in RSS client.  
     */
    private Date nextPublicationDate() {
        Date result = startTime.getTime();
        startTime.add(Calendar.SECOND, -1);
        return result;
    }

    
    private void initRss(IRssChannel rssChannel) {
        String title = props.getString(getContentTitleKey(), "");
        rssChannel.setTitle(title);
        String link = props.getString(getContentLinkKey(), null);
        if (link != null) {
            rssChannel.setLinks(Collections.singletonList(link));
        }
        String description = props.getString(getContentDescriptionKey(), "");
        rssChannel.setDescription(description);
        String author = props.getString(getContentAuthorKey(), null);
        if (author != null) {
            rssChannel.setAuthors(Collections.singletonList(author));
        }
        String copyright = props.getString(getContentCopyrightKey(), null);
        if (copyright != null) {
            rssChannel.setCopyright(copyright);
        }
    }
    
}
