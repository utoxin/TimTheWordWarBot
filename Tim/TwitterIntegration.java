/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Tim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.ArrayUtils;
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
	TwitterStream userStream;
	TwitterStream publicStream;
	String accessKey;
	String accessSecret;
	String consumerKey;
	String consumerSecret;

	static User BotTimmy;
	boolean started = false;
	HashMap<Long,HashSet<String>> accountsToChannels = new HashMap<>(32);

	public TwitterIntegration() {
		accessKey = Tim.db.getSetting("twitter_access_key");
		accessSecret = Tim.db.getSetting("twitter_access_secret");
		consumerKey = Tim.db.getSetting("twitter_consumer_key");
		consumerSecret = Tim.db.getSetting("twitter_consumer_secret");

		token = new AccessToken(accessKey, accessSecret);
		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(consumerKey, consumerSecret);
		twitter.setOAuthAccessToken(token);

		try {
			BotTimmy = twitter.showUser("BotTimmy");
		} catch (TwitterException ex) {
			Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void sendTweet(String message) {
		try {
			if (message.length() > 118) {
				message = message.substring(0, 115) + "...";
			}

			StatusUpdate status = new StatusUpdate(message + " #NaNoWriMo #FearTimmy");
			twitter.updateStatus(status);
		} catch (TwitterException ex) {
			Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private Set<Long> getTwitterIds(String[] usernames) {
		ResponseList<User> check;
		Set<Long> userIds = new HashSet<>(128);

		try {
			check = twitter.lookupUsers(usernames);

			for(User user : check) {
				userIds.add(user.getId());
			}
		} catch (TwitterException ex) {
			Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}

		return userIds;
	}

	public void startStream() {
		userStream = new TwitterStreamFactory().getInstance();
		userStream.setOAuthConsumer(consumerKey, consumerSecret);
		userStream.setOAuthAccessToken(token);
		userStream.addListener(userListener);
		userStream.user();

		publicStream = new TwitterStreamFactory().getInstance();
		publicStream.setOAuthConsumer(consumerKey, consumerSecret);
		publicStream.setOAuthAccessToken(token);
		updateStreamFilters();
	}

	public void updateStreamFilters() {
		Set<Long> userIds = new HashSet<>(128);
		Set<Long> tmpUserIds;
		HashSet<String> tmpAccountHash;

		String[] hashtags = {"#NaNoWriMo"};

		for (ChannelInfo channel : Tim.db.channel_data.values()) {
			if (channel.twitter_accounts.size() > 0) {
				tmpUserIds = getTwitterIds(channel.twitter_accounts.toArray(new String[channel.twitter_accounts.size()]));

				for (Long userId : tmpUserIds) {
					if (!accountsToChannels.containsKey(userId)) {
						accountsToChannels.put(userId, new HashSet<String>(64));
					}

					tmpAccountHash = accountsToChannels.get(userId);
					tmpAccountHash.add(channel.channel.getName().toLowerCase());
				}

				userIds.addAll(
					tmpUserIds
				);
			}
		}

		long[] finalUserIds = ArrayUtils.toPrimitive(userIds.toArray(new Long[userIds.size()]));

		FilterQuery filter = new FilterQuery(0, finalUserIds, hashtags);

		if (started) {
			publicStream.cleanUp();
		} else {
			started = true;
		}

		publicStream.addListener(publicListener);
		publicStream.filter(filter);
	}

	StatusListener publicListener = new StatusListener() {
		@Override
		public void onStatus( Status status ) {
			String colorString;
			Relationship checkFriendship;

			if (status.getUser().getScreenName().equals("NaNoWriMo") && status.getInReplyToUserId() == -1) {
				colorString = Colors.BOLD + Colors.DARK_BLUE;
			} else if (status.getUser().getScreenName().equals("NaNoWordSprints") && status.getInReplyToUserId() == -1) {
				colorString = Colors.BOLD + Colors.DARK_GREEN;
			} else if (status.getUser().getScreenName().equals("BotTimmy") && status.getInReplyToUserId() == -1) {
				colorString = Colors.BOLD + Colors.RED;
			} else if (status.getUser().getScreenName().equals("officeduckfrank") && status.getInReplyToUserId() == -1) {
				colorString = Colors.BOLD + Colors.MAGENTA;
			} else {
				colorString = Colors.BOLD + Colors.OLIVE;
			}

			String message = colorString + "@" + status.getUser().getScreenName() + ": " + Colors.NORMAL + status.getText();

			HashSet<String> channels = accountsToChannels.get(status.getUser().getId());

			float timeDiff;
			for (String channelName : channels) {
				ChannelInfo channel = Tim.db.channel_data.get(channelName);
				
				if (channel.tweetBucket < channel.tweetBucketMax) {
					timeDiff = (System.currentTimeMillis() - channel.twitterTimer) / 1000f;
					channel.twitterTimer = System.currentTimeMillis();
					channel.tweetBucket += (timeDiff / 60f) * channel.tweetBucketChargeRate;

					if (channel.tweetBucket > channel.tweetBucketMax) {
						channel.tweetBucket = channel.tweetBucketMax;
					}
				}
				
				if (channel.muzzled) {
					continue;
				}
				
				if (channel.tweetBucket >= 1) {
					Tim.bot.sendMessage(channel.channel, message);
					channel.tweetBucket -= 1f;
				}
			}

			if (!status.getUser().getScreenName().equals("BotTimmy")) {
				try {
					checkFriendship = twitter.showFriendship(BotTimmy.getId(), status.getUser().getId());
					if (status.getText().toLowerCase().contains("#nanowrimo") && Tim.rand.nextInt(100) < 3 && checkFriendship.isTargetFollowingSource() ) {
						int r = Tim.rand.nextInt(Tim.amusement.eightballs.size());
						String message2 = "@" + status.getUser().getScreenName() + " " + Tim.amusement.eightballs.get(r);
						if (message2.length() > 118) {
							message2 = message2.substring(0, 115) + "...";
						}

						StatusUpdate reply = new StatusUpdate(message2 + " #NaNoWriMo #FearTimmy");

						reply.setInReplyToStatusId(status.getId());
						twitter.updateStatus(reply);
					}
				} catch (TwitterException ex) {
					Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}

		@Override
		public void onDeletionNotice( StatusDeletionNotice sdn ) {
		}

		@Override
		public void onTrackLimitationNotice( int i ) {
		}

		@Override
		public void onScrubGeo( long l, long l1 ) {
		}

		@Override
		public void onException( Exception excptn ) {
		}

		@Override
		public void onStallWarning(StallWarning sw) {
		}
	};

	UserStreamListener userListener = new UserStreamListener() {
		@Override
		public void onStatus( Status status ) {
			boolean sendReply = false;
			boolean getItem = false;
			if (status.getInReplyToUserId() == TwitterIntegration.BotTimmy.getId()) {
				sendReply = true;
				if (Tim.rand.nextInt(100) < 15) {
					getItem = true;
				}
			} else if (status.getText().toLowerCase().contains("@bottimmy") && Tim.rand.nextInt(100) < 50) {
				sendReply = true;
				if (Tim.rand.nextInt(100) < 25) {
					getItem = true;
				}
			} else if (Tim.rand.nextInt(100) < 2) {
				sendReply = true;
				if (Tim.rand.nextInt(100) < 50) {
					getItem = true;
				}
			}

			if (status.getUser().getId() == TwitterIntegration.BotTimmy.getId()) {
				sendReply = false;
			}

			if (sendReply) {
				try {
					String message;
					if (getItem) {
						int r = Tim.rand.nextInt(Tim.amusement.approved_items.size());
						message = "@" + status.getUser().getScreenName() + " Here, have " + Tim.amusement.approved_items.get(r);
					} else {
						int r = Tim.rand.nextInt(Tim.amusement.eightballs.size());
						message = "@" + status.getUser().getScreenName() + " " + Tim.amusement.eightballs.get(r);
					}

					if (message.length() > 118) {
						message = message.substring(0, 115) + "...";
					}

					StatusUpdate reply = new StatusUpdate(message + " #NaNoWriMo #FearTimmy");
					reply.setInReplyToStatusId(status.getId());

					twitter.updateStatus(reply);
				} catch (TwitterException ex) {
					Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}

		@Override
		public void onDeletionNotice( StatusDeletionNotice sdn ) {
		}

		@Override
		public void onTrackLimitationNotice( int i ) {
		}

		@Override
		public void onScrubGeo( long l, long l1 ) {
		}

		@Override
		public void onException( Exception excptn ) {
		}

		@Override
		public void onDeletionNotice( long l, long l1 ) {
		}

		@Override
		public void onFriendList( long[] longs ) {
		}

		@Override
		public void onFavorite( User user, User user1, Status status ) {
		}

		@Override
		public void onUnfavorite( User user, User user1, Status status ) {
		}

		@Override
		public void onFollow( User user, User user1 ) {
		}

		@Override
		public void onDirectMessage( DirectMessage dm ) {
		}

		@Override
		public void onUserListMemberAddition( User user, User user1, UserList ul ) {
		}

		@Override
		public void onUserListMemberDeletion( User user, User user1, UserList ul ) {
		}

		@Override
		public void onUserListSubscription( User user, User user1, UserList ul ) {
		}

		@Override
		public void onUserListUnsubscription( User user, User user1, UserList ul ) {
		}

		@Override
		public void onUserListCreation( User user, UserList ul ) {
		}

		@Override
		public void onUserListUpdate( User user, UserList ul ) {
		}

		@Override
		public void onUserListDeletion( User user, UserList ul ) {
		}

		@Override
		public void onUserProfileUpdate( User user ) {
		}

		@Override
		public void onBlock( User user, User user1 ) {
		}

		@Override
		public void onUnblock( User user, User user1 ) {
		}

		@Override
		public void onStallWarning(StallWarning sw) {
		}
	};
}