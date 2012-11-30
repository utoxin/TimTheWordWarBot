/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Tim;

import java.util.HashMap;
import org.pircbotx.Channel;

/**
 *
 * @author mwalker
 */
public class ChannelInfo {
	public Channel channel;
	public long chatterTimer;
	public int chatterMaxBaseOdds;
	public int chatterNameMultiplier;
	public int chatterTimeMultiplier;
	public int chatterTimeDivisor;
	public int chatterLevel;
	
	/**
	 * Flag maps for storing various channel settings.
	 * 
	 * These are used to store the various settings for channels. The keys are command names or twitter accounts
	 * and if the boolean is true, it will function for this channel. If the boolean is false, it will not. There
	 * is also support for a 'default' key that will be used if no matching hash is found.
	 */
	public HashMap<String, Boolean> command_flags = new HashMap<String, Boolean>();
	public HashMap<String, Boolean> reaction_flags = new HashMap<String, Boolean>();
	public HashMap<String, Boolean> mute_flags = new HashMap<String, Boolean>();
	public HashMap<String, Boolean> idle_flags = new HashMap<String, Boolean>();
	public HashMap<String, Boolean> twitter_relays = new HashMap<String, Boolean>();

	/**
	 * Used for temporarily muzzling Timmy in a channel.
	 * 
	 * If this is is greater than the current epoch, none of his reactions or idlers will function. Direct command 
	 * usage will not be affected.
	 */
	public long muzzledUntil = 0;

	/**
	 * Construct channel with default flags.
	 *
	 * @param name What is the name of the channel
	 */
	public ChannelInfo( Channel channel ) {
		this.channel = channel;
	}

	public void updateFlag(String set, String key, boolean value) {
		updateFlag(set, key, value, true);
	}

	public void updateFlag(String set, String key, boolean value, boolean updateDB) {
		if ("command".equals(set)) {
			command_flags.put(key, value);
		} else if ("reaction".equals(set)) {
			reaction_flags.put(key, value);
		} else if ("mute".equals(set)) {
			mute_flags.put(key, value);
		} else if ("idle".equals(set)) {
			idle_flags.put(key, value);
		} else if ("twitter".equals(set)) {
			twitter_relays.put(key, value);
		}

		if (updateDB) {
			Tim.db.updateChannelFlag(channel, set, key, value);
		}
	}

	public boolean checkFlag(String set, String key) {
		boolean muted = muzzledUntil > (System.currentTimeMillis() / 1000);

		if ("command".equals(set)) {
			if (muted) {
				if (mute_flags.get(key) == null) {
					if (mute_flags.get("default") == null) {
						updateFlag("mute", "default", false);
					}
					return mute_flags.get("default");
				} else {
					return mute_flags.get(key);
				}
			} else {
				if (command_flags.get(key) == null) {
					if (command_flags.get("default") == null) {
						updateFlag(set, "default", true);
					}
					return command_flags.get("default");
				} else {
					return command_flags.get(key);
				}
			}
		} else if ("reaction".equals(set)) {
			if (reaction_flags.get(key) == null) {
				if (reaction_flags.get("default") == null) {
					updateFlag(set, "default", true);
				}
				return reaction_flags.get("default");
			} else {
				return reaction_flags.get(key);
			}
		} else if ("idle".equals(set)) {
			if (idle_flags.get(key) == null) {
				if (idle_flags.get("default") == null) {
					updateFlag(set, "default", true);
				}
				return idle_flags.get("default");
			} else {
				return idle_flags.get(key);
			}
		} else if ("twitter".equals(set)) {
			if (twitter_relays.get(key) == null) {
				if (twitter_relays.get("default") == null) {
					updateFlag(set, "default", false);
				}
				return twitter_relays.get("default");
			} else {
				return twitter_relays.get(key);
			}
		}

		return true;
	}
	
	public void setChatterTimers( int maxBaseOdds, int nameMultiplier, int timeMultiplier, int timeDivisor, int chatterLevel ) {
		this.chatterMaxBaseOdds = maxBaseOdds;
		this.chatterNameMultiplier = nameMultiplier;
		this.chatterTimeMultiplier = timeMultiplier;
		this.chatterTimeDivisor = timeDivisor;
		this.chatterLevel = chatterLevel;

		if (this.chatterMaxBaseOdds == 0) {
			this.chatterMaxBaseOdds = 20;
		}

		if (this.chatterNameMultiplier == 0) {
			this.chatterNameMultiplier = 4;
		}

		if (this.chatterTimeMultiplier == 0) {
			this.chatterTimeMultiplier = 4;
		}

		if (this.chatterTimeDivisor == 0) {
			this.chatterTimeDivisor = 2;
		}
		
		this.chatterTimer = System.currentTimeMillis() / 1000;
	}
}
