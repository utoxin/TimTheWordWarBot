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
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author mwalker
 */
public class VelociraptorHandler {
	public void sighting(Event event) {
		Channel channel;
		boolean action = false;
		
		if (event instanceof MessageEvent) {
			channel = ((MessageEvent) event).getChannel();
		} else if (event instanceof ActionEvent) {
			channel = ((ActionEvent) event).getChannel();
			action = true;
		} else {
			return;
		}
		
		ChannelInfo cdata = Tim.db.channel_data.get(channel.getName().toLowerCase());

		if (Tim.rand.nextInt(100) < cdata.velociraptor_odds) {
			cdata.recordVelociraptorSighting();

			if (action) {
				event.respond("jots down a note in a red notebook labeled 'Velociraptor Sighting Log'.");
			} else {
				event.respond("Velociraptor sighted! Incident has been logged.");
			}

			cdata.velociraptor_odds--;

			if (cdata.activeVelociraptors > 10) {
				int odds = (cdata.activeVelociraptors - 10);
				if (odds > 50) {
					odds = 50;
				}

				if (Tim.rand.nextInt(100) < odds) {
					String attack = Tim.db.getRandomChannelWithVelociraptors(cdata.channel);

					if (!attack.equals("")) {
						ChannelInfo victimCdata = Tim.db.channel_data.get(attack);
						int attackCount = Tim.rand.nextInt(cdata.activeVelociraptors / 2);
						int defendingCount = Tim.rand.nextInt(victimCdata.activeVelociraptors / 2) + (victimCdata.activeVelociraptors / 2);
						double killPercent = (((double) attackCount / (double) defendingCount) * 25.0) + Tim.rand.nextInt(attackCount);

						if (killPercent > 100) {
							killPercent = 100;
						}

						int killCount = (int) (defendingCount * (killPercent / 100.0));

						cdata.recordSwarmKills(attackCount, killCount);
						victimCdata.recordSwarmDeaths(killCount);

						channel.send().message(String.format("Suddenly, %d of the velociraptors go charging off to attack a group in %s! "
							+ "After a horrific battle, they manage to kill %d of them...", attackCount, attack, killCount));

						if (victimCdata.chatter_enabled.get("velociraptor") && victimCdata.muzzled == false) {
							Tim.bot.sendIRC().message(victimCdata.channel, String.format("A swarm of %d velociraptors suddenly appears from the direction of %s. "
								+ "The local raptors do their best to fight them off, and %d of them die before the swarm disappears.", attackCount, cdata.channel, killCount));
						}
					}
				}
			}
		}
	}
}
