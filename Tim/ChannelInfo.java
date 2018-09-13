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

import java.text.SimpleDateFormat;
import java.util.*;
import org.pircbotx.User;

public class ChannelInfo {
	public String channel;

	HashMap<String, Boolean> chatter_enabled = new HashMap<>(16);
	public HashMap<String, Boolean> commands_enabled = new HashMap<>(16);
	Set<String> twitter_accounts = new HashSet<>(16);
	
	User lastSpeaker;
	long lastSpeakerTime;

	float reactiveChatterLevel;
	float randomChatterLevel;
	float chatterNameMultiplier;
	
	int velociraptorSightings;
	int activeVelociraptors;
	int deadVelociraptors;
	int killedVelociraptors;
	Date lastSighting = new Date();

	long twitterTimer;
	float tweetBucket;
	float tweetBucketMax;
	float tweetBucketChargeRate;

	// TODO : Rework this to check through the current wars for the channel and automatically muzzle if needed
	public boolean muzzled = false;
	public boolean auto_muzzle_wars = true;
	public boolean auto_muzzled = false;
	long muzzledUntil = 0;

	int fox_odds = 75;
	int lights_odds = 100;
	int cheeseburger_odds = 50;
	int test_odds = 100;
	int hug_odds = 100;
	int tissue_odds = 100;
	int aypwip_odds = 100;
	int answer_odds = 65;
	int eightball_odds = 100;
	int soon_odds = 100;
	int velociraptor_odds = 50;
	int groot_odds = 100;

	final int max_lights_odds = 100;
	final int max_fox_odds = 75;
	final int max_cheeseburger_odds = 50;
	final int max_test_odds = 100;
	final int max_hug_odds = 100;
	final int max_tissue_odds = 100;
	final int max_aypwip_odds = 100;
	final int max_answer_odds = 65;
	final int max_eightball_odds = 100;
	final int max_soon_odds = 100;
	final int max_velociraptor_odds = 50;
	final int max_groot_odds = 100;

	public HashMap<String, User> userList = new HashMap<>();

	public ChannelInfo(String channel) {
		this.channel = channel;
	}

	static public HashMap<String, Boolean> getCommandDefaults() {
		HashMap<String, Boolean> command_defaults = new HashMap<>();

		command_defaults.put("attack", Boolean.TRUE);
		command_defaults.put("banish", Boolean.TRUE);
		command_defaults.put("catch", Boolean.TRUE);
		command_defaults.put("chainstory", Boolean.TRUE);
		command_defaults.put("challenge", Boolean.TRUE);
		command_defaults.put("commandment", Boolean.TRUE);
		command_defaults.put("defenestrate", Boolean.TRUE);
		command_defaults.put("dance", Boolean.TRUE);
		command_defaults.put("dice", Boolean.TRUE);
		command_defaults.put("eightball", Boolean.TRUE);
		command_defaults.put("expound", Boolean.TRUE);
		command_defaults.put("foof", Boolean.TRUE);
		command_defaults.put("fridge", Boolean.TRUE);
		command_defaults.put("get", Boolean.TRUE);
		command_defaults.put("herd", Boolean.TRUE);
		command_defaults.put("lick", Boolean.FALSE);
		command_defaults.put("ping", Boolean.TRUE);
		command_defaults.put("search", Boolean.TRUE);
		command_defaults.put("sing", Boolean.TRUE);
		command_defaults.put("summon", Boolean.TRUE);
		command_defaults.put("velociraptor", Boolean.TRUE);
		command_defaults.put("woot", Boolean.TRUE);

		return command_defaults;
	}

