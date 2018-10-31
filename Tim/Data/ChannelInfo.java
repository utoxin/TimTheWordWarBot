package Tim.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import Tim.Tim;
import org.pircbotx.User;

public class ChannelInfo {
	// Max Odds
	public final int                      max_lights_odds       = 100;
	public final int                      max_fox_odds          = 75;
	public final int                      max_cheeseburger_odds = 50;
	public final int                      max_test_odds         = 100;
	public final int                      max_hug_odds          = 100;
	public final int                      max_tissue_odds       = 100;
	public final int                      max_aypwip_odds       = 100;
	public final int                      max_answer_odds       = 65;
	public final int                      max_eightball_odds    = 100;
	public final int                      max_soon_odds         = 100;
	public final int                      max_groot_odds        = 100;
	public       String                   channel;
	public       HashMap<String, Boolean> chatter_enabled       = new HashMap<>();
	public       HashMap<String, Boolean> commands_enabled      = new HashMap<>();
	public       Set<String>              twitter_accounts      = new HashSet<>();
	public       User                     lastSpeaker;
	public       long                     lastSpeakerTime;
	public       float                    reactiveChatterLevel;
	public       float                    randomChatterLevel;
	public       float                    chatterNameMultiplier;
	public       int                      velociraptorSightings;
	public       int                      activeVelociraptors;
	public       int                      deadVelociraptors;
	public       int                      killedVelociraptors;
	// Twitter related data
	public       long                     twitterTimer;
	public       float                    tweetBucket;
	public       float                    tweetBucketMax;
	public       float                    tweetBucketChargeRate;
	public       boolean                  auto_muzzle_wars      = true;
	// Current Odds
	public       int                      fox_odds              = 75;
	public       int                      lights_odds           = 100;
	public       int                      cheeseburger_odds     = 50;
	public       int                      test_odds             = 100;
	public       int                      hug_odds              = 100;
	public       int                      tissue_odds           = 100;
	public       int                      aypwip_odds           = 100;
	public       int                      answer_odds           = 65;
	public       int                      eightball_odds        = 100;
	public       int                      soon_odds             = 100;
	public       int                      groot_odds            = 100;
	public       int                      raptorStrengthBoost   = 0;
	public       HashMap<String, User>    userList              = new HashMap<>();
	public       boolean                  muzzled               = false;
	public       long                     muzzledUntil          = 0;
	public String lastWarId = "";
	public String newestWarId = "";

	public ChannelInfo(String channel) {
		this.channel = channel;
	}

	@Override
	public String toString() {
		StringBuilder result   = new StringBuilder();
		String        NEW_LINE = System.getProperty("line.separator");

		result.append(this.getClass()
						  .getName())
			  .append(" Object {")
			  .append(NEW_LINE);
		result.append(" Channel: ")
			  .append(channel)
			  .append(NEW_LINE);
		result.append(" Chatter Settings: ")
			  .append(chatter_enabled)
			  .append(NEW_LINE);
		result.append(" Command Settings: ")
			  .append(commands_enabled)
			  .append(NEW_LINE);
		result.append(" Twitter Accounts: ")
			  .append(twitter_accounts)
			  .append(NEW_LINE);
		result.append(" Chatter Name *: ")
			  .append(chatterNameMultiplier)
			  .append(NEW_LINE);
		result.append(" Twitter Timer: ")
			  .append(twitterTimer)
			  .append(NEW_LINE);
		result.append(" Twitter Bucket: ")
			  .append(tweetBucket)
			  .append(NEW_LINE);
		result.append(" Twitter Bucket Max: ")
			  .append(tweetBucketMax)
			  .append(NEW_LINE);
		result.append(" Twitter Bucket Charge Rate: ")
			  .append(tweetBucketChargeRate)
			  .append(NEW_LINE);
		result.append(" Muzzled: ")
			  .append(muzzled)
			  .append(NEW_LINE);
		result.append(" Auto Muzzle Wars: ")
			  .append(auto_muzzle_wars)
			  .append(NEW_LINE);

		result.append("}");

		return result.toString();
	}

	public void setDefaultOptions() {
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
		chatter_defaults.put("groot", Boolean.TRUE);

		return chatter_defaults;
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

	public boolean amusement_chatter_available() {
		return chatter_enabled.get("get") || chatter_enabled.get("eightball") || chatter_enabled.get("fridge") || chatter_enabled.get("defenestrate")
			   || chatter_enabled.get("sing") || chatter_enabled.get("foof") || chatter_enabled.get("dance") || chatter_enabled.get("summon")
			   || chatter_enabled.get("catch") || chatter_enabled.get("search") || chatter_enabled.get("herd") || chatter_enabled.get("banish");
	}

	public void recordSighting() {
		recordSighting(1);
	}

	public void recordSighting(int increment) {
		velociraptorSightings += increment;
		activeVelociraptors += increment;

		Tim.db.saveChannelSettings(this);
	}

	public void recordNewRaptors(int newRaptors) {
		activeVelociraptors += newRaptors;

		Tim.db.saveChannelSettings(this);
	}

	public void recordLeavingRaptors(int leavingRaptors) {
		activeVelociraptors -= leavingRaptors;

		if (activeVelociraptors < 0) {
			activeVelociraptors = 0;
		}

		Tim.db.saveChannelSettings(this);
	}

	public void recordKills(int kills) {
		killedVelociraptors += kills;

		Tim.db.saveChannelSettings(this);
	}

	public void recordDeaths(int deaths) {
		activeVelociraptors -= deaths;
		deadVelociraptors += deaths;

		if (activeVelociraptors < 0) {
			activeVelociraptors = 0;
		}

		Tim.db.saveChannelSettings(this);
	}

	public void setReactiveChatter(float chatterLevel, float nameMultiplier) {
		this.reactiveChatterLevel = chatterLevel;
		this.chatterNameMultiplier = nameMultiplier;
	}

	public void setRandomChatter(float chatterLevel) {
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
			Tim.twitterStream.addAccount(name, this);
		}
	}

	public void removeTwitterAccount(String name) {
		this.twitter_accounts.remove(name);
		Tim.twitterStream.removeAccount(name, this);
	}

	public void setMuzzleFlag(boolean muzzled) {
		setMuzzleFlag(muzzled, 0);
	}

	public void setMuzzleFlag(boolean muzzled, long expires) {
		this.muzzled = muzzled;
		this.muzzledUntil = expires;
		Tim.db.saveChannelSettings(this);
	}

	public boolean isMuzzled() {
		boolean automuzzle = false;

		for (WordWar war : Tim.warticker.wars) {
			if (war.channel.equalsIgnoreCase(this.channel) && this.auto_muzzle_wars) {
				automuzzle = true;
				break;
			}
		}

		return this.muzzled || automuzzle;
	}

	public void clearTimedMuzzle() {
		if (this.muzzled && this.muzzledUntil > 0 && this.muzzledUntil < (System.currentTimeMillis() / 1000)) {
			this.muzzled = false;
			this.muzzledUntil = 0;
		}
	}
}
