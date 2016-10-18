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
	HashMap<String, Boolean> commands_enabled = new HashMap<>(16);
	Set<String> twitter_accounts = new HashSet<>(16);
	
	User lastSpeaker;
	long lastSpeakerTime;

	public int reactiveChatterLevel;
	public int randomChatterLevel;
	public int chatterNameMultiplier;
	
	int velociraptorSightings;
	int activeVelociraptors;
	int deadVelociraptors;
	int killedVelociraptors;
	Date lastSighting = new Date();

	long twitterTimer;
	float tweetBucket;
	float tweetBucketMax;
	float tweetBucketChargeRate;

	boolean muzzled = false;
	boolean auto_muzzle_wars = true;
	boolean auto_muzzled = false;
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
	int velociraptor_odds = 100;
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
	final int max_velociraptor_odds = 100;
	final int max_groot_odds = 100;

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

		chatter_enabled.put("banish", Boolean.TRUE);
		chatter_enabled.put("bored", Boolean.FALSE);
		chatter_enabled.put("catch", Boolean.TRUE);
		chatter_enabled.put("chainstory", Boolean.TRUE);
		chatter_enabled.put("challenge", Boolean.TRUE);
		chatter_enabled.put("dance", Boolean.TRUE);
		chatter_enabled.put("defenestrate", Boolean.TRUE);
		chatter_enabled.put("eightball", Boolean.TRUE);
		chatter_enabled.put("foof", Boolean.TRUE);
		chatter_enabled.put("fridge", Boolean.TRUE);
		chatter_enabled.put("get", Boolean.TRUE);
		chatter_enabled.put("greetings", Boolean.TRUE);
		chatter_enabled.put("helpful_reactions", Boolean.TRUE);
		chatter_enabled.put("herd", Boolean.TRUE);
		chatter_enabled.put("markov", Boolean.TRUE);
		chatter_enabled.put("search", Boolean.TRUE);
		chatter_enabled.put("silly_reactions", Boolean.TRUE);
		chatter_enabled.put("sing", Boolean.TRUE);
		chatter_enabled.put("summon", Boolean.TRUE);
		chatter_enabled.put("velociraptor", Boolean.TRUE);

		commands_enabled.put("attack", Boolean.TRUE);
		commands_enabled.put("banish", Boolean.TRUE);
		commands_enabled.put("catch", Boolean.TRUE);
		commands_enabled.put("chainstory", Boolean.TRUE);
		commands_enabled.put("challenge", Boolean.TRUE);
		commands_enabled.put("commandment", Boolean.TRUE);
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
		commands_enabled.put("velociraptor", Boolean.TRUE);
		commands_enabled.put("woot", Boolean.TRUE);
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

	public void setReactiveChatter(int chatterLevel, int nameMultiplier) {
		this.reactiveChatterLevel = chatterLevel;
		this.chatterNameMultiplier = nameMultiplier;
	}

	public void setRandomChatter(int chatterLevel) {
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
			Tim.twitterstream.addAccount(name, this);
		}
	}

	void removeTwitterAccount(String name) {
		this.twitter_accounts.remove(name);
		Tim.twitterstream.removeAccount(name, this);
	}

	void setMuzzleFlag(boolean muzzled, boolean auto) {
		setMuzzleFlag(muzzled, auto, 0);
	}

	void setMuzzleFlag(boolean muzzled, boolean auto, long expires) {
		this.muzzled = muzzled;
		this.auto_muzzled = auto;
		this.muzzledUntil = expires;
	}
}
