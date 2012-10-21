/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Tim;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.pircbotx.Colors;
import twitter4j.*;
import twitter4j.auth.AccessToken;

/**
 *
 * @author Matthew Walker
 */
public class TwitterIntegration extends StatusAdapter {
	Twitter twitter;
	AccessToken token;
	
	public TwitterIntegration() {
		token = new AccessToken(Tim.db.getSetting("twitter_access_key"), Tim.db.getSetting("twitter_access_secret"));
		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(Tim.db.getSetting("twitter_consumer_key"), Tim.db.getSetting("twitter_consumer_secret"));
		twitter.setOAuthAccessToken(token);
	}
	
	public void startStream() {
		TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
		twitterStream.setOAuthConsumer(Tim.db.getSetting("twitter_consumer_key"), Tim.db.getSetting("twitter_consumer_secret"));
		twitterStream.setOAuthAccessToken(token);

		StatusListener listener = new StatusListener() {
			public void onStatus( Status status ) {
				String colorString;

				if (status.getInReplyToUserId() != -1) {
					return;
				}

				if (status.getUser().getScreenName().equals("NaNoWriMo")) {
					colorString = Colors.BOLD + Colors.DARK_BLUE;
				} else if (status.getUser().getScreenName().equals("NaNoWordsprints")) {
					colorString = Colors.BOLD + Colors.DARK_GREEN;
				} else if (status.getUser().getScreenName().equals("BotTimmy")) {
					colorString = Colors.BOLD + Colors.DARK_GREEN;
				} else {
					colorString = Colors.BOLD + Colors.DARK_GRAY;
				}

				String message = colorString + "@" + status.getUser().getScreenName() + ": " + Colors.NORMAL + status.getText();

				for (ChannelInfo channel : Tim.db.channel_data.values()) {
					if (channel.relayTwitter) {
						Tim.bot.sendMessage(channel.channel, message);
					}
				}
			}

			public void onDeletionNotice( StatusDeletionNotice sdn ) {
			}

			public void onTrackLimitationNotice( int i ) {
			}

			public void onScrubGeo( long l, long l1 ) {
			}

			public void onException( Exception excptn ) {
			}

		};
			
		try {
			User NaNoWriMo = twitter.showUser("NaNoWriMo");
			User NaNoWordsprints = twitter.showUser("NaNoWordsprints");
			User BotTimmy = twitter.showUser("BotTimmy");

			long[] userIds = {NaNoWriMo.getId(), NaNoWordsprints.getId(), BotTimmy.getId()};
			
			FilterQuery filter = new FilterQuery(0, userIds);
			
			twitterStream.addListener(listener);
			twitterStream.filter(filter);
		} catch (TwitterException ex) {
			Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
