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

import org.pircbotx.Channel;
import org.pircbotx.User;

/**
 *
 * @author Marc
 *
 */
class WordWar {

	long remaining;
	long time_to_start;
	int total_chains;
	int current_chain;
	public ChannelInfo cdata;

	long duration;
	long base_duration;
	long break_duration;
	boolean do_randomness;

	private final int db_id;
	private final Channel channel;
	private final User starter;
	private final String name;

	WordWar(long time, long to_start, int total_chains, int current_chain, int break_duration, boolean do_randomness, String warname, User startingUser, Channel hosting_channel) {
		this.starter = startingUser;
		this.time_to_start = to_start;
		this.total_chains = total_chains;
		this.current_chain = current_chain;
		this.base_duration = time;
		this.break_duration = break_duration;
		this.do_randomness = do_randomness;
		this.name = warname;
		this.channel = hosting_channel;
		this.cdata = Tim.db.channel_data.get(hosting_channel.getName().toLowerCase());

		if (total_chains > 1 && do_randomness) {
			this.duration = base_duration + (Tim.rand.nextInt((int) Math.floor(base_duration * 0.2))) - ((long) Math.floor(base_duration * 0.1));
		} else {
			this.duration = this.base_duration;
		}

		this.remaining = this.duration;

		db_id = Tim.db.create_war(hosting_channel, startingUser, warname, base_duration, duration, remaining, to_start, total_chains, current_chain, break_duration, do_randomness);

		if (this.time_to_start <= 0 && (!cdata.muzzled || cdata.auto_muzzled)) {
			cdata.setMuzzleFlag(true, true);
		}
	}

	WordWar(long base_duration, long duration, long remaining, long to_start, int total_chains, int current_chain, int break_duration, boolean do_randomness, String warname, User startingUser, Channel hosting_channel, int db_id) {
		this.starter = startingUser;
		this.time_to_start = to_start;
		this.base_duration = base_duration;
		this.duration = duration;
		this.remaining = remaining;
		this.total_chains = total_chains;
		this.current_chain = current_chain;
		this.break_duration = break_duration;
		this.do_randomness = do_randomness;
		this.name = warname;
		this.channel = hosting_channel;
		this.cdata = Tim.db.channel_data.get(hosting_channel.getName().toLowerCase());

		if (this.base_duration == 0) {
			this.base_duration = duration;
		}

		this.db_id = db_id;

		if (this.time_to_start <= 0 && (!cdata.muzzled || cdata.auto_muzzled)) {
			cdata.setMuzzleFlag(true, true);
		}
	}

	void endWar() {
		if (db_id > 0) {
			Tim.db.deleteWar(db_id);
		}

		if (cdata.auto_muzzled) {
			cdata.setMuzzleFlag(false, false);
		}
	}

	void updateDb() {
		Tim.db.update_war(db_id, duration, remaining, time_to_start, current_chain);
	}

	public Channel getChannel() {
		return this.channel;
	}

	public String getName() {
		String nameString;

		if (total_chains > 1) {
			nameString = name + " (" + current_chain + " / " + total_chains + ")";
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

	private String getDurationText() {
		String text = "";
		long hours = 0, minutes = 0, seconds, tmp;

		tmp = duration;

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

	User getStarter() {
		return starter;
	}

	String getDescription() {
		long minutes;
		long seconds;

		String about = "WordWar " + this.getName() + ":";
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

	String getDescriptionWithChannel() {
		return this.getDescription() + " in " + this.channel.getName();
	}
}
