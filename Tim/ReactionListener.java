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
				if (message.toLowerCase().contains("how many lights") && cdata.chatterLevel > 0) {
					bot.sendMessage(event.getChannel(), "There are FOUR LIGHTS!");
				} else if (message.toLowerCase().startsWith("test") && cdata.chatterLevel > 0) {
					event.respond(pickGrade());
				} else if ((message.contains(":(") || message.contains("):")) && cdata.chatterLevel > 0) {
					bot.sendAction(event.getChannel(), "gives " + event.getUser().getNick() + " a hug.");
				} else if (message.contains(":'(") && cdata.chatterLevel > 0) {
					bot.sendAction(event.getChannel(), "passes " + event.getUser().getNick() + " a tissue.");
				} else if (Pattern.matches("(?i).*how do (i|you) (change|set) ?(my|your)? (nick|name).*", message)) {
					event.respond("To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere");
				} else if (Pattern.matches("(?i).*are you (thinking|pondering) what i.*m (thinking|pondering).*", message) && cdata.chatterLevel > 0) {
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
					if (Pattern.matches("(?i)" + Tim.bot.getNick() + ".*[?]", message) && Tim.rand.nextInt(100) < 75 && cdata.chatterLevel > 0) {
						Tim.amusement.eightball(event.getChannel(), event.getUser(), false);
						return;
					}

					this.interact(event.getUser(), event.getChannel(), message, "say");
					if (cdata.doMarkov) {
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
			if (message.toLowerCase().contains("how many lights") && cdata.chatterLevel > 0) {
				bot.sendMessage(event.getChannel(), "There are FOUR LIGHTS!");
			} else if ((message.contains(":(") || message.contains("):")) && cdata.chatterLevel > 0) {
				bot.sendAction(event.getChannel(), "gives " + event.getUser().getNick() + " a hug.");
			} else if (message.toLowerCase().startsWith("tests") && cdata.chatterLevel > 0) {
				event.respond(pickGrade());
			} else if (message.contains(":'(") && cdata.chatterLevel > 0) {
				bot.sendAction(event.getChannel(), "passes " + event.getUser().getNick() + " a tissue.");
			} else if (Pattern.matches("(?i).*how do (i|you) (change|set) ?(my|your)? (nick|name).*", message)) {
				event.respond("To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere");
			} else if (Pattern.matches("(?i).*are you (thinking|pondering) what i.*m (thinking|pondering).*", message) && cdata.chatterLevel > 0) {
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
				if ((Pattern.matches("(?i)" + Tim.bot.getNick() + ".*[?]", message) && Tim.rand.nextInt(100) < 75) && cdata.chatterLevel > 0) {
					Tim.amusement.eightball(event.getChannel(), event.getUser(), false);
					return;
				}

				this.interact(event.getUser(), event.getChannel(), message, "emote");
				if (cdata.doMarkov) {
					Tim.markhov.process_markhov(message, "emote");
				}
			}
		}
	}

	private void interact( User sender, Channel channel, String message, String type ) {
		ChannelInfo cdata = Tim.db.channel_data.get(channel.getName().toLowerCase());

		if (cdata.chatterLevel == 0) {
			return;
		}
		
		long elapsed = System.currentTimeMillis() / 1000 - cdata.chatterTimer;
		long odds = Math.round(Math.sqrt(elapsed) / (6 - cdata.chatterLevel));
		if (odds > (cdata.chatterLevel * 4)) {
			odds = (cdata.chatterLevel * 4);
		}

		if (message.toLowerCase().contains(Tim.bot.getNick().toLowerCase())) {
			odds = odds * cdata.chatterNameMultiplier;
		}

		if (Tim.rand.nextInt(100) < odds) {
			String[] actions;

			cdata.chatterTimer += Tim.rand.nextInt((int) elapsed / cdata.chatterTimeDivisor);

			if (cdata.doMarkov && !cdata.doRandomActions) {
				actions = new String[] {
					"markhov",};
			} else if (cdata.doMarkov && cdata.doRandomActions) {
				actions = new String[] {
					"markhov",
					"markhov",
					"challenge",
					"amusement",
					"amusement",
					"amusement",};
			} else if (!cdata.doMarkov && cdata.doRandomActions) {
				actions = new String[] {
					"challenge",
					"amusement",
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
		}
	}
	
	private String pickGrade() {
		int grade = Tim.rand.nextInt(50) + 50;
		
		if (grade < 60) {
			return "F";
		} else if (grade < 63) {
			return "D-";
		} else if (grade < 67) {
			return "D";
		} else if (grade < 70) {
			return "D+";
		} else if (grade < 73) {
			return "C-";
		} else if (grade < 77) {
			return "C";
		} else if (grade < 80) {
			return "C+";
		} else if (grade < 83) {
			return "B-";
		} else if (grade < 87) {
			return "B";
		} else if (grade < 90) {
			return "B+";
		} else if (grade < 93) {
			return "A-";
		} else if (grade < 97) {
			return "A";
		} else {
			return "A+";
		}
	}
}