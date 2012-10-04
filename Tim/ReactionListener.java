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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.ServerPingEvent;

/**
 *
 * @author Matthew Walker
 */
public class ReactionListener extends ListenerAdapter {
	@Override
	public void onMessage( MessageEvent event ) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		PircBotX bot = event.getBot();
		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());

		if (!Tim.db.ignore_list.contains(event.getUser().getNick())) {
			if (message.charAt(0) != '$' && message.charAt(0) != '!') {
				if (message.toLowerCase().contains("how many lights")) {
					bot.sendMessage(event.getChannel(), "There are FOUR LIGHTS!");
				} else if (message.contains(":(") || message.contains("):")) {
					bot.sendAction(event.getChannel(), "gives " + event.getUser().getNick() + " a hug.");
				} else if (message.contains(":'(")) {
					bot.sendAction(event.getChannel(), "passes " + event.getUser().getNick() + " a tissue.");
				} else if (Pattern.matches("(?i).*how do i (change|set) my (nick|name).*", message)) {
					event.respond("To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere");
				} else if (message.toLowerCase().contains("are you thinking what i'm thinking") || message.toLowerCase().contains("are you pondering what i'm pondering")) {
					int i = Tim.rand.nextInt(Tim.amusement.aypwips.size());
					Tim.bot.sendMessage(event.getChannel(), String.format(Tim.amusement.aypwips.get(i), event.getUser().getNick()));
				} else if (Pattern.matches("(?i).*markhov test.*", message)) {
					try {
						Thread.sleep(Tim.rand.nextInt(500) + 500);
						Tim.bot.sendMessage(event.getChannel(), Tim.markhov.generate_markhov("say"));
					} catch (InterruptedException ex) {
						Logger.getLogger(ReactionListener.class.getName()).log(Level.SEVERE, null, ex);
					}
				} else {
					if (Pattern.matches("(?i)" + Tim.bot.getNick() + ".*[?]", message) && Tim.rand.nextInt(100) < 50) {
						Tim.amusement.eightball(event.getChannel(), event.getUser(), false);
					}

					this.interact(event.getUser(), event.getChannel(), message, "say");
					if (cdata.doMarkhov) {
						Tim.markhov.process_markhov(message, "say");
					}
				}
			}
		}
	}

	@Override
	public void onAction( ActionEvent event ) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		PircBotX bot = event.getBot();
		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());

		if (Tim.db.admin_list.contains(event.getUser().getNick().toLowerCase())) {
			if (message.equalsIgnoreCase("punches " + Tim.bot.getNick() + " in the face!")) {
				Tim.bot.sendAction(event.getChannel(), "falls over and dies.  x.x");
				Tim.bot.shutdown();
			}
		}

		if (!Tim.db.ignore_list.contains(event.getUser().getNick())) {
			if (message.toLowerCase().contains("how many lights")) {
				bot.sendMessage(event.getChannel(), "There are FOUR LIGHTS!");
			} else if (message.contains(":(") || message.contains("):")) {
				bot.sendAction(event.getChannel(), "gives " + event.getUser().getNick() + " a hug.");
			} else if (message.contains(":'(")) {
				bot.sendAction(event.getChannel(), "passes " + event.getUser().getNick() + " a tissue.");
			} else if (Pattern.matches("(?i).*how do i (change|set) my (nick|name).*", message)) {
				event.respond("To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere");
			} else if (message.toLowerCase().contains("are you thinking what i'm thinking") || message.toLowerCase().contains("are you pondering what i'm pondering")) {
				int i = Tim.rand.nextInt(Tim.amusement.aypwips.size());
				Tim.bot.sendMessage(event.getChannel(), String.format(Tim.amusement.aypwips.get(i), event.getUser().getNick()));
			} else if (Pattern.matches("(?i).*markhov test.*", message)) {
				try {
					Thread.sleep(Tim.rand.nextInt(500) + 500);
					Tim.bot.sendMessage(event.getChannel(), Tim.markhov.generate_markhov("emote"));
				} catch (InterruptedException ex) {
					Logger.getLogger(ReactionListener.class.getName()).log(Level.SEVERE, null, ex);
				}
			} else {
				if (Pattern.matches("(?i)" + Tim.bot.getNick() + ".*[?]", message) && Tim.rand.nextInt(100) < 50) {
					Tim.amusement.eightball(event.getChannel(), event.getUser(), false);
				}

				this.interact(event.getUser(), event.getChannel(), message, "emote");
				if (cdata.doMarkhov) {
					Tim.markhov.process_markhov(message, "emote");
				}
			}
		}
	}

	@Override
	public void onServerPing( ServerPingEvent event ) {
		/**
		 * This loop is used to reduce the chatter odds on idle channels, by periodically triggering idle chatter in
		 * channels. If they currently have chatter turned off, this simply decreases their timer, and then goes on.
		 * That way, the odds don't build up to astronomical levels while people are idle or away, resulting in lots of
		 * spam when they come back.
		 */
		for (ChannelInfo cdata : Tim.db.channel_data.values()) {
			cdata = Tim.db.channel_data.get(cdata.Name);

			long elapsed = System.currentTimeMillis() / 1000 - cdata.chatterTimer;
			long odds = Math.round(Math.sqrt(elapsed) / 3);

			if (odds < cdata.chatterMaxBaseOdds) {
				continue;
			}

			if (Tim.rand.nextInt(100) < odds) {
				String[] actions;

				int newDivisor = cdata.chatterTimeDivisor;
				if (newDivisor > 1) {
					newDivisor -= 1;
				}
				cdata.chatterTimer += Math.round(elapsed / 2);

				if (cdata.doMarkhov && !cdata.doRandomActions) {
					actions = new String[] {
						"markhov"
					};
				} else if (cdata.doMarkhov && cdata.doRandomActions) {
					actions = new String[] {
						"markhov",
						"amusement"
					};
				} else if (!cdata.doMarkhov && cdata.doRandomActions) {
					actions = new String[] {
						"amusement"
					};
				} else {
					continue;
				}

				String action = actions[Tim.rand.nextInt(actions.length)];

				if ("markhov".equals(action)) {
					Tim.markhov.randomAction(Tim.bot.getChannel(cdata.Name), "say");
				} else if ("amusement".equals(action)) {
					Tim.amusement.randomAction(null, Tim.bot.getChannel(cdata.Name));
				}
			}
		}
	}

	private void interact( User sender, Channel channel, String message, String type ) {
		ChannelInfo cdata = Tim.db.channel_data.get(channel.getName().toLowerCase());

		long elapsed = System.currentTimeMillis() / 1000 - cdata.chatterTimer;
		long odds = Math.round(Math.sqrt(elapsed) / 3);
		if (odds > cdata.chatterMaxBaseOdds) {
			odds = cdata.chatterMaxBaseOdds;
		}

		if (message.toLowerCase().contains(Tim.bot.getNick().toLowerCase())) {
			odds = odds * cdata.chatterNameMultiplier;
		}

		if (Tim.rand.nextInt(100) < odds) {
			String[] actions;

			if (cdata.doMarkhov && !cdata.doRandomActions) {
				actions = new String[] {
					"markhov"
				};
			} else if (cdata.doMarkhov && cdata.doRandomActions) {
				actions = new String[] {
					"markhov",
					"challenge",
					"amusement",
					"amusement",};
			} else if (!cdata.doMarkhov && cdata.doRandomActions) {
				actions = new String[] {
					"challenge",
					"amusement",
					"amusement",};
			} else {
				return;
			}

			String action = actions[Tim.rand.nextInt(actions.length)];

			if ("markhov".equals(action)) {
				Tim.markhov.randomAction(channel, type);
			} else if ("challenge".equals(action)) {
				Tim.challenge.randomAction(sender, channel);
			} else if ("amusement".equals(action)) {
				Tim.amusement.randomAction(sender, channel);
			}

			cdata.chatterTimer += Tim.rand.nextInt((int) elapsed / cdata.chatterTimeDivisor);
		}
	}
}
