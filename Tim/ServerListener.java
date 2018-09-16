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

import java.util.regex.Pattern;

import Tim.Data.ChannelInfo;
import Tim.Utility.TagReplacer;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

/**
 *
 * @author Matthew Walker
 */
class ServerListener extends ListenerAdapter {
	@Override
	public void onConnect(ConnectEvent event) {
		String post_identify = Tim.db.getSetting("post_identify");
		if (!"".equals(post_identify)) {
			String[] post_identify_lines = post_identify.split("\n");

			for (String line : post_identify_lines) {
				if (!line.equals("")) event.respond(line);
			}
		}

		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Tim.warticker = WarTicker.getInstance();
		Tim.deidler = DeIdler.getInstance();

		if (!Tim.db.getSetting("twitter_access_key").equals("")) {
			Tim.twitterStream = new TwitterIntegration();
			Tim.twitterStream.startStream();
		}
	}

	@Override
	public void onKick(KickEvent event) {
		if (event.getRecipient() != null && event.getRecipient().getNick().equals(Tim.bot.getNick())) {
			Tim.db.deleteChannel(event.getChannel());
		}
	}

	@Override
	public void onInvite(InviteEvent event) {
		if (event.getUser() != null && !Tim.db.ignore_list.contains(event.getUser().getNick())) {
			Tim.bot.sendIRC().joinChannel(event.getChannel());
		}
	}

	@Override
	public void onPart(PartEvent event) {
		if (event.getUser().equals(Tim.bot.getUserBot())) {
			Tim.channelStorage.channelList.remove(event.getChannel().getName().toLowerCase());
		} else {
			Tim.db.channel_data.get(event.getChannel().getName().toLowerCase()).userList.remove(event.getUser().getNick().toLowerCase());
		}
	}

	@Override
	public void onQuit(QuitEvent event) {
		Tim.db.channel_data.values().stream().filter(cdata -> cdata.userList.containsKey(event.getUser().getNick().toLowerCase())).forEach(cdata -> cdata.userList.remove(event.getUser().getNick().toLowerCase()));
	}

	@Override
	public void onNickChange(NickChangeEvent event) {
		Tim.db.channel_data.values().stream().filter(cdata -> cdata.userList.containsKey(event.getOldNick().toLowerCase())).forEach(cdata -> {
			cdata.userList.remove(event.getOldNick().toLowerCase());
			cdata.userList.put(event.getNewNick().toLowerCase(), event.getUser());
		});
	}

	@Override
	public void onJoin(JoinEvent event) {
		if (event.getUser() != null && event.getUser().equals(Tim.bot.getUserBot())) {
			Tim.channelStorage.channelList.put(event.getChannel().getName().toLowerCase(), event.getChannel().createSnapshot());

			if (!Tim.db.channel_data.containsKey(event.getChannel().getName().toLowerCase())) {
				Tim.db.joinChannel(event.getChannel());
			}

			for (User user : event.getChannel().getUsers()) {
				Tim.db.channel_data.get(event.getChannel().getName().toLowerCase()).userList.put(user.getNick().toLowerCase(), user);
			}

		} else {
			Tim.db.channel_data.get(event.getChannel().getName().toLowerCase()).userList.put(event.getUser().getNick().toLowerCase(), event.getUser());

			ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());
			int warsCount = 0;

			try {
				String message = "";
				if (cdata.chatter_enabled.get("silly_reactions")
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
					&& !Tim.db.soft_ignore_list.contains(event.getUser().getNick().toLowerCase())
				) {
					TagReplacer tagReplacer = new TagReplacer();
					tagReplacer.setDynamicTag("target", event.getUser().getNick());
					message = tagReplacer.doTagReplacment(Tim.db.dynamic_lists.get("greetings").get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("greetings").size())));
				}

				if (cdata.chatter_enabled.get("helpful_reactions")
					&& Tim.warticker.wars.size() > 0
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
				) {
					warsCount = Tim.warticker.wars.stream().filter((wm) -> (wm.getChannel().equalsIgnoreCase(event.getChannel().getName()))).map((_item) -> 1).reduce(warsCount, Integer::sum);

					if (warsCount > 0) {
						boolean plural = warsCount >= 2;
						if (!message.equals("")) {
							message += " ";
						}

						message += "There " + (plural ? "are" : "is") + " " + warsCount + " war" + (plural ? "s" : "")
							+ " currently running in this channel:";
					}
				}

				Thread.sleep(500);
				if (!message.equals("")) {
					event.getChannel().send().message(message);
				}

				if (cdata.chatter_enabled.get("helpful_reactions")
					&& warsCount > 0
					&& !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())
				) {
					Tim.warticker.wars.stream().filter((wm) -> (wm.getChannel().equalsIgnoreCase(event.getChannel().getName()))).forEach(
						(wm) -> event.getChannel().send().message(wm.getDescription())
					);
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
						event.getChannel().send().message(Tim.db.dynamic_lists.get("extra_greetings").get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("extra_greetings").size())));
					} else {
						int velociraptorCount = cdata.activeVelociraptors;
						String velociraptorDate = cdata.getLastSighting();

						event.getChannel().send().message(String.format("This channel has %d active velociraptors! The last one was spotted on %s.", velociraptorCount, velociraptorDate));
					}
				}

				if (cdata.chatter_enabled.get("silly_reactions") && event.getUser().getNick().equalsIgnoreCase("trillian")) {
					Thread.sleep(1000);
					event.getChannel().send().message("All hail the velociraptor queen!");
					Tim.raptors.sighting(event);
				}
			} catch (InterruptedException ex) {
				Tim.printStackTrace(ex);
			}
		}
	}
}
