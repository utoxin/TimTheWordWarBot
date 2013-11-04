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
	public long remaining;
	public long time_to_start;
	public int total_chains;
	public int current_chain;
	public ChannelInfo cdata;

	protected long duration;
	
	private final int db_id;
	private final Channel channel;
	private final User starter;
	private final String name;

	public WordWar( long time, long to_start, int total_chains, int current_chain, String warname, User startingUser, Channel hosting_channel ) {
		this.starter = startingUser;
		this.time_to_start = to_start;
		this.total_chains = total_chains;
		this.current_chain = current_chain;
		this.duration = this.remaining = time;
		this.name = warname;
		this.channel = hosting_channel;
		this.cdata = Tim.db.channel_data.get(hosting_channel.getName().toLowerCase());
		
		db_id = Tim.db.create_war(hosting_channel, startingUser, warname, time, time, to_start, total_chains, current_chain);

		if (this.time_to_start <= 0 && (cdata.muzzled == false || cdata.auto_muzzled)) {
			cdata.setMuzzleFlag(true, true);
		}
	}
	
	public WordWar( long duration, long remaining, long to_start, int total_chains, int current_chain, String warname, User startingUser, Channel hosting_channel, int db_id ) {
		this.starter = startingUser;
		this.time_to_start = to_start;
		this.duration = duration;
		this.remaining = remaining;
		this.total_chains = total_chains;
		this.current_chain = current_chain;
		this.name = warname;
		this.channel = hosting_channel;
		this.cdata = Tim.db.channel_data.get(hosting_channel.getName().toLowerCase());

		this.db_id = db_id;

		if (this.time_to_start <= 0 && (cdata.muzzled == false || cdata.auto_muzzled)) {
			cdata.setMuzzleFlag(true, true);
		}
	}

	protected void endWar() {
		if (db_id > 0) {
			Tim.db.deleteWar(db_id);
		}

		if (cdata.auto_muzzled) {
			cdata.setMuzzleFlag(false, false);
		}
	}

	public void updateDb() {
		Tim.db.update_war(db_id, remaining, time_to_start, current_chain);
	}
	
	public Channel getChannel() {
		return this.channel;
	}

	public long getDuration() {
		return duration;
	}

	public String getName() {
		String nameString;
		
		if (total_chains > 1) {
			nameString = name + " (" + current_chain + " / " + total_chains + ")";
		} else {
			nameString = name;
		}
		
		return nameString + " [" + this.duration / 60 + " Min]";
	}

	public String getName(boolean includeCounter) {
		if (includeCounter) {
			return getName();
		} else {
			return name + " [" + this.duration / 60 + " Min]";
		}
	}
	
	public User getStarter() {
		return starter;
	}

	public long getTime_to_start() {
		return time_to_start;
	}

	public String getDescription() {
		long minutes;
		long seconds;

		String about = "WordWar '" + this.getName() + ":";
		if (this.time_to_start > 0) {
			minutes = this.time_to_start / 60;
			seconds = this.time_to_start % 60;
			about += " starts in ";
		} else {
			minutes = this.remaining / 60;
			seconds = this.remaining % 60;
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
		return about;
	}

	public String getDescriptionWithChannel() {
		return this.getDescription() + " in " + this.channel.getName();
	}
}
