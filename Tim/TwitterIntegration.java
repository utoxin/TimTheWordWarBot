package Tim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import Tim.Data.ChannelInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.pircbotx.Colors;
import twitter4j.*;
import twitter4j.auth.AccessToken;

public class TwitterIntegration extends StatusAdapter {
	private Twitter twitter;
	private AccessToken token;
	TwitterStream publicStream;
	private String consumerKey;
	private String consumerSecret;

	private static User BotTimmy;
	private boolean reconnectPending = false;
	private boolean needFilterUpdate = false;
	private long lastConnect;
	private HashMap<String, Long> accountCache = new HashMap<>();
	private HashMap<Long, HashSet<String>> accountsToChannels = new HashMap<>();
	private IDs friendIDs;
	
	private boolean connected = false;

	TwitterIntegration() {
		String accessKey = Tim.db.getSetting("twitter_access_key");
		String accessSecret = Tim.db.getSetting("twitter_access_secret");
		consumerKey = Tim.db.getSetting("twitter_consumer_key");
		consumerSecret = Tim.db.getSetting("twitter_consumer_secret");

		if (!accessKey.equals("") && !accessSecret.equals("") && !consumerKey.equals("") && !consumerSecret.equals("")) {
			token = new AccessToken(accessKey, accessSecret);
			twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(consumerKey, consumerSecret);
			twitter.setOAuthAccessToken(token);
			
			try {
				BotTimmy = twitter.showUser("BotTimmy");
				friendIDs = twitter.getFriendsIDs(BotTimmy.getId());
				connected = true;
			} catch (TwitterException ex) {
				Tim.printStackTrace(ex);
			}
		}
	}

	long checkAccount(String accountName) {
		if (!connected) return 0;

		if (accountCache.containsKey(accountName)) {
			return accountCache.get(accountName);
		} else {
			try {
				User check = twitter.showUser(accountName);
				accountCache.put(accountName, check.getId());
				return check.getId();
			} catch (TwitterException ex) {
				Tim.printStackTrace(ex);
			}
			
			return 0;
		}
	}

	void sendDeidleTweet(String message) {
		try {
			if (!connected) return;

			if (friendIDs.getIDs().length > 0 && Tim.rand.nextInt(100) < 15) {
				long userId = friendIDs.getIDs()[Tim.rand.nextInt(friendIDs.getIDs().length)];
				User tempUser = twitter.showUser(userId);
				
				message = "@" + tempUser.getScreenName() + " " + message;
			}
			
			if (message.length() > (280-11)) {
				message = message.substring(0, (280-13)) + "...";
			}

			StatusUpdate status = new StatusUpdate(message + " #FearTimmy");
			twitter.updateStatus(status);
		} catch (TwitterException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private Set<Long> getTwitterIds(String[] usernames) {
		ResponseList<User> check;
		Set<Long> userIds = new HashSet<>(128);
		
		try {
			check = twitter.lookupUsers(usernames);

			check.forEach((user) -> userIds.add(user.getId()));
		} catch (TwitterException ex) {
			Tim.printStackTrace(ex);
		}

		return userIds;
	}

	void startStream() {
		if (!connected) return;

		initAccountList();
		updateStreamFilters();
	}

	private void initAccountList() {
		HashSet<String> tmpAccountHash;
		Set<Long> tmpUserIds;

		for (ChannelInfo channel : Tim.db.channel_data.values()) {
			if (channel.twitter_accounts.size() > 0) {
				tmpUserIds = getTwitterIds(channel.twitter_accounts.toArray(new String[0]));

				for (Long userId : tmpUserIds) {
					if (!accountsToChannels.containsKey(userId)) {
						accountsToChannels.put(userId, new HashSet<>(64));
					}

					tmpAccountHash = accountsToChannels.get(userId);
					tmpAccountHash.add(channel.channel);
				}
			}

		}

		needFilterUpdate = true;
	}

	public void addAccount(String account, ChannelInfo channel) {
		if (!connected) return;

		long accountId = checkAccount(account);
		if (accountId == 0) {
			return;
		}

		if (!accountsToChannels.containsKey(accountId)) {
			accountsToChannels.put(accountId, new HashSet<>(64));
			needFilterUpdate = true;
		}

		HashSet<String> tmpAccountHash = accountsToChannels.get(accountId);
		tmpAccountHash.add(channel.channel);

		updateStreamFilters();
	}

	public void removeAccount(String account, ChannelInfo channel) {
		if (!connected) return;

		long accountId = checkAccount(account);
		if (accountId == 0) {
			return;
		}

		if (!accountsToChannels.containsKey(accountId)) {
			return;
		}

		HashSet<String> tmpAccountHash = accountsToChannels.get(accountId);
		tmpAccountHash.remove(channel.channel);

		if (tmpAccountHash.isEmpty()) {
			accountsToChannels.remove(accountId);
			needFilterUpdate = true;
		}

		updateStreamFilters();
	}

	private void updateStreamFilters() {
		if (needFilterUpdate) {
			if (publicStream != null) {
				if (reconnectPending) {
					return;
				} else {
					reconnectPending = true;
					long nextConnect = (lastConnect + 90000) - System.currentTimeMillis();
					if (nextConnect > 0) {
						Logger.getLogger(TwitterIntegration.class.getName()).log(Level.INFO, "Sleeping for {0} milliseconds before twitter reconnect.", nextConnect);
						try {
							Thread.sleep(nextConnect);
						} catch (InterruptedException ex) {
							Tim.printStackTrace(ex);
						}
					}
					publicStream.cleanUp();
				}
			}

			String[] hashtags = {"#NaNoWriMo"};
			long[] finalUserIds = ArrayUtils.toPrimitive(accountsToChannels.keySet().toArray(new Long[0]));

			publicStream = new TwitterStreamFactory().getInstance();
			publicStream.setOAuthConsumer(consumerKey, consumerSecret);
			publicStream.setOAuthAccessToken(token);
			publicStream.addListener(publicListener);

			FilterQuery filter = new FilterQuery(0, finalUserIds, hashtags);
			publicStream.filter(filter);

			lastConnect = System.currentTimeMillis();
			reconnectPending = false;
			needFilterUpdate = false;
		}
	}

	private StatusListener publicListener = new StatusListener() {
		@Override
		public void onStatus(Status status) {
			String colorString;
			Relationship checkFriendship;

			switch (status.getUser().getScreenName()) {
				case "NaNoWriMo":
					colorString = Colors.BOLD + Colors.DARK_BLUE;
					break;
				case "NaNoWordSprints":
					colorString = Colors.BOLD + Colors.DARK_GREEN;
					break;
				case "BotTimmy":
					colorString = Colors.BOLD + Colors.RED;
					break;
				case "officeduckfrank":
					colorString = Colors.BOLD + Colors.MAGENTA;
					break;
				default:
					colorString = Colors.BOLD + Colors.OLIVE;
					break;
			}

			if (status.getInReplyToUserId() != -1) {
				return;
			}

			String message = colorString + "@" + status.getUser().getScreenName() + ": " + Colors.NORMAL + status.getText().replaceAll("\\n", " ");

			HashSet<String> channels = accountsToChannels.get(status.getUser().getId());

			float timeDiff;

			for (String channelName : channels) {
				ChannelInfo channel = Tim.db.channel_data.get(channelName);

				if (channel.isMuzzled()) {
					continue;
				}

				if (channel.tweetBucket < channel.tweetBucketMax) {
					timeDiff = (System.currentTimeMillis() - channel.twitterTimer) / 1000f;
					channel.twitterTimer = System.currentTimeMillis();
					channel.tweetBucket += (timeDiff / 60f) * channel.tweetBucketChargeRate;

					if (channel.tweetBucket > channel.tweetBucketMax) {
						channel.tweetBucket = channel.tweetBucketMax;
					}
				}

				if (channel.tweetBucket >= 1) {
					Tim.channelStorage.channelList.get(channel.channel).send().message(message);
					channel.tweetBucket -= 1f;
				}
			}

			if (!status.getUser().getScreenName().equals("BotTimmy")) {
				try {
					checkFriendship = twitter.showFriendship(BotTimmy.getId(), status.getUser().getId());
					if (status.getText().toLowerCase().contains("#nanowrimo") && Tim.rand.nextInt(100) < 3 && checkFriendship.isTargetFollowingSource() && !status.isRetweet()) {
						String message2;

						if (Tim.rand.nextInt(100) < 20) {
							int r = Tim.rand.nextInt(Tim.db.eightBalls.size());
							message2 = "@" + status.getUser().getScreenName() + " " + Tim.db.eightBalls.get(r);
						} else {
							message2 = "@" + status.getUser().getScreenName() + " " + Tim.markov.generate_markov();
						}

						if (message2.length() > (280-22)) {
							message2 = message2.substring(0, (280-25)) + "...";
						}

						StatusUpdate reply = new StatusUpdate(message2 + " #NaNoWriMo #FearTimmy");

						reply.setInReplyToStatusId(status.getId());
						twitter.updateStatus(reply);
					}
				} catch (TwitterException ex) {
					Tim.printStackTrace(ex);
				}
			}
		}

		@Override
		public void onDeletionNotice(StatusDeletionNotice sdn) {}

		@Override
		public void onTrackLimitationNotice(int i) {}

		@Override
		public void onScrubGeo(long l, long l1) {}

		@Override
		public void onException(Exception excptn) {}

		@Override
		public void onStallWarning(StallWarning sw) {}
	};
}
