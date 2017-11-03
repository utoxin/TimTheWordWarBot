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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.pircbotx.Colors;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author mwalker
 */
class UserCommandListener extends ListenerAdapter {

	@Override
	public void onMessage(MessageEvent event) {
		if (event.getUser() == null) {
			return;
		}

		String message = Colors.removeFormattingAndColors(event.getMessage());

		if (!Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())) {
			if (message.charAt(0) == '!') {
				if (message.startsWith("!skynet")) {
					return;
				}

				String command;
				String[] args = null;

				int space = message.indexOf(" ");
				if (space > 0) {
					command = message.substring(1, space).toLowerCase();
					args = message.substring(space + 1).split(" ", 0);
				} else {
					command = message.substring(1).toLowerCase();
				}

				if (command.equals("startwar")) {
					if (args != null && args.length >= 1) {
						Tim.warticker.startWar(event, args);
					} else {
						event.respond("Usage: !startwar <duration in min> [<time to start in min> [<name>]]");
					}
				} else if (command.equals("chainwar")) {
					if (args != null && args.length > 1) {
						Tim.warticker.startChainWar(event, args);
					} else {
						event.respond("Usage: !chainwar <duration in min> <war count> [<name>]");
					}
				} else if (command.equals("starwar")) {
					event.respond("A long time ago, in a galaxy far, far away...");
				} else if (command.equals("endwar")) {
					Tim.warticker.endWar(event, args);
				} else if (command.equals("listwars")) {
					Tim.warticker.listWars(event, false);
				} else if (command.equals("listall")) {
					Tim.warticker.listAllWars(event);
				} else if (command.equals("boxodoom")) {
					Tim.amusement.boxodoom(event, args);
				} else if (command.equals("pickone")) {
					Tim.amusement.pickone(event, args);
				} else if (command.equals("eggtimer")) {
					double time = 15;
					if (args != null) {
						try {
							time = Double.parseDouble(args[0]);
						} catch (NumberFormatException e) {
							event.respond("Could not understand first parameter. Was it numeric?");
							return;
						}
					}

					event.respond("Your timer has been set.");
					try {
						Thread.sleep((long) (time * 60 * 1000));
					} catch (InterruptedException ex) {
						Logger.getLogger(UserCommandListener.class.getName()).log(Level.SEVERE, null, ex);
					}
					event.respond("Your timer has expired!");
				} else if (command.equals("ignore")) {
					if (args != null && (args[0].equals("soft") || args[0].equals("hard"))) {
						if (args[0].equals("hard")) {
							Tim.db.ignore_list.add(event.getUser().getNick().toLowerCase());
							event.respond("Fine. I didn't like you either. See if I talk to you ever again...");
						} else {
							Tim.db.soft_ignore_list.add(event.getUser().getNick().toLowerCase());
							event.respond("Okay, I'll stop bothering you. Sorry!");
						}
						Tim.db.saveIgnore(event.getUser().getNick().toLowerCase(), args[0]);
					} else {
						event.respond("Usage: !ignore <soft/hard>");
						event.respond("Warning: Hard ignores can only be cleared by admins.");
					}
				} else if (command.equals("unignore")) {
					if (Tim.db.soft_ignore_list.remove(event.getUser().getNick().toLowerCase())) {
						event.respond("Okay! Thanks! I'll try not to be /TOO/ annoying...");
						Tim.db.deleteIgnore(event.getUser().getNick().toLowerCase());
					} else {
						event.respond("Okay... I wasn't ignoring you anyway. :)");
					}
				} else if (command.equals("help")) {
					this.printCommandList(event);
				} else if (command.equals("credits")) {
					event.respond(
						"I was created by MysteriousAges in 2008 using PHP, and ported to the Java PircBot library in 2009. "
						+ "Utoxin started helping during NaNoWriMo 2010. Sourcecode is available here: "
						+ "https://github.com/utoxin/TimTheWordWarBot, and my NaNoWriMo profile page is here: "
						+ "http://nanowrimo.org/en/participants/timmybot");
				} else if (!Tim.story.parseUserCommand(event) && !Tim.challenge.parseCommand(command, args, event) && !Tim.amusement.parseUserCommand(event)) {
					event.respond("!" + command + " was not part of my training.");
				}
			}
		}
	}

	private void printCommandList(MessageEvent event) {
		if (event.getUser() == null) {
			return;
		}

		event.getChannel().send().action("whispers something to " + event.getUser().getNick() + ". (Check for a new window or tab with the help text.)");

		String[] strs = {"I am a robot trained by the WordWar Monks of Honolulu. You have "
			+ "never heard of them. It is because they are awesome.",
						 "Core Commands:",
						 "    !startwar <duration> <time to start> <an optional name> - Starts a word war",
						 "    !chainwar <base duration> <war count> - Starts a series of wars, with some randomness",
						 "    !listwars - I will tell you about the wars currently in progress.",
						 "    !boxodoom <difficulty> <duration> - Difficulty is extraeasy/easy/average/hard/extreme/insane/impossible/tadiera, duration in minutes.",
						 "    !eggtimer <time> - I will send you a message after <time> minutes.",
						 "    !ignore <hard/soft> - Make Timmy ignore you. Soft lets you keep using commands. Hard is EVERYTHING.",
						 "    !unignore - Turn off soft ignores. Hard ignores have to be removed by an admin.",
						 "    !credits - Details of my creators, and where to find my source code.",};
		for (String str : strs) {
			event.getUser().send().message(str);
		}

		Tim.story.helpSection(event);
		Tim.challenge.helpSection(event);
		Tim.amusement.helpSection(event);

		String[] post = {"I... I think there might be other tricks I know... You'll have to find them!",
						 "I will also respond to the /invite command if you would like to see me in another channel. "
		};
		for (String aPost : post) {
			event.getUser().send().message(aPost);
		}
	}
}
