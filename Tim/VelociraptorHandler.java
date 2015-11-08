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

import java.util.logging.Level;
import java.util.logging.Logger;

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

		Logger.getLogger(VelociraptorHandler.class.getName()).log(Level.INFO, "Raptor Spotted In ''{0}''", new Object[]{channel.getName()});

		ChannelInfo cdata = Tim.db.channel_data.get(channel.getName().toLowerCase());

		Logger.getLogger(VelociraptorHandler.class.getName()).log(Level.INFO, "Channel Active Raptors: {0}", new Object[]{cdata.activeVelociraptors});

		if (Tim.rand.nextInt(100) < cdata.velociraptor_odds) {
			cdata.recordVelociraptorSighting();

			if (action) {
				event.respond("jots down a note in a red notebook labeled 'Velociraptor Sighting Log'.");
			} else {
				event.respond("Velociraptor sighted! Incident has been logged.");
			}

			cdata.velociraptor_odds--;
			
			if (Tim.rand.nextInt(100) < 10) {
				swarm(cdata.channel);
			}
		}
	}
	
	public void swarm(String channel) {
		ChannelInfo cdata = Tim.db.channel_data.get(channel.toLowerCase());

		Logger.getLogger(VelociraptorHandler.class.getName()).log(Level.INFO, "Channel Active Raptors: {0}", new Object[]{cdata.activeVelociraptors});

		if (cdata.activeVelociraptors > 2 && Tim.rand.nextInt(100) < (Math.log(cdata.activeVelociraptors) * 8)) {
			String attack = Tim.db.getRandomChannelWithVelociraptors(cdata.channel);

			if (!attack.equals("")) {
				ChannelInfo victimCdata = Tim.db.channel_data.get(attack);
				int attackCount = Tim.rand.nextInt(cdata.activeVelociraptors / 2);
				int returnCount = Tim.rand.nextInt(attackCount / 2);

				int defendingCount = victimCdata.activeVelociraptors;
				
				double killPercent = (((double) attackCount / (double) defendingCount) * 25.0) + (Math.log(Tim.rand.nextInt(attackCount)) * 5);

				if (killPercent > 100) {
					killPercent = 100;
				}

				int killCount = (int) (defendingCount * (killPercent / 100.0));
				if (killCount < 0) {
					return;
				}

				cdata.recordSwarmKills(attackCount - returnCount, killCount);
				victimCdata.recordSwarmDeaths(killCount);

				Tim.bot.sendIRC().message(channel, String.format("Suddenly, %d of the velociraptors go charging off to attack a group in %s! "
					+ "After a horrific battle, they manage to kill %d of them, and %d return home!", attackCount, attack, killCount, returnCount));

				if (victimCdata.chatter_enabled.get("velociraptor") && victimCdata.muzzled == false) {
					Tim.bot.sendIRC().message(victimCdata.channel, String.format("A swarm of %d velociraptors suddenly appears from the direction of %s. "
						+ "The local raptors do their best to fight them off, and %d of them die before the swarm disappears.", attackCount, cdata.channel, killCount));
				}
			}
		} else {
			if (cdata.activeVelociraptors > 1) {
				int newCount = Tim.rand.nextInt(cdata.activeVelociraptors / 2);
				
				if (cdata.activeVelociraptors >= 4) {
					newCount -= Tim.rand.nextInt(cdata.activeVelociraptors / 4);
				}

				if (newCount < 1) {
					return;
				}

				cdata.recordVelociraptorSighting(newCount);

				if (newCount > 1) {
					Tim.bot.sendIRC().message(channel, String.format("Something is going on in the swarm... hey, where did those %d baby"
						+ " raptors come from?! Clever girls.", newCount));
				} else {
					Tim.bot.sendIRC().message(channel, "Something is going on in the swarm... hey, where did that baby"
						+ " raptor come from?! Clever girl.");
				}
			} else if (cdata.activeVelociraptors < 0) {
				cdata.activeVelociraptors = 0;
			}
		}
	}
}
