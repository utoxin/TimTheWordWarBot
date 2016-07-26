package Tim;

/*
 * Copyright (C) 2015 mwalker
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import org.pircbotx.Colors;
import twitter4j.*;
import twitter4j.auth.AccessToken;

class TwitterIntegration extends StatusAdapter {
	private Twitter twitter;
	private AccessToken token;
	TwitterStream userStream;
	TwitterStream publicStream;
	private String consumerKey;
	private String consumerSecret;

	private static User BotTimmy;
	private boolean reconnectPending = false;
	private boolean needFilterUpdate = false;
	private long lastConnect;
	private HashMap<String, Long> accountCache = new HashMap<>(32);
	private HashMap<Long, HashSet<String>> accountsToChannels = new HashMap<>(32);
	private IDs friendIDs;

	TwitterIntegration() {
		String accessKey = Tim.db.getSetting("twitter_access_key");
		String accessSecret = Tim.db.getSetting("twitter_access_secret");
		consumerKey = Tim.db.getSetting("twitter_consumer_key");
		consumerSecret = Tim.db.getSetting("twitter_consumer_secret");

		token = new AccessToken(accessKey, accessSecret);
		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(consumerKey, consumerSecret);
		twitter.setOAuthAccessToken(token);

		try {
			BotTimmy = twitter.showUser("BotTimmy");
			friendIDs = twitter.getFriendsIDs(BotTimmy.getId());
		} catch (TwitterException ex) {
			Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	long checkAccount(String accountName) {
		if (accountCache.containsKey(accountName)) {
			return accountCache.get(accountName);
		} else {
			try {
				User check = twitter.showUser(accountName);
				accountCache.put(accountName, check.getId());
				return check.getId();
			} catch (TwitterException ex) {
				Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
			}

			return 0;
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

	void sendDeidleTweet(String message) {
		try {
			if (friendIDs.getIDs().length > 0 && Tim.rand.nextInt(100) < 15) {
				long userId = friendIDs.getIDs()[Tim.rand.nextInt(friendIDs.getIDs().length)];
				User tempUser = twitter.showUser(userId);
				
				message = "@" + tempUser.getScreenName() + " " + message;
			}
			
			if (message.length() > 129) {
				message = message.substring(0, 126) + "...";
			}

			StatusUpdate status = new StatusUpdate(message + " #FearTimmy");
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

			check.forEach((user) -> userIds.add(user.getId()));
		} catch (TwitterException ex) {
			Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}

		return userIds;
	}

	void startStream() {
		userStream = new TwitterStreamFactory().getInstance();
		userStream.setOAuthConsumer(consumerKey, consumerSecret);
		userStream.setOAuthAccessToken(token);
		userStream.addListener(userListener);
		userStream.user();

		initAccountList();
		updateStreamFilters();
	}

	private void initAccountList() {
		HashSet<String> tmpAccountHash;
		Set<Long> tmpUserIds;

		for (ChannelInfo channel : Tim.db.channel_data.values()) {
			if (channel.twitter_accounts.size() > 0) {
				tmpUserIds = getTwitterIds(channel.twitter_accounts.toArray(new String[channel.twitter_accounts.size()]));

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

	boolean addAccount(String account, ChannelInfo channel) {
		long accountId = checkAccount(account);
		if (accountId == 0) {
			return false;
		}

		if (!accountsToChannels.containsKey(accountId)) {
			accountsToChannels.put(accountId, new HashSet<>(64));
			needFilterUpdate = true;
		}

		HashSet<String> tmpAccountHash = accountsToChannels.get(accountId);
		tmpAccountHash.add(channel.channel);

		updateStreamFilters();
		return true;
	}

	void removeAccount(String account, ChannelInfo channel) {
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
							Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
						}
					}
					publicStream.cleanUp();
				}
			}

			String[] hashtags = {"#NaNoWriMo"};
			long[] finalUserIds = ArrayUtils.toPrimitive(accountsToChannels.keySet().toArray(new Long[accountsToChannels.size()]));

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

				if (channel.muzzled) {
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
					Tim.bot.getUserChannelDao().getChannel(channel.channel).send().message(message);
					channel.tweetBucket -= 1f;
				}
			}

			if (!status.getUser().getScreenName().equals("BotTimmy")) {
				try {
					checkFriendship = twitter.showFriendship(BotTimmy.getId(), status.getUser().getId());
					if (status.getText().toLowerCase().contains("#nanowrimo") && Tim.rand.nextInt(100) < 3 && checkFriendship.isTargetFollowingSource() && !status.isRetweet()) {
						String message2;

						if (Tim.rand.nextInt(100) < 20) {
							int r = Tim.rand.nextInt(Tim.amusement.eightBalls.size());
							message2 = "@" + status.getUser().getScreenName() + " " + Tim.amusement.eightBalls.get(r);
						} else {
							message2 = "@" + status.getUser().getScreenName() + " " + Tim.markov.generate_markov("say");
						}

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

	private UserStreamListener userListener = new UserStreamListener() {
		@Override
		public void onStatus(Status status) {
			boolean sendReply = false;
			boolean getItem = false;
			
			if (status.getText().toLowerCase().startsWith("@bottimmy")) {
				try {
					if (status.getText().toLowerCase().contains("!unfollow")) {
						twitter.destroyFriendship(status.getUser().getId());

						StatusUpdate reply = new StatusUpdate("@" + status.getUser().getScreenName() + " Okay, I won't follow you anymore... #SadTimmy #NaNoWriMo");
						reply.setInReplyToStatusId(status.getId());

						twitter.updateStatus(reply);
						
						friendIDs = twitter.getFriendsIDs(BotTimmy.getId());

						return;
					} else if (status.getText().toLowerCase().contains("!follow")) {
						twitter.createFriendship(status.getUser().getId());

						StatusUpdate reply = new StatusUpdate("@" + status.getUser().getScreenName() + " Hurray! A new friend! #HappyTimmy #NaNoWriMo");
						reply.setInReplyToStatusId(status.getId());

						twitter.updateStatus(reply);
												
						friendIDs = twitter.getFriendsIDs(BotTimmy.getId());
						
						return;
					} else if (status.getText().toLowerCase().contains("!fridge")) {
						Pattern fridgeVictimPattern = Pattern.compile("!fridge @(\\S+)", Pattern.CASE_INSENSITIVE);
						Matcher fridgeVictimMatcher = fridgeVictimPattern.matcher(status.getText());

						String target;
						if (fridgeVictimMatcher.find() && Tim.rand.nextInt(100) > 33) {
							target = fridgeVictimMatcher.group(1);
						} else {
							target = status.getUser().getScreenName();
						}

						StatusUpdate reply = new StatusUpdate("Timmy hurls a " + Tim.amusement.colours.get(Tim.rand.nextInt(Tim.amusement.colours.size())) + " fridge at @" + target + "! #FearTimmy #NaNoWriMo");
						reply.setInReplyToStatusId(status.getId());

						twitter.updateStatus(reply);
						
						return;
					}
				} catch (TwitterException ex) {
					Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

			if (status.getInReplyToUserId() == TwitterIntegration.BotTimmy.getId()) {
				sendReply = true;
				if (Tim.rand.nextInt(100) < 15) {
					getItem = true;
				}
			} else if (status.getText().toLowerCase().contains("@bottimmy") && Tim.rand.nextInt(100) < 80) {
				sendReply = true;
				if (Tim.rand.nextInt(100) < 25) {
					getItem = true;
				}
			} else if (Tim.rand.nextInt(100) < 2) {
				sendReply = true;
				if (Tim.rand.nextInt(100) < 25) {
					getItem = true;
				}
			}

			if (status.getUser().getId() == TwitterIntegration.BotTimmy.getId() || status.isRetweet()) {
				sendReply = false;
			}

			if (sendReply) {
				try {
					String message;
					if (getItem) {
						int r = Tim.rand.nextInt(Tim.amusement.approvedItems.size());
						message = "@" + status.getUser().getScreenName() + " Here, have " + Tim.amusement.approvedItems.get(r);
					} else if (Tim.rand.nextInt(100) < 20) {
						int r = Tim.rand.nextInt(Tim.amusement.eightBalls.size());
						message = "@" + status.getUser().getScreenName() + " " + Tim.amusement.eightBalls.get(r);
					} else {
						message = "@" + status.getUser().getScreenName() + " " + Tim.markov.generate_markov("say");
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
		public void onFollow(User user, User user1) {
			try {
				friendIDs = twitter.getFriendsIDs(BotTimmy.getId());
			} catch (TwitterException ex) {
				Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		@Override
		public void onUnfollow(User user, User user1) {
			try {
				friendIDs = twitter.getFriendsIDs(BotTimmy.getId());
			} catch (TwitterException ex) {
				Logger.getLogger(TwitterIntegration.class.getName()).log(Level.SEVERE, null, ex);
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
		public void onDeletionNotice(long l, long l1) {}

		@Override
		public void onFriendList(long[] longs) {}

		@Override
		public void onFavorite(User user, User user1, Status status) {}

		@Override
		public void onUnfavorite(User user, User user1, Status status) {}

		@Override
		public void onDirectMessage(DirectMessage dm) {}

		@Override
		public void onUserListMemberAddition(User user, User user1, UserList ul) {}

		@Override
		public void onUserListMemberDeletion(User user, User user1, UserList ul) {}

		@Override
		public void onUserListSubscription(User user, User user1, UserList ul) {}

		@Override
		public void onUserListUnsubscription(User user, User user1, UserList ul) {}

		@Override
		public void onUserListCreation(User user, UserList ul) {}

		@Override
		public void onUserListUpdate(User user, UserList ul) {}

		@Override
		public void onUserListDeletion(User user, UserList ul) {}

		@Override
		public void onUserProfileUpdate(User user) {}

		@Override
		public void onUserSuspension(long suspendedUser) {}

		@Override
		public void onUserDeletion(long deletedUser) {}

		@Override
		public void onBlock(User user, User user1) {}

		@Override
		public void onUnblock(User user, User user1) {}

		@Override
		public void onRetweetedRetweet(User source, User target, Status retweetedStatus) {}

		@Override
		public void onFavoritedRetweet(User source, User target, Status favoritedRetweeet) {}

		@Override
		public void onQuotedTweet(User source, User target, Status quotingTweet) {}

		@Override
		public void onStallWarning(StallWarning sw) {}
	};
}