	static public HashMap<String, Boolean> getChatterDefaults() {
		HashMap<String, Boolean> chatter_defaults = new HashMap<>();

		chatter_defaults.put("banish", Boolean.TRUE);
		chatter_defaults.put("bored", Boolean.FALSE);
		chatter_defaults.put("catch", Boolean.TRUE);
		chatter_defaults.put("chainstory", Boolean.TRUE);
		chatter_defaults.put("challenge", Boolean.TRUE);
		chatter_defaults.put("dance", Boolean.TRUE);
		chatter_defaults.put("defenestrate", Boolean.TRUE);
		chatter_defaults.put("eightball", Boolean.TRUE);
		chatter_defaults.put("foof", Boolean.TRUE);
		chatter_defaults.put("fridge", Boolean.TRUE);
		chatter_defaults.put("get", Boolean.TRUE);
		chatter_defaults.put("greetings", Boolean.TRUE);
		chatter_defaults.put("helpful_reactions", Boolean.TRUE);
		chatter_defaults.put("herd", Boolean.TRUE);
		chatter_defaults.put("markov", Boolean.TRUE);
		chatter_defaults.put("search", Boolean.TRUE);
		chatter_defaults.put("silly_reactions", Boolean.TRUE);
		chatter_defaults.put("sing", Boolean.TRUE);
		chatter_defaults.put("summon", Boolean.TRUE);
		chatter_defaults.put("velociraptor", Boolean.TRUE);

		return chatter_defaults;
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

	void setDefaultOptions() {
		chatterNameMultiplier = 3;
		reactiveChatterLevel = 5;
		randomChatterLevel = 2;
		
		velociraptorSightings = 0;
		activeVelociraptors = 0;
		deadVelociraptors = 0;
		killedVelociraptors = 0;

		tweetBucketMax = 10;
		tweetBucket = 5;
		tweetBucketChargeRate = 0.5f;

		chatter_enabled.putAll(getChatterDefaults());
		commands_enabled.putAll(getCommandDefaults());
	}

	boolean amusement_chatter_available() {
		return chatter_enabled.get("get") || chatter_enabled.get("eightball") || chatter_enabled.get("fridge")
			|| chatter_enabled.get("defenestrate") || chatter_enabled.get("sing") || chatter_enabled.get("foof")
			|| chatter_enabled.get("dance") || chatter_enabled.get("summon") || chatter_enabled.get("catch")
			|| chatter_enabled.get("search") || chatter_enabled.get("herd") || chatter_enabled.get("banish");
	}

	void recordVelociraptorSighting() {
		recordVelociraptorSighting(1);
	}
	
	void recordVelociraptorSighting(int increment) {
		velociraptorSightings += increment;
		activeVelociraptors += increment;
		lastSighting.setTime(System.currentTimeMillis());
		
		Tim.db.saveChannelSettings(this);
	}

	void recordSwarmKills(int left, int kills) {
		activeVelociraptors -= left;
		killedVelociraptors += kills;

		if (activeVelociraptors < 0) {
			activeVelociraptors = 0;
		}
		
		Tim.db.saveChannelSettings(this);
	}
	
	void recordSwarmDeaths(int deaths) {
		activeVelociraptors -= deaths;

		if (deaths > 0) {
			deadVelociraptors += deaths;
		}

		if (activeVelociraptors < 0) {
			activeVelociraptors = 0;
		}
		
		Tim.db.saveChannelSettings(this);
	}
	
	String getLastSighting() {
		if (velociraptorSightings == 0) {
			return "Never";
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(lastSighting);

		SimpleDateFormat sdf = new SimpleDateFormat("MMM d");

		return sdf.format(lastSighting) + getDayOfMonthSuffix(cal.get(Calendar.DAY_OF_MONTH));
	}
	
	private String getDayOfMonthSuffix(int n) {
		if (n >= 11 && n <= 13) {
			return "th";
		}

		switch (n % 10) {
			case 1:  return "st";
			case 2:  return "nd";
			case 3:  return "rd";
			default: return "th";
		}
	}

	void setReactiveChatter(float chatterLevel, float nameMultiplier) {
		this.reactiveChatterLevel = chatterLevel;
		this.chatterNameMultiplier = nameMultiplier;
	}

	void setRandomChatter(float chatterLevel) {
		this.randomChatterLevel = chatterLevel;
	}

	void setWarAutoMuzzle(boolean auto_muzzle_wars) {
		this.auto_muzzle_wars = auto_muzzle_wars;
	}

	void setTwitterTimers(float tweetBucketMax, float tweetBucketChargeRate) {
		this.tweetBucketMax = tweetBucketMax;
		this.tweetBucketChargeRate = tweetBucketChargeRate;

		this.tweetBucket = tweetBucketMax / 2;
		this.twitterTimer = System.currentTimeMillis();
	}

	void addChatterSetting(String name, Boolean value) {
		this.chatter_enabled.put(name, value);
	}

	void addCommandSetting(String name, Boolean value) {
		this.commands_enabled.put(name, value);
	}

	void addTwitterAccount(String name) {
		addTwitterAccount(name, false);
	}

	void addTwitterAccount(String name, boolean triggerUpdate) {
		this.twitter_accounts.add(name);

		if (triggerUpdate) {
			Tim.twitterStream.addAccount(name, this);
		}
	}

	void removeTwitterAccount(String name) {
		this.twitter_accounts.remove(name);
		Tim.twitterStream.removeAccount(name, this);
	}

	public void setMuzzleFlag(boolean muzzled, boolean auto) {
		setMuzzleFlag(muzzled, auto, 0);
	}

	void setMuzzleFlag(boolean muzzled, boolean auto, long expires) {
		this.muzzled = muzzled;
		this.auto_muzzled = auto;
		this.muzzledUntil = expires;
	}
}
