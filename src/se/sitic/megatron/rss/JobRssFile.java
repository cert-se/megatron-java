package se.sitic.megatron.rss;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.TypedProperties;


/**
 * Handles the Job RSS file, which is a feed for completed log- and mail jobs.
 */
public class JobRssFile extends AbstractRssFile  {
    // UNUSED: private static final Logger log = Logger.getLogger(JobRssFile.class);

    
    public JobRssFile(TypedProperties props) {
        super(props);
    }
    

    @Override
    protected boolean isReadingExistingRssFile() {
        return true;
    }

    
    @Override
    protected String getFileKey() {
        return AppProperties.RSS_JOB_FILE_KEY;
    }

    
    @Override
    protected String getContentTitleKey() {
        return AppProperties.RSS_JOB_CONTENT_TITLE_KEY;
    }

    
    @Override
    protected String getContentLinkKey() {
        return AppProperties.RSS_JOB_CONTENT_LINK_KEY;
    }

    
    @Override
    protected String getContentDescriptionKey() {
        return AppProperties.RSS_JOB_CONTENT_DESCRIPTION_KEY;
    }

    
    @Override
    protected String getContentAuthorKey() {
        return AppProperties.RSS_JOB_CONTENT_AUTHOR_KEY;
    }

    
    @Override
    protected String getContentCopyrightKey() {
        return AppProperties.RSS_JOB_CONTENT_COPYRIGHT_KEY;
    }

    
    @Override
    protected String getMaxNoOfItemsKey() {
        return AppProperties.RSS_JOB_MAX_NO_OF_ITEMS_KEY;
    }

    
    @Override
    protected String getItemExpireTimeInMinutesKey() {
        return AppProperties.RSS_JOB_ITEM_EXPIRE_TIME_IN_MINUTES_KEY;
    }

}
