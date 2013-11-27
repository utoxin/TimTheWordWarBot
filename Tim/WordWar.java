/**
 * This file is part of Timmy, the Wordwar Bot.
 *
 * Timmy is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Timmy is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Timmy. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package Tim;

import org.pircbotx.Channel;
import org.pircbotx.User;

/**
 *
 * @author Marc
 *
 */
public class WordWar {
	public enum WordWarState {
		STARTING_PAUSED, STARTING, RUNNING_PAUSED, RUNNING, COMPLETED, CANCELED
	}

	public WordWarState state = WordWarState.STARTING_PAUSED;

	public long time_remaining;
	public long base_duration;
	public long current_duration;
	
	public boolean randomize_chain_length = true;
	public float chain_length_variation = 0.1f;

	public long break_duration = 0;
	public int total_wars = 1;
	public int current_war = 1;

	public ChannelInfo cdata;

	public int db_id = 0;
	public Channel channel;
	public User starter;
	public String name;

	public WordWar(String name, User starter, Channel channel) {
		this.name = name;
		this.starter = starter;
		this.channel = channel;
		this.cdata = Tim.db.channel_data.get(channel.getName().toLowerCase());
	}

	public void setAttributes(WordWarState state, long time_remaining, long base_duration, long current_duration) {
		this.state = state;
		this.time_remaining = time_remaining;
		this.base_duration = base_duration;
		this.current_duration = current_duration;
	}

	public void setChainAttributes(int total_wars, int current_war, long break_duration, boolean randomize_chain_length, float chain_length_variation) {
		this.total_wars = total_wars;
		this.current_war = current_war;
		this.break_duration = break_duration;
		this.randomize_chain_length = randomize_chain_length;
		this.chain_length_variation = chain_length_variation;
	}

	public boolean start_war() {
		if (state != WordWarState.RUNNING && state != WordWarState.RUNNING_PAUSED) {
			if (randomize_chain_length) {
				if (Tim.rand.nextBoolean()) {
					time_remaining = base_duration + Math.round(base_duration * (chain_length_variation * Tim.rand.nextFloat()));
				} else {
					time_remaining = base_duration - Math.round(base_duration * (chain_length_variation * Tim.rand.nextFloat()));
				}
			} else {
				time_remaining = base_duration;
			}

			current_duration = time_remaining;

			state = WordWarState.RUNNING;

			return true;
		} else {
			return false;
		}
	}
	
	public boolean pause_war() {
		if (state == WordWarState.RUNNING) {
			state = WordWarState.RUNNING_PAUSED;
			return true;
		} else if (state == WordWarState.STARTING) {
			state = WordWarState.STARTING_PAUSED;
			return true;
		} else {
			return false;
		}
	}

	public boolean resume_war() {
		if (state == WordWarState.RUNNING_PAUSED) {
			state = WordWarState.RUNNING;
			return true;
		} else if (state == WordWarState.STARTING_PAUSED) {
			state = WordWarState.STARTING;
			return true;
		} else {
			return false;
		}
	}
	
	protected void endWar(boolean early) {
		if (db_id > 0) {
			state = early ? WordWarState.CANCELED : WordWarState.COMPLETED;
			Tim.db.update_war(this);
		}
	}

	public void db_update_war() {
		if (db_id == 0) {
			db_id = Tim.db.create_war(this);
		} else {
			Tim.db.update_war(this);
		}
	}

	public Channel getChannel() {
		return this.channel;
	}

	public long getDuration() {
		return current_duration;
	}

	public String getName() {
		String nameString;

		if (total_wars > 1) {
			nameString = name + " (" + current_war + " / " + total_wars + ")";
		} else {
			nameString = name;
		}

		return nameString + " [" + getDurationText() + "]";
	}

	public String getName(boolean includeCounter) {
		if (includeCounter) {
			return getName();
		} else {
			return name;
		}
	}

	public String getDurationText() {
		String text = "";
		long hours = 0, minutes = 0, seconds, tmp;

		tmp = current_duration;

		if (tmp > (60 * 60)) {
			hours = tmp / (60 * 60);
			tmp = tmp % (60 * 60);
		}

		if (tmp > 60) {
			minutes = tmp / 60;
			tmp = tmp % 60;
		}

		seconds = tmp;

		if (hours > 0) {
			text += hours + "H ";
		}

		if (minutes > 0 || (seconds > 0 && hours > 0)) {
			text += minutes + "M ";
		}

		if (seconds > 0) {
			text += seconds + "S";
		}

		return text.trim();
	}

	public User getStarter() {
		return starter;
	}

	public long getTime_to_start() {
		return time_remaining;
	}

	public String getDescription() {
		long minutes;
		long seconds;

		String about = "WordWar " + this.getName() + " ";
		
		if (state == WordWarState.CANCELED) {
			about += " was canceled.";
		} else if (state == WordWarState.COMPLETED) {
			about += " was completed.";
		} else {
			if (state == WordWarState.STARTING || state == WordWarState.STARTING_PAUSED) {
				minutes = time_remaining / 60;
				seconds = time_remaining % 60;
				
				if (state == WordWarState.STARTING_PAUSED) {
					about += " (PAUSED) ";
				}
				
				about += " starts in ";
			} else {
				minutes = time_remaining / 60;
				seconds = time_remaining % 60;
				
				if (state == WordWarState.RUNNING_PAUSED) {
					about += " (PAUSED) ";
				}
				
				about += " ends in ";
			}

			if (minutes > 0) {
				about += minutes + " minutes";
				if (seconds > 0) {
					about += " and ";
				}
			}
			if (seconds > 0) {
				about += seconds + " seconds";
			}
		}

		return about;
	}

	public String getDescriptionWithChannel() {
		return this.getDescription() + " in " + this.channel.getName();
	}
}
