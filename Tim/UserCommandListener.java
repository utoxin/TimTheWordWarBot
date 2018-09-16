package Tim;

import Tim.Commands.Amusement.*;
import Tim.Commands.ICommandHandler;
import Tim.Commands.Utility.InteractionControls;
import Tim.Commands.Writing.War;
import Tim.Data.CommandData;
import Tim.Utility.CommandParser;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

/**
 * @author mwalker
 */
class UserCommandListener extends ListenerAdapter {
	private ICommandHandler[] commandHandlers = {
		new InteractionControls(),
		Tim.story,
		Tim.challenge,
		Tim.amusement,
		new Summon(),
		new Defenestrate(),
		new Fridge(),
		new Raptor(),
		new War()
	};

	@Override
	public void onMessage(MessageEvent event) {
		if (event.getUser() != null && !Tim.db.ignore_list.contains(event.getUser().getNick().toLowerCase())) {
			CommandData commandData = CommandParser.parseCommand(event);

			if (commandData.type == CommandData.CommandType.TIMMY_USER) {
				switch (commandData.command) {
					case "eggtimer":
						double time = 15;
						if (commandData.args.length > 0) {
							try {
								time = Double.parseDouble(commandData.args[0]);
							} catch (NumberFormatException e) {
								event.respond("Could not understand first parameter. Was it numeric?");
								return;
							}
						}

						event.respond("Your timer has been set.");
						try {
							Thread.sleep((long) (time * 60 * 1000));
						} catch (InterruptedException ex) {
							Tim.printStackTrace(ex);
						}
						event.respond("Your timer has expired!");
						break;

					case "ignore":
						if (commandData.args.length > 0 && (commandData.args[0].equals("soft") || commandData.args[0].equals("hard"))) {
							if (commandData.args[0].equals("hard")) {
								Tim.db.ignore_list.add(event.getUser().getNick().toLowerCase());
								event.respond("Fine. I didn't like you either. See if I talk to you ever again...");
							} else {
								Tim.db.soft_ignore_list.add(event.getUser().getNick().toLowerCase());
								event.respond("Okay, I'll stop bothering you. Sorry!");
							}
							Tim.db.saveIgnore(event.getUser().getNick().toLowerCase(), commandData.args[0]);
						} else {
							event.respond("Usage: !ignore <soft/hard>");
							event.respond("Warning: Hard ignores can only be cleared by admins.");
						}
						break;

					case "unignore":
						if (Tim.db.soft_ignore_list.remove(event.getUser().getNick().toLowerCase())) {
							event.respond("Okay! Thanks! I'll try not to be /TOO/ annoying...");
							Tim.db.deleteIgnore(event.getUser().getNick().toLowerCase());
						} else {
							event.respond("Okay... I wasn't ignoring you anyway. :)");
						}
						break;

					case "help":
						this.printCommandList(event);
						break;

					case "credits":
						event.respond(
							"I was created by MysteriousAges in 2008 using PHP, and ported to the Java PircBot library in 2009. "
								+ "Utoxin started helping during NaNoWriMo 2010. Sourcecode is available here: "
								+ "https://github.com/utoxin/TimTheWordWarBot, and my NaNoWriMo profile page is here: "
								+ "http://nanowrimo.org/en/participants/timmybot");
						break;

					default:
						boolean commandHandled = false;

						for (ICommandHandler handler : this.commandHandlers) {
							if (handler != null) {
								if (handler.handleCommand(commandData)) {
									commandHandled = true;
									break;
								}
							}
						}

						if (!commandHandled) {
							event.respond("!" + commandData.command + " was not part of my training.");
						}

						break;
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
			"General Commands:",
			"    !boxodoom <difficulty> <duration> - Difficulty is extraeasy/easy/average/hard/extreme/insane/impossible/tadiera, duration in minutes.",
			"    !eggtimer <time> - I will send you a message after <time> minutes.",
			"    !ignore <hard/soft> - Make Timmy ignore you. Soft lets you keep using commands. Hard is EVERYTHING.",
			"    !unignore - Turn off soft ignores. Hard ignores have to be removed by an admin.",
			"    !credits - Details of my creators, and where to find my source code.",};
		for (String str : strs) {
			event.getUser().send().message(str);
		}

		War.helpSection(event);
		Tim.story.helpSection(event);
		Tim.challenge.helpSection(event);
		Tim.amusement.helpSection(event);
		InteractionControls.helpSection(event);
		Dice.helpSection(event);

		String[] post = {"I... I think there might be other tricks I know... You'll have to find them!",
			"I will also respond to the /invite command if you would like to see me in another channel. "
		};
		for (String aPost : post) {
			event.getUser().send().message(aPost);
		}
	}
}
