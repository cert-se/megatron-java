package se.sitic.megatron.rss;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.TypedProperties;


/**
 * Handles the Stats RSS file, which contains statistics generated 
 * from the database. The file is re-generated every run.
 */
public class StatsRssFile extends AbstractRssFile  {
    // UNUSED: private static final Logger log = Logger.getLogger(StatsRssFile);

    
    public StatsRssFile(TypedProperties props) {
        super(props);
    }
    

    @Override
    protected boolean isReadingExistingRssFile() {
        // RSS file is re-generated every run
        return false;
    }

    
    @Override
    protected String getFileKey() {
        return AppProperties.RSS_STATS_FILE_KEY;
    }

    
    @Override
    protected String getContentTitleKey() {
        return AppProperties.RSS_STATS_CONTENT_TITLE_KEY;
    }

    
    @Override
    protected String getContentLinkKey() {
        return AppProperties.RSS_STATS_CONTENT_LINK_KEY;
    }

    
    @Override
    protected String getContentDescriptionKey() {
        return AppProperties.RSS_STATS_CONTENT_DESCRIPTION_KEY;
    }

    
    @Override
    protected String getContentAuthorKey() {
        return AppProperties.RSS_STATS_CONTENT_AUTHOR_KEY;
    }

    
    @Override
    protected String getContentCopyrightKey() {
        return AppProperties.RSS_STATS_CONTENT_COPYRIGHT_KEY;
    }

    
    @Override
    protected String getMaxNoOfItemsKey() {
        return AppProperties.RSS_STATS_MAX_NO_OF_ITEMS_KEY;
    }

    
    @Override
    protected String getItemExpireTimeInMinutesKey() {
        return AppProperties.RSS_STATS_ITEM_EXPIRE_TIME_IN_MINUTES_KEY;
    }

}
