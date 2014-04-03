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

import java.util.ArrayList;
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
	private int answer_odds = 65;
	private int eightball_odds = 100;

	private final int max_lights_odds = 100;
	private final int max_fox_odds = 75;
	private final int max_cheeseburger_odds = 50;
	private final int max_test_odds = 100;
	private final int max_hug_odds = 100;
	private final int max_tissue_odds = 100;
	private final int max_aypwip_odds = 100;
	private final int max_answer_odds = 65;
	private final int max_eightball_odds = 100;

	@Override
	public void onMessage(MessageEvent event) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		PircBotX bot = event.getBot();
		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());

		if (cdata.muzzled) {
			return;
		}

		if (Tim.rand.nextInt(250) == 0) {
			if (lights_odds < max_lights_odds) {
				lights_odds += Tim.rand.nextInt(5);
			}
			if (fox_odds < max_fox_odds) {
				fox_odds += Tim.rand.nextInt(5);
			}
			if (cheeseburger_odds < max_cheeseburger_odds) {
				cheeseburger_odds += Tim.rand.nextInt(5);
			}
			if (test_odds < max_test_odds) {
				test_odds += Tim.rand.nextInt(5);
			}
			if (hug_odds < max_hug_odds) {
				hug_odds += Tim.rand.nextInt(5);
			}
			if (tissue_odds < max_tissue_odds) {
				tissue_odds += Tim.rand.nextInt(5);
			}
			if (aypwip_odds < max_aypwip_odds) {
				aypwip_odds += Tim.rand.nextInt(5);
			}
			if (answer_odds < max_answer_odds) {
				answer_odds += Tim.rand.nextInt(5);
			}
			if (eightball_odds < max_eightball_odds) {
				eightball_odds += Tim.rand.nextInt(5);
			}
		}

		if (!Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase()) &&
			!Tim.db.soft_ignore_list.contains(event.getUser().getNick().toLowerCase())) {
			if (message.charAt(0) != '$' && message.charAt(0) != '!') {
				if (!message.equals(":(") && !message.equals("):")) {
					cdata.lastSpeaker = event.getUser();
					cdata.lastSpeakerTime = System.currentTimeMillis();
				} else if (cdata.lastSpeakerTime <= (System.currentTimeMillis() - (60 * 1000))) {
					cdata.lastSpeaker = event.getUser();
				}

				if (message.toLowerCase().contains("how many lights") && cdata.chatter_enabled.get("silly_reactions")) {
					if (Tim.rand.nextInt(100) < lights_odds) {
						event.getChannel().send().message("There are FOUR LIGHTS!");
						lights_odds -= Tim.rand.nextInt(5);
					}
				} else if (message.toLowerCase().contains("what does the fox say") && cdata.chatter_enabled.get("silly_reactions")) {
					if (Tim.rand.nextInt(100) < fox_odds) {
						event.respond("Foxes don't talk. Sheesh.");
						fox_odds -= Tim.rand.nextInt(5);
					}
				} else if (message.toLowerCase().contains("cheeseburger") && cdata.chatter_enabled.get("silly_reactions")) {
					if (Tim.rand.nextInt(100) < cheeseburger_odds) {
						event.respond("I can has cheezburger?");
						cheeseburger_odds -= Tim.rand.nextInt(5);
					}
				} else if (message.toLowerCase().startsWith("test") && cdata.chatter_enabled.get("silly_reactions")) {
					if (Tim.rand.nextInt(100) < test_odds) {
						event.respond("After due consideration, your test earned a: " + pickGrade());
						test_odds -= Tim.rand.nextInt(5);
					}
				} else if ((message.contains(":(") || message.contains("):")) && cdata.chatter_enabled.get("silly_reactions")) {
					if (Tim.rand.nextInt(100) < hug_odds) {
						event.getChannel().send().action("gives " + cdata.lastSpeaker.getNick() + " a hug.");
						hug_odds -= Tim.rand.nextInt(5);
					}
				} else if (message.contains(":'(") && cdata.chatter_enabled.get("silly_reactions")) {
					if (Tim.rand.nextInt(100) < tissue_odds) {
						event.getChannel().send().action("passes " + event.getUser().getNick() + " a tissue.");
						tissue_odds -= Tim.rand.nextInt(5);
					}
				} else if (Pattern.matches("(?i).*how do (i|you) (change|set) ?(my|your)? (nick|name).*", message) && cdata.chatter_enabled.get("helpful_reactions")) {
					event.respond("To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere");
				} else if (Pattern.matches("(?i).*are you (thinking|pondering) what i.*m (thinking|pondering).*", message) && cdata.chatter_enabled.get("silly_reactions")) {
					if (Tim.rand.nextInt(100) < aypwip_odds) {
						int i = Tim.rand.nextInt(Tim.amusement.aypwips.size());
						event.getChannel().send().message(String.format(Tim.amusement.aypwips.get(i), event.getUser().getNick()));
						aypwip_odds -= Tim.rand.nextInt(5);
					}
				} else if (Pattern.matches("(?i).*what.*is.*the.*answer.*", message) && cdata.chatter_enabled.get("silly_reactions")) {
					if (Tim.rand.nextInt(100) < answer_odds) {
						event.respond("The answer is 42. Everyone knows that.");
						answer_odds -= Tim.rand.nextInt(5);
					}
				} else if (Pattern.matches("(?i)" + Tim.bot.getNick() + ".*[?]", message) && cdata.chatter_enabled.get("silly_reactions")) {
					if (Tim.rand.nextInt(100) < eightball_odds) {
						Tim.amusement.eightball(event.getChannel(), event.getUser(), false);
						eightball_odds -= Tim.rand.nextInt(5);
					}
				} else {
					this.interact(event.getUser(), event.getChannel(), message, "say");
					Tim.markov.process_markov(message, "say", event.getUser().getNick());
				}
			}
		}
	}

	@Override
	public void onAction(ActionEvent event) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		PircBotX bot = event.getBot();
		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());

		if (event.getUser().getLogin().toLowerCase().equals("utoxin")) {
			if (message.equalsIgnoreCase("punches " + Tim.bot.getNick() + " in the face!")) {
				event.getChannel().send().action("falls over and dies.  x.x");
				Tim.shutdown();
			}
		}

		if (cdata.muzzled) {
			return;
		}

		if (Tim.rand.nextInt(500) == 0) {
			if (lights_odds < max_lights_odds) {
				lights_odds++;
			}
			if (fox_odds < max_fox_odds) {
				fox_odds += Tim.rand.nextInt(5);
			}
			if (cheeseburger_odds < max_cheeseburger_odds) {
				cheeseburger_odds += Tim.rand.nextInt(5);
			}
			if (test_odds < max_test_odds) {
				test_odds += Tim.rand.nextInt(5);
			}
			if (hug_odds < max_hug_odds) {
				hug_odds += Tim.rand.nextInt(5);
			}
			if (tissue_odds < max_tissue_odds) {
				tissue_odds += Tim.rand.nextInt(5);
			}
			if (aypwip_odds < max_aypwip_odds) {
				aypwip_odds += Tim.rand.nextInt(5);
			}
			if (answer_odds < max_answer_odds) {
				answer_odds += Tim.rand.nextInt(5);
			}
			if (eightball_odds < max_eightball_odds) {
				eightball_odds += Tim.rand.nextInt(5);
			}
		}

		if (!Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase()) &&
			!Tim.db.soft_ignore_list.contains(event.getUser().getNick().toLowerCase())
		) {
			if (message.toLowerCase().contains("how many lights") && cdata.chatter_enabled.get("silly_reactions")) {
				if (Tim.rand.nextInt(100) < lights_odds) {
					event.getChannel().send().message("There are FOUR LIGHTS!");
					lights_odds -= Tim.rand.nextInt(5);
				}
			} else if (message.toLowerCase().contains("what does the fox say") && cdata.chatter_enabled.get("silly_reactions")) {
				if (Tim.rand.nextInt(100) < fox_odds) {
					event.respond("mutters under his breath. \"Foxes don't talk. Sheesh.\"");
					fox_odds -= Tim.rand.nextInt(5);
				}
			} else if (message.toLowerCase().contains("cheeseburger") && cdata.chatter_enabled.get("silly_reactions")) {
				if (Tim.rand.nextInt(100) < cheeseburger_odds) {
					event.respond("sniffs the air, and peers around. \"Can has cheezburger?\"");
					cheeseburger_odds -= Tim.rand.nextInt(5);
				}
			} else if ((message.contains(":(") || message.contains("):")) && cdata.chatter_enabled.get("silly_reactions")) {
				if (Tim.rand.nextInt(100) < hug_odds) {
					event.getChannel().send().action("gives " + event.getUser().getNick() + " a hug.");
					hug_odds -= Tim.rand.nextInt(5);
				}
			} else if (message.toLowerCase().startsWith("tests") && cdata.chatter_enabled.get("silly_reactions")) {
				if (Tim.rand.nextInt(100) < test_odds) {
					event.respond("considers, and gives " + event.getUser().getNick() + " a grade: " + pickGrade());
					test_odds -= Tim.rand.nextInt(5);
				}
			} else if (message.contains(":'(") && cdata.chatter_enabled.get("silly_reactions")) {
				if (Tim.rand.nextInt(100) < tissue_odds) {
					event.getChannel().send().action("passes " + event.getUser().getNick() + " a tissue.");
					tissue_odds -= Tim.rand.nextInt(5);
				}
			} else if (Pattern.matches("(?i).*how do (i|you) (change|set) ?(my|your)? (nick|name).*", message) && cdata.chatter_enabled.get("helpful_reactions")) {
				event.respond("To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere");
			} else if (Pattern.matches("(?i).*are you (thinking|pondering) what i.*m (thinking|pondering).*", message) && cdata.chatter_enabled.get("silly_reactions")) {
				if (Tim.rand.nextInt(100) < aypwip_odds) {
					int i = Tim.rand.nextInt(Tim.amusement.aypwips.size());
					event.getChannel().send().message(String.format(Tim.amusement.aypwips.get(i), event.getUser().getNick()));
					aypwip_odds -= Tim.rand.nextInt(5);
				}
			} else if (Pattern.matches("(?i).*what.*is.*the.*answer.*", message) && cdata.chatter_enabled.get("silly_reactions")) {
				if (Tim.rand.nextInt(100) < answer_odds) {
					event.respond("sighs at the question. \"The answer is 42. I thought you knew that...\"");
					answer_odds -= Tim.rand.nextInt(5);
				}
			} else if (Pattern.matches("(?i)" + Tim.bot.getNick() + ".*[?]", message) && cdata.chatter_enabled.get("silly_reactions")) {
				if (Tim.rand.nextInt(100) < eightball_odds) {
					Tim.amusement.eightball(event.getChannel(), event.getUser(), false);
					eightball_odds -= Tim.rand.nextInt(5);
				}
			} else {
				this.interact(event.getUser(), event.getChannel(), message, "emote");
				Tim.markov.process_markov(message, "emote", event.getUser().getNick());
			}
		}
	}

	private void interact(User sender, Channel channel, String message, String type) {
		ChannelInfo cdata = Tim.db.channel_data.get(channel.getName().toLowerCase());

		if (cdata.randomChatterLevel <= 0) {
			return;
		}

		long odds = cdata.reactiveChatterLevel;

		if (message.toLowerCase().contains(Tim.bot.getNick().toLowerCase())) {
			odds = odds * cdata.chatterNameMultiplier;
		}

		if (Tim.rand.nextInt(100) < odds) {
			ArrayList<String> enabled_actions = new ArrayList<>(16);

			if (cdata.chatter_enabled.get("markov")) {
				enabled_actions.add("markov");
				enabled_actions.add("markov");
				enabled_actions.add("markov");
			}
			if (cdata.amusement_chatter_available()) {
				enabled_actions.add("amusement");
				enabled_actions.add("amusement");
				enabled_actions.add("amusement");
			}
			if (cdata.chatter_enabled.get("challenge")) {
				enabled_actions.add("challenge");
			}

			if (enabled_actions.isEmpty()) {
				return;
			}

			String action = enabled_actions.toArray(new String[enabled_actions.size()])[Tim.rand.nextInt(enabled_actions.size())];

			switch (action) {
				case "markov":
					if ("say".equals(type) && Tim.rand.nextBoolean()) {
						type = "mutter";
					} else if ("emote".equals(type) && Tim.rand.nextBoolean()) {
						type = "mutter";
					}
					Tim.markov.randomAction(channel.getName().toLowerCase(), type);
					break;
				case "challenge":
					Tim.challenge.randomAction(sender, cdata.channel);
					break;
				case "amusement":
					Tim.amusement.randomAction(sender, cdata.channel);
					break;
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
