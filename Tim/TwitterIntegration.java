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
	static User NaNoWriMo;
	static User NaNoWordSprints;
	static User BotTimmy;
	
	public TwitterIntegration() {
		token = new AccessToken(Tim.db.getSetting("twitter_access_key"), Tim.db.getSetting("twitter_access_secret"));
		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(Tim.db.getSetting("twitter_consumer_key"), Tim.db.getSetting("twitter_consumer_secret"));
		twitter.setOAuthAccessToken(token);

		try {
			NaNoWriMo = twitter.showUser("NaNoWriMo");
			NaNoWordSprints = twitter.showUser("NaNoWordSprints");
			BotTimmy = twitter.showUser("BotTimmy");
		} catch (TwitterException ex) {
			Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public void startStream() {
		StatusListener publicListener = new StatusListener() {
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
					colorString = Colors.BOLD + Colors.RED;
				} else {
					if (status.getText().contains("?") && status.getText().toLowerCase().contains("#nanowrimo")) {
						if (Tim.rand.nextInt(100) < 10) {
							try {
								int r = Tim.rand.nextInt(Tim.amusement.eightballs.size());
								StatusUpdate reply = new StatusUpdate("@" + status.getUser().getScreenName() + " " + Tim.amusement.eightballs.get(r) + " #NaNoWriMo #FearTimmy");
								reply.setInReplyToStatusId(status.getId());
								twitter.updateStatus(reply);
							} catch (TwitterException ex) {
								Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
							}
						}
					}

					return;
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

		StatusListener userListener = new UserStreamListener() {
			public void onStatus( Status status ) {
				if (status.getInReplyToUserId() == TwitterIntegration.BotTimmy.getId()) {
					try {
						int r = Tim.rand.nextInt(Tim.amusement.eightballs.size());
						StatusUpdate reply = new StatusUpdate("@" + status.getUser().getScreenName() + " " + Tim.amusement.eightballs.get(r) + " #NaNoWriMo #FearTimmy");
						reply.setInReplyToStatusId(status.getId());
						twitter.updateStatus(reply);
					} catch (TwitterException ex) {
						Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
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

			public void onDeletionNotice( long l, long l1 ) {
			}

			public void onFriendList( long[] longs ) {
			}

			public void onFavorite( User user, User user1, Status status ) {
			}

			public void onUnfavorite( User user, User user1, Status status ) {
			}

			public void onFollow( User user, User user1 ) {
			}

			public void onRetweet( User user, User user1, Status status ) {
			}

			public void onDirectMessage( DirectMessage dm ) {
			}

			public void onUserListMemberAddition( User user, User user1, UserList ul ) {
			}

			public void onUserListMemberDeletion( User user, User user1, UserList ul ) {
			}

			public void onUserListSubscription( User user, User user1, UserList ul ) {
			}

			public void onUserListUnsubscription( User user, User user1, UserList ul ) {
			}

			public void onUserListCreation( User user, UserList ul ) {
			}

			public void onUserListUpdate( User user, UserList ul ) {
			}

			public void onUserListDeletion( User user, UserList ul ) {
			}

			public void onUserProfileUpdate( User user ) {
			}

			public void onBlock( User user, User user1 ) {
			}

			public void onUnblock( User user, User user1 ) {
			}
		};

		long[] userIds = {NaNoWriMo.getId(), NaNoWordSprints.getId(), BotTimmy.getId()};
		String[] hashtags = {"#NaNoWriMo"};

		FilterQuery filter = new FilterQuery(0, userIds, hashtags);

		TwitterStream publicStream = new TwitterStreamFactory().getInstance();
		publicStream.setOAuthConsumer(Tim.db.getSetting("twitter_consumer_key"), Tim.db.getSetting("twitter_consumer_secret"));
		publicStream.setOAuthAccessToken(token);
		publicStream.addListener(publicListener);
		publicStream.filter(filter);

		TwitterStream userStream = new TwitterStreamFactory().getInstance();
		userStream.setOAuthConsumer(Tim.db.getSetting("twitter_consumer_key"), Tim.db.getSetting("twitter_consumer_secret"));
		userStream.setOAuthAccessToken(token);
		userStream.addListener(userListener);
		userStream.user();
	}
}
