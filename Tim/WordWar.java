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
	private Channel channel;
	private User starter;
	private long duration;
	private String name;
	public long remaining;
	public long time_to_start;

	public WordWar( long time, long to_start, String warname, User startingUser, Channel hosting_channel ) {
		this.starter = startingUser;
		this.time_to_start = to_start;
		this.duration = this.remaining = time;
		this.name = warname;
		this.channel = hosting_channel;
	}

	public Channel getChannel() {
		return this.channel;
	}

	public long getDuration() {
		return duration;
	}

	public String getName() {
		return name;
	}

	public User getStarter() {
		return starter;
	}

	public long getTime_to_start() {
		return time_to_start;
	}

	public String getDescription() {
		int count = 0;
		long minutes;
		long seconds;

		count++;
		String about = "WordWar '" + this.getName() + "':";
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
		return this.getDescription() + " in " + this.channel;
	}
}
