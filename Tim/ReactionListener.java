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
	private int lights_odds = 100;
	private int fox_odds = 75;
	private int cheeseburger_odds = 50;
	private int test_odds = 100;
	private int hug_odds = 100;
	private int tissue_odds = 100;
	private int aypwip_odds = 100;
	private int eightball_odds = 100;

	private final int max_lights_odds = 100;
	private final int max_fox_odds = 75;
	private final int max_cheeseburger_odds = 50;
	private final int max_test_odds = 100;
	private final int max_hug_odds = 100;
	private final int max_tissue_odds = 100;
	private final int max_aypwip_odds = 100;
	private final int max_eightball_odds = 100;
	
	@Override
	public void onMessage( MessageEvent event ) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		PircBotX bot = event.getBot();
		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());

		if (cdata.muzzled) {
			return;
		}
		
		if (Tim.rand.nextInt(500) == 0) {
			if (lights_odds < max_lights_odds) {
				lights_odds++;
			}
			if (fox_odds < max_fox_odds && Tim.rand.nextBoolean()) {
				fox_odds++;
			}
			if (cheeseburger_odds < max_cheeseburger_odds && Tim.rand.nextBoolean()) {
				cheeseburger_odds++;
			}
			if (test_odds < max_test_odds) {
				test_odds++;
			}
			if (hug_odds < max_hug_odds && Tim.rand.nextBoolean()) {
				hug_odds++;
			}
			if (tissue_odds < max_tissue_odds && Tim.rand.nextBoolean()) {
				tissue_odds++;
			}
			if (aypwip_odds < max_aypwip_odds) {
				aypwip_odds++;
			}
			if (eightball_odds < max_eightball_odds) {
				eightball_odds++;
			}
		}

		if (cdata.chatterLevel > 0 && !Tim.db.ignore_list.contains(event.getUser().getNick())) {
			if (message.charAt(0) != '$' && message.charAt(0) != '!') {
				if (message.toLowerCase().contains("how many lights")) {
					if (Tim.rand.nextInt(100) < lights_odds) {
						bot.sendMessage(event.getChannel(), "There are FOUR LIGHTS!");
						lights_odds-=5;
					}
				} else if (message.toLowerCase().contains("what does the fox say")) {
					if (Tim.rand.nextInt(100) < fox_odds) {
						event.respond("Foxes don't talk. Sheesh.");
						fox_odds-=5;
					}
				} else if (message.toLowerCase().contains("cheeseburger")) {
					if (Tim.rand.nextInt(100) < cheeseburger_odds) {
						event.respond("I can has cheezburger?");
						cheeseburger_odds-=5;
					}
				} else if (message.toLowerCase().startsWith("test")) {
					if (Tim.rand.nextInt(100) < test_odds) {
						event.respond("After due consideration, your test earned a: " + pickGrade());
						test_odds-=5;
					}
				} else if ((message.contains(":(") || message.contains("):"))) {
					if (Tim.rand.nextInt(100) < hug_odds) {
						bot.sendAction(event.getChannel(), "gives " + event.getUser().getNick() + " a hug.");
						hug_odds-=5;
					}
				} else if (message.contains(":'(")) {
					if (Tim.rand.nextInt(100) < tissue_odds) {
						bot.sendAction(event.getChannel(), "passes " + event.getUser().getNick() + " a tissue.");
						tissue_odds-=5;
					}
				} else if (Pattern.matches("(?i).*how do (i|you) (change|set) ?(my|your)? (nick|name).*", message)) {
					event.respond("To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere");
				} else if (Pattern.matches("(?i).*are you (thinking|pondering) what i.*m (thinking|pondering).*", message)) {
					if (Tim.rand.nextInt(100) < aypwip_odds) {
						int i = Tim.rand.nextInt(Tim.amusement.aypwips.size());
						Tim.bot.sendMessage(event.getChannel(), String.format(Tim.amusement.aypwips.get(i), event.getUser().getNick()));
						aypwip_odds-=5;
					}
				} else if (Pattern.matches("(?i)" + Tim.bot.getNick() + ".*[?]", message)) {
					if (Tim.rand.nextInt(100) < eightball_odds) {
						Tim.amusement.eightball(event.getChannel(), event.getUser(), false);
						eightball_odds-=5;
					}
				} else {
					this.interact(event.getUser(), event.getChannel(), message, "say");
					if (cdata.doMarkov) {
						Tim.markov.process_markov(message, "say");
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

		if (cdata.muzzled) {
			return;
		}
		
		if (Tim.rand.nextInt(500) == 0) {
			if (lights_odds < max_lights_odds) {
				lights_odds++;
			}
			if (fox_odds < max_fox_odds && Tim.rand.nextBoolean()) {
				fox_odds++;
			}
			if (cheeseburger_odds < max_cheeseburger_odds && Tim.rand.nextBoolean()) {
				cheeseburger_odds++;
			}
			if (test_odds < max_test_odds) {
				test_odds++;
			}
			if (hug_odds < max_hug_odds && Tim.rand.nextBoolean()) {
				hug_odds++;
			}
			if (tissue_odds < max_tissue_odds && Tim.rand.nextBoolean()) {
				tissue_odds++;
			}
			if (aypwip_odds < max_aypwip_odds) {
				aypwip_odds++;
			}
			if (eightball_odds < max_eightball_odds) {
				eightball_odds++;
			}
		}

		if (cdata.chatterLevel > 0 && !Tim.db.ignore_list.contains(event.getUser().getNick())) {
			if (message.toLowerCase().contains("how many lights")) {
				if (Tim.rand.nextInt(100) < lights_odds) {
					bot.sendMessage(event.getChannel(), "There are FOUR LIGHTS!");
					lights_odds-=5;
				}
			} else if (message.toLowerCase().contains("what does the fox say")) {
				if (Tim.rand.nextInt(100) < fox_odds) {
					event.respond("mutters under his breath. \"Foxes don't talk. Sheesh.\"");
					fox_odds-=5;
				}
			} else if (message.toLowerCase().contains("cheeseburger")) {
				if (Tim.rand.nextInt(100) < cheeseburger_odds) {
					event.respond("sniffs the air, and peers around. \"Can has cheezburger?\"");
					cheeseburger_odds-=5;
				}
			} else if ((message.contains(":(") || message.contains("):"))) {
				if (Tim.rand.nextInt(100) < hug_odds) {
					bot.sendAction(event.getChannel(), "gives " + event.getUser().getNick() + " a hug.");
					hug_odds-=5;
				}
			} else if (message.toLowerCase().startsWith("tests")) {
				if (Tim.rand.nextInt(100) < test_odds) {
					event.respond("considers, and gives " + event.getUser().getNick() + " a grade: " + pickGrade());
					test_odds-=5;
				}
			} else if (message.contains(":'(")) {
				if (Tim.rand.nextInt(100) < tissue_odds) {
					bot.sendAction(event.getChannel(), "passes " + event.getUser().getNick() + " a tissue.");
					tissue_odds-=5;
				}
			} else if (Pattern.matches("(?i).*how do (i|you) (change|set) ?(my|your)? (nick|name).*", message)) {
				event.respond("To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere");
			} else if (Pattern.matches("(?i).*are you (thinking|pondering) what i.*m (thinking|pondering).*", message)) {
				if (Tim.rand.nextInt(100) < aypwip_odds) {
					int i = Tim.rand.nextInt(Tim.amusement.aypwips.size());
					Tim.bot.sendMessage(event.getChannel(), String.format(Tim.amusement.aypwips.get(i), event.getUser().getNick()));
					aypwip_odds-=5;
				}
			} else if (Pattern.matches("(?i)" + Tim.bot.getNick() + ".*[?]", message)) {
				if (Tim.rand.nextInt(100) < eightball_odds) {
					Tim.amusement.eightball(event.getChannel(), event.getUser(), false);
					eightball_odds-=5;
				}
			} else {
				this.interact(event.getUser(), event.getChannel(), message, "emote");
				if (cdata.doMarkov) {
					Tim.markov.process_markov(message, "emote");
				}
			}
		}
	}

	private void interact( User sender, Channel channel, String message, String type ) {
		ChannelInfo cdata = Tim.db.channel_data.get(channel.getName().toLowerCase());

		if (cdata.chatterLevel <= 0) {
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

			cdata.chatterTimer += Tim.rand.nextInt((int) elapsed);

			if (cdata.doMarkov && !cdata.doRandomActions) {
				actions = new String[] {
					"markhov",};
			} else if (cdata.doMarkov && cdata.doRandomActions) {
				actions = new String[] {
					"markhov",
					"markhov",
					"challenge",
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
				if ("say".equals(type) && Tim.rand.nextBoolean()) {
					type = "mutter";
				} else if ("emote".equals(type) && Tim.rand.nextBoolean()) {
					type = "mutter";
				}
				Tim.markov.randomAction(channel, type);
			} else if ("challenge".equals(action)) {
				Tim.challenge.randomAction(sender, channel);
			} else if ("amusement".equals(action)) {
				Tim.amusement.randomAction(sender, channel);
			}
		}
	}
	
	private String pickGrade() {
		int grade = Tim.rand.nextInt(50) + 51;
		
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