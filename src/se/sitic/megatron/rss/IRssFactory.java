package se.sitic.megatron.rss;


/**
 * Creates objects that handle parsing, building, and saving RSS feeds.
 */
public interface IRssFactory {

    public IRssParser createRssParser();

    public IRssChannel createRssChannel();

    public IRssItem createRssItem(IRssChannel parentChannel);

    public IRssWriter createRssWriter();

}
