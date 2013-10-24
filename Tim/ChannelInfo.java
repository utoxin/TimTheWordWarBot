/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Tim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.pircbotx.Channel;

/**
 *
 * @author Matthew Walker
 */
public class ChannelInfo {

	public Channel channel;

	public HashMap<String, Boolean> chatter_enabled = new HashMap<>(16);
	public HashMap<String, Boolean> commands_enabled = new HashMap<>(16);
	public Set<String> twitter_accounts = new HashSet<>(16);

	public long chatterTimer;
	public int chatterNameMultiplier;
	public int chatterLevel;

	public long twitterTimer;
	public float tweetBucket;
	public float tweetBucketMax;
	public float tweetBucketChargeRate;
	
	public boolean muzzled;
	public boolean auto_muzzled;

	public ChannelInfo(Channel channel) {
		this.channel = channel;
		this.muzzled = false;
		this.auto_muzzled = false;
	}

	public void setDefaultOptions() {
		chatterNameMultiplier = 3;
		chatterLevel = 1;
		
		tweetBucketMax = 10;
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

		commands_enabled.put("eightball", Boolean.TRUE);
		commands_enabled.put("expound", Boolean.TRUE);
		commands_enabled.put("dice", Boolean.TRUE);
		commands_enabled.put("woot", Boolean.TRUE);
		commands_enabled.put("get", Boolean.TRUE);
		commands_enabled.put("fridge", Boolean.TRUE);
		commands_enabled.put("dance", Boolean.TRUE);
		commands_enabled.put("lick", Boolean.FALSE);
		commands_enabled.put("commandment", Boolean.TRUE);
		commands_enabled.put("defenestrate", Boolean.TRUE);
		commands_enabled.put("summon", Boolean.TRUE);
		commands_enabled.put("foof", Boolean.TRUE);
		commands_enabled.put("creeper", Boolean.TRUE);
		commands_enabled.put("search", Boolean.TRUE);
		commands_enabled.put("challenge", Boolean.TRUE);
		commands_enabled.put("chainstory", Boolean.TRUE);
	}

	public boolean amusement_chatter_available() {
		return chatter_enabled.get("get") || chatter_enabled.get("eightball") || chatter_enabled.get("fridge")
				|| chatter_enabled.get("defenestrate") || chatter_enabled.get("sing") || chatter_enabled.get("foof")
				|| chatter_enabled.get("dance") || chatter_enabled.get("summon") || chatter_enabled.get("creeper")
				|| chatter_enabled.get("search");
	}

	public void setChatterTimers( int nameMultiplier, int chatterLevel ) {
		this.chatterNameMultiplier = nameMultiplier;
		this.chatterLevel = chatterLevel;

		this.chatterTimer = System.currentTimeMillis() / 1000;
	}
	
	public void setChatterLevel( int chatterLevel ) {
		this.chatterLevel = chatterLevel;

		this.chatterTimer = System.currentTimeMillis() / 1000;
	}

	public void setTwitterTimers(float tweetBucketMax, float tweetBucketChargeRate) {
		this.tweetBucketMax = tweetBucketMax;
		this.tweetBucketChargeRate = tweetBucketChargeRate;

		this.tweetBucket = 0;
		this.twitterTimer = System.currentTimeMillis() / 1000;
	}
	
	public void addChatterSetting(String name, Boolean value) {
		this.chatter_enabled.put(name, value);
	}
	
	public void addCommandSetting(String name, Boolean value) {
		this.commands_enabled.put(name, value);
	}
	
	public void addTwitterAccount(String name) {
		this.twitter_accounts.add(name);
	}
	
	public void removeTwitterAccount(String name) {
		this.twitter_accounts.remove(name);
	}
	
	public void setMuzzleFlag(boolean muzzled, boolean auto) {
		this.muzzled = muzzled;
		this.auto_muzzled = auto;
	}
}
