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

import java.util.regex.Pattern;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author Matthew Walker
 */
public class ReactionListener extends ListenerAdapter {
	@Override
	public void onMessage(MessageEvent event) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		PircBotX bot = event.getBot();

		if (message.charAt(0) != '$' && message.charAt(0) != '!' ) {
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
			} else if (Pattern.matches("(?i)" + Tim.bot.getNick() + ".*[?]", message)) {
				int r = Tim.rand.nextInt(100);

				if (r < 50) {
					Tim.amusement.eightball(event.getChannel(), event.getUser(), false);
				}
			} else if (Pattern.matches("(?i).*markhov test.*", message)) {
				Tim.sendDelayedMessage(event.getChannel(), Tim.markhov.generate_markhov("say"), Tim.rand.nextInt(500) + 500);
			}
		}
	}

	@Override
	public void onAction(ActionEvent event) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		PircBotX bot = event.getBot();
		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());

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
			Tim.sendDelayedMessage(event.getChannel(), Tim.markhov.generate_markhov("emote"), Tim.rand.nextInt(500) + 500);
		} else if (Pattern.matches("(?i)" + Tim.bot.getNick() + ".*[?]", message) && Tim.rand.nextInt(100) < 50) {
			Tim.amusement.eightball(event.getChannel(), event.getUser(), false);
		} else {
			this.interact(sender, channel, message, "emote");
			if (cdata.doMarkhov) {
				Tim.markhov.process_markhov(message, "emote");
			}
		}
	}

	private void interact( String sender, String channel, String message, String type ) {
		ChannelInfo cdata = this.channel_data.get(channel.toLowerCase());

		long elapsed = System.currentTimeMillis() / 1000 - cdata.chatterTimer;
		long odds = (long) Math.log(elapsed) * cdata.chatterTimeMultiplier;
		if (odds > cdata.chatterMaxBaseOdds) {
			odds = cdata.chatterMaxBaseOdds;
		}

		if (message.toLowerCase().contains(this.getNick().toLowerCase())) {
			odds = odds * cdata.chatterNameMultiplier;
		}

		if (this.rand.nextInt(100) < odds) {
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
					"amusement",
				};
			} else if (cdata.doMarkhov && cdata.doRandomActions) {
				actions = new String[] {
					"challenge",
					"amusement",
					"amusement",
				};
			} else {
				return;
			}

			String action = actions[rand.nextInt(actions.length)];
			
			if ("markhov".equals(action)) {
				markhov.randomAction(sender, channel, message, type);
			} else if ("challenge".equals(action)) {
				challenge.randomAction(sender, channel, message, type);
			} else if ("amusement".equals(action)) {
				amusement.randomAction(sender, channel, message, type);
			}

			cdata.chatterTimer += this.rand.nextInt((int) elapsed / cdata.chatterTimeDivisor);
			channelLog("Chattered On: " + cdata.Name + "   Odds: " + Long.toString(odds) + "%");
		}
	}
}
