/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tim;

import java.util.*;
import org.pircbotx.User;

/**
 *
 * @author Matthew Walker
 */
public class ChannelInfo {

	public String channel;

	public HashMap<String, Boolean> chatter_enabled = new HashMap<>(16);
	public HashMap<String, Boolean> commands_enabled = new HashMap<>(16);
	public Set<String> twitter_accounts = new HashSet<>(16);
	
	public User lastSpeaker;
	public long lastSpeakerTime;

	public int reactiveChatterLevel;
	public int randomChatterLevel;
	public int chatterNameMultiplier;

	public long twitterTimer;
	public float tweetBucket;
	public float tweetBucketMax;
	public float tweetBucketChargeRate;

	public boolean muzzled = false;
	public boolean auto_muzzle_wars = true;
	public boolean auto_muzzled = false;

	public ChannelInfo(String channel) {
		this.channel = channel;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass().getName()).append(" Object {").append(NEW_LINE);
		result.append(" Channel: ").append(channel).append(NEW_LINE);
		result.append(" Chatter Settings: ").append(chatter_enabled).append(NEW_LINE);
		result.append(" Command Settings: ").append(commands_enabled).append(NEW_LINE);
		result.append(" Twitter Accounts: ").append(twitter_accounts).append(NEW_LINE);
		result.append(" Chatter Name *: ").append(chatterNameMultiplier).append(NEW_LINE);
		result.append(" Twitter Timer: ").append(twitterTimer).append(NEW_LINE);
		result.append(" Twitter Bucket: ").append(tweetBucket).append(NEW_LINE);
		result.append(" Twitter Bucket Max: ").append(tweetBucketMax).append(NEW_LINE);
		result.append(" Twitter Bucket Charge Rate: ").append(tweetBucketChargeRate).append(NEW_LINE);
		result.append(" Muzzled: ").append(muzzled).append(NEW_LINE);
		result.append(" Auto Muzzle Wars: ").append(auto_muzzle_wars).append(NEW_LINE);

		result.append("}");

		return result.toString();
	}

	public void setDefaultOptions() {
		chatterNameMultiplier = 3;
		reactiveChatterLevel = 5;
		randomChatterLevel = 2;

		tweetBucketMax = 10;
		tweetBucket = 5;
		tweetBucketChargeRate = 0.5f;

		chatter_enabled.put("greetings", Boolean.TRUE);
		chatter_enabled.put("helpful_reactions", Boolean.TRUE);
		chatter_enabled.put("silly_reactions", Boolean.TRUE);
		chatter_enabled.put("markov", Boolean.TRUE);
		chatter_enabled.put("challenge", Boolean.TRUE);
		chatter_enabled.put("get", Boolean.TRUE);
		chatter_enabled.put("eightball", Boolean.TRUE);
		chatter_enabled.put("fridge", Boolean.TRUE);
		chatter_enabled.put("defenestrate", Boolean.TRUE);
		chatter_enabled.put("sing", Boolean.TRUE);
		chatter_enabled.put("foof", Boolean.TRUE);
		chatter_enabled.put("dance", Boolean.TRUE);
		chatter_enabled.put("summon", Boolean.TRUE);
		chatter_enabled.put("creeper", Boolean.TRUE);
		chatter_enabled.put("search", Boolean.TRUE);
		chatter_enabled.put("chainstory", Boolean.TRUE);
		chatter_enabled.put("herd", Boolean.TRUE);
		chatter_enabled.put("banish", Boolean.TRUE);

		commands_enabled.put("attack", Boolean.TRUE);
		commands_enabled.put("banish", Boolean.TRUE);
		commands_enabled.put("chainstory", Boolean.TRUE);
		commands_enabled.put("challenge", Boolean.TRUE);
		commands_enabled.put("commandment", Boolean.TRUE);
		commands_enabled.put("creeper", Boolean.TRUE);
		commands_enabled.put("defenestrate", Boolean.TRUE);
		commands_enabled.put("dance", Boolean.TRUE);
		commands_enabled.put("dice", Boolean.TRUE);
		commands_enabled.put("eightball", Boolean.TRUE);
		commands_enabled.put("expound", Boolean.TRUE);
		commands_enabled.put("foof", Boolean.TRUE);
		commands_enabled.put("fridge", Boolean.TRUE);
		commands_enabled.put("get", Boolean.TRUE);
		commands_enabled.put("herd", Boolean.TRUE);
		commands_enabled.put("lick", Boolean.FALSE);
		commands_enabled.put("ping", Boolean.TRUE);
		commands_enabled.put("search", Boolean.TRUE);
		commands_enabled.put("sing", Boolean.TRUE);
		commands_enabled.put("summon", Boolean.TRUE);
		commands_enabled.put("woot", Boolean.TRUE);
	}

	public boolean amusement_chatter_available() {
		return chatter_enabled.get("get") || chatter_enabled.get("eightball") || chatter_enabled.get("fridge")
			|| chatter_enabled.get("defenestrate") || chatter_enabled.get("sing") || chatter_enabled.get("foof")
			|| chatter_enabled.get("dance") || chatter_enabled.get("summon") || chatter_enabled.get("creeper")
			|| chatter_enabled.get("search") || chatter_enabled.get("herd") || chatter_enabled.get("banish");
	}

	public void setReactiveChatter(int chatterLevel, int nameMultiplier) {
		this.reactiveChatterLevel = chatterLevel;
		this.chatterNameMultiplier = nameMultiplier;
	}

	public void setRandomChatter(int chatterLevel) {
		this.randomChatterLevel = chatterLevel;
	}

	public void setWarAutoMuzzle(boolean auto_muzzle_wars) {
		this.auto_muzzle_wars = auto_muzzle_wars;
	}

	public void setTwitterTimers(float tweetBucketMax, float tweetBucketChargeRate) {
		this.tweetBucketMax = tweetBucketMax;
		this.tweetBucketChargeRate = tweetBucketChargeRate;

		this.tweetBucket = tweetBucketMax / 2;
		this.twitterTimer = System.currentTimeMillis();
	}

	public void addChatterSetting(String name, Boolean value) {
		this.chatter_enabled.put(name, value);
	}

	public void addCommandSetting(String name, Boolean value) {
		this.commands_enabled.put(name, value);
	}

	public void addTwitterAccount(String name) {
		addTwitterAccount(name, false);
	}

	public void addTwitterAccount(String name, boolean triggerUpdate) {
		this.twitter_accounts.add(name);

		if (triggerUpdate) {
			Tim.twitterstream.addAccount(name, this);
		}
	}

	public void removeTwitterAccount(String name) {
		this.twitter_accounts.remove(name);
		Tim.twitterstream.removeAccount(name, this);
	}

	public void setMuzzleFlag(boolean muzzled, boolean auto) {
		this.muzzled = muzzled;
		this.auto_muzzled = auto;
	}
}
