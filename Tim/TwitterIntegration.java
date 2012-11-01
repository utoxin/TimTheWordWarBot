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
	static User BotTimmy;
	static User NaNoWordSprints;
	static User NaNoWriMo;
	static User officeduckfrank;
	
	public TwitterIntegration() {
		token = new AccessToken(Tim.db.getSetting("twitter_access_key"), Tim.db.getSetting("twitter_access_secret"));
		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(Tim.db.getSetting("twitter_consumer_key"), Tim.db.getSetting("twitter_consumer_secret"));
		twitter.setOAuthAccessToken(token);

		try {
			BotTimmy = twitter.showUser("BotTimmy");
			NaNoWordSprints = twitter.showUser("NaNoWordSprints");
			NaNoWriMo = twitter.showUser("NaNoWriMo");
			officeduckfrank = twitter.showUser("officeduckfrank");
		} catch (TwitterException ex) {
			Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public void startStream() {
		StatusListener publicListener = new StatusListener() {
			public void onStatus( Status status ) {
				String colorString;
				
				Tim.bot.log("@" + status.getUser().getScreenName() + ": " + status.getText());
				
				if (status.getUser().getScreenName().equals("NaNoWriMo") && status.getInReplyToUserId() == -1) {
					colorString = Colors.BOLD + Colors.DARK_BLUE;
				} else if (status.getUser().getScreenName().equals("NaNoWordSprints") && status.getInReplyToUserId() == -1) {
					colorString = Colors.BOLD + Colors.DARK_GREEN;
				} else if (status.getUser().getScreenName().equals("BotTimmy") && status.getInReplyToUserId() == -1) {
					colorString = Colors.BOLD + Colors.RED;
				} else if (status.getUser().getScreenName().equals("officeduckfrank") && status.getInReplyToUserId() == -1) {
					colorString = Colors.BOLD + Colors.MAGENTA;
				} else {
					try {
						if (status.getText().toLowerCase().contains("#nanowrimo") && Tim.rand.nextInt(100) < 3 && twitter.existsFriendship(status.getUser().getScreenName(), "BotTimmy")) {
							int r = Tim.rand.nextInt(Tim.amusement.eightballs.size());
							StatusUpdate reply = new StatusUpdate("@" + status.getUser().getScreenName() + " " + Tim.amusement.eightballs.get(r) + " #NaNoWriMo #FearTimmy");
							reply.setInReplyToStatusId(status.getId());
							twitter.updateStatus(reply);
						}
					} catch (TwitterException ex) {
						Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
					}

					return;
				}

				String message = colorString + "@" + status.getUser().getScreenName() + ": " + Colors.NORMAL + status.getText();

				for (ChannelInfo channel : Tim.db.channel_data.values()) {
					if (status.getUser().getId() == NaNoWriMo.getId() && channel.relayNaNoWriMo) {
						Tim.bot.sendMessage(channel.channel, message);
					} else if (status.getUser().getId() == NaNoWordSprints.getId() && channel.relayNaNoWordSprints) {
						Tim.bot.sendMessage(channel.channel, message);
					} else if (status.getUser().getId() == BotTimmy.getId() && channel.relayBotTimmy) {
						Tim.bot.sendMessage(channel.channel, message);
					} else if (status.getUser().getId() == officeduckfrank.getId() && channel.relayofficeduckfrank) {
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
						Tim.bot.log("@" + status.getUser().getScreenName() + ": " + status.getText());
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

		long[] userIds = {NaNoWriMo.getId(), NaNoWordSprints.getId(), BotTimmy.getId(), officeduckfrank.getId()};
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
