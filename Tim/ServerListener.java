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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;

/**
 *
 * @author Matthew Walker
 */
public class ServerListener extends ListenerAdapter {

	@Override
	public void onConnect(ConnectEvent event) {
		String post_identify = Tim.db.getSetting("post_identify");
		if (!"".equals(post_identify)) {
			event.respond(post_identify);
		}

		Tim.warticker = WarTicker.getInstance();
		Tim.deidler = DeIdler.getInstance();

		if (!Tim.db.getSetting("twitter_access_key").equals("")) {
			Tim.twitterstream = new TwitterIntegration();
			Tim.twitterstream.startStream();
		}
	}

	@Override
	public void onKick(KickEvent event) {
		if (event.getRecipient().getNick().equals(Tim.bot.getNick())) {
			Tim.db.deleteChannel(event.getChannel());
		}
	}

	@Override
	public void onInvite(InviteEvent event) {
		if (!Tim.db.ignore_list.contains(event.getUser())) {
			Tim.bot.sendIRC().joinChannel(event.getChannel());
			if (!Tim.db.channel_data.containsKey(event.getChannel())) {
				Tim.db.joinChannel(Tim.bot.getUserChannelDao().getChannel(event.getChannel()));
			}
		}
	}

	@Override
	public void onJoin(JoinEvent event) {
		if (event.getUser().getNick().equals(Tim.bot.getNick())) {

		} else {
			ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());
			int warscount = 0;

			try {
				String message = "";
				if (cdata.chatter_enabled.get("silly_reactions")
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
					&& !Tim.db.soft_ignore_list.contains(event.getUser().getNick().toLowerCase())					
				) {
					message = String.format(Tim.db.greetings.get(Tim.rand.nextInt(Tim.db.greetings.size())), event.getUser().getNick());
				}

				if (cdata.chatter_enabled.get("helpful_reactions") 
					&& Tim.warticker.wars.size() > 0
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
				) {
					for (Map.Entry<String, WordWar> wm : Tim.warticker.wars.entrySet()) {
						if (wm.getValue().getChannel().equals(event.getChannel())) {
							warscount++;
						}
					}

					if (warscount > 0) {
						boolean plural = warscount >= 2;
						if (!message.equals("")) {
							message += " ";
						}

						message += "There " + (plural ? "are" : "is") + " " + warscount + " war" + (plural ? "s" : "")
							+ " currently running in this channel:";
					}
				}

				Thread.sleep(500);
				if (!message.equals("")) {
					event.getChannel().send().message(message);
				}

				if (cdata.chatter_enabled.get("helpful_reactions") 
					&& warscount > 0
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
				) {
					for (Map.Entry<String, WordWar> wm : Tim.warticker.wars.entrySet()) {
						if (wm.getValue().getChannel().equals(event.getChannel())) {
							event.getChannel().send().message(wm.getValue().getDescription());
						}
					}
				}
				
				if (cdata.chatter_enabled.get("helpful_reactions") && (Pattern.matches("(?i)mib_......", event.getUser().getNick()) || Pattern.matches("(?i)guest.*", event.getUser().getNick()))) {
					Thread.sleep(500);
					event.getChannel().send().message(
						String.format("%s: To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere", event.getUser().getNick()));
				}

				int r = Tim.rand.nextInt(100);

				if (cdata.chatter_enabled.get("silly_reactions") 
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
					&& !Tim.db.soft_ignore_list.contains(event.getUser().getNick().toLowerCase())
					&& r < 15
				) {
					Thread.sleep(500);
					if (Tim.rand.nextBoolean()) {
						r = Tim.rand.nextInt(Tim.db.extra_greetings.size());
						event.getChannel().send().message(Tim.db.extra_greetings.get(r));
					} else {
						int velociraptorCount = Tim.db.getVelociraptorSightingCount(cdata);
						String velociraptorDate = Tim.db.getVelociraptorSightingDate(cdata);
						
						event.getChannel().send().message(String.format("This channel has had %d total velociraptor sightings. The last one was on %s.", velociraptorCount, velociraptorDate));
					}
				}
			} catch (InterruptedException ex) {
				Logger.getLogger(ServerListener.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
