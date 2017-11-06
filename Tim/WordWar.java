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

import org.pircbotx.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	final Integer db_id;
	private final String channel;
	private final String starter;
	private final String name;

	ConcurrentHashMap<String, Integer> members = new ConcurrentHashMap<>();

	WordWar(long time, long to_start, int total_chains, int current_chain, int break_duration, boolean do_randomness, String warname, User startingUser, String hosting_channel) {
		this.starter = startingUser.getNick();
		this.time_to_start = to_start;
		this.total_chains = total_chains;
		this.current_chain = current_chain;
		this.base_duration = time;
		this.break_duration = break_duration;
		this.do_randomness = do_randomness;
		this.name = warname;
		this.channel = hosting_channel;
		this.cdata = Tim.db.channel_data.get(hosting_channel.toLowerCase());

		if (total_chains > 1 && do_randomness) {
			this.duration = base_duration + (Tim.rand.nextInt((int) Math.floor(base_duration * 0.2))) - ((long) Math.floor(base_duration * 0.1));
		} else {
			this.duration = this.base_duration;
		}

		this.remaining = this.duration;

		db_id = Tim.db.create_war(hosting_channel, startingUser.getNick(), warname, base_duration, duration, remaining, to_start, total_chains, current_chain, break_duration, do_randomness);

		if (this.time_to_start <= 0 && (!cdata.muzzled || cdata.auto_muzzled)) {
			cdata.setMuzzleFlag(true, true);
		}

		if (Tim.warticker != null) {
			Tim.warticker.wars_by_id.put(db_id, this);
		}
	}

	WordWar(long base_duration, long duration, long remaining, long to_start, int total_chains, int current_chain, int break_duration, boolean do_randomness, String warname, String startingUser, String hosting_channel, int db_id) {
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

		String channelName = hosting_channel.toLowerCase();
		this.cdata = Tim.db.channel_data.get(channelName);

		if (this.cdata == null) {
			Logger.getLogger(WordWar.class.getName()).log(Level.INFO, "PANIC!!!!!!");
			Logger.getLogger(WordWar.class.getName()).log(Level.INFO, "Failed To Get ChannelInfo For: "+channelName);
		}

		if (this.base_duration == 0) {
			this.base_duration = duration;
		}

		this.db_id = db_id;

		if (this.time_to_start <= 0 && (!cdata.muzzled || cdata.auto_muzzled)) {
			cdata.setMuzzleFlag(true, true);
		}

		if (Tim.warticker != null) {
			Tim.warticker.wars_by_id.put(db_id, this);
		}
		members = Tim.db.loadWarMembers(db_id);
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
		Tim.db.updateWarMembers(db_id, members);
	}

	public void addMember(String nick, Integer count) {
		members.put(nick, count);
		Tim.db.updateWarMembers(db_id, members);
	}

	public void removeMember(String nick) {
		members.remove(nick);
		Tim.db.updateWarMembers(db_id, members);
	}

	public String getChannel() {
		return this.channel;
	}

	public String getName(boolean includeId, boolean includeDuration, int idFieldWidth, int durationFieldWidth) {
		ArrayList<String> nameParts = new ArrayList<>();

		if (includeId) {
			nameParts.add(String.format("[ID %"+idFieldWidth+"d]", db_id));
		}

		if (includeDuration) {
			nameParts.add(String.format("[%"+durationFieldWidth+"s]", getDurationText(duration)));
		}

		nameParts.add(name);

		if (total_chains > 1) {
			nameParts.add(String.format("(%d/%d)", current_chain, total_chains));
		}

		return String.join(" ", nameParts);
	}

	public String getName(boolean includeId, boolean includeDuration) {
		return getName(includeId, includeDuration, 1, 1);
	}

	public String getSimpleName() {
		return getName(false, false);
	}

	public String getInternalName() {
		return name.toLowerCase();
	}

	public String getDurationText(long duration) {
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

	String getStarter() {
		return starter;
	}

	String getDescription() {
		return getDescription(1, 1);
	}

	String getDescription(int idFieldWidth, int durationFieldWidth) {
		String about = this.getName(true, true, idFieldWidth, durationFieldWidth) + " :: ";
		if (this.time_to_start > 0) {
			about += "Starts In: ";
			about += getDurationText(time_to_start);
		} else {
			about += "Ends In: ";
			about += getDurationText(remaining);
		}

		return about;
	}

	String getDescriptionWithChannel() {
		return this.getDescription(1, 1) + " :: " + this.channel;
	}

	String getDescriptionWithChannel(int idFieldWidth, int durationFieldWidth) {
		return this.getDescription(idFieldWidth, durationFieldWidth) + " :: " + this.channel;
	}
}
