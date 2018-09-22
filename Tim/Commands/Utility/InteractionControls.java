package Tim.Commands.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import Tim.Commands.ICommandHandler;
import Tim.Data.ChannelInfo;
import Tim.Data.CommandData;
import Tim.Tim;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.hooks.types.GenericUserEvent;

public class InteractionControls implements ICommandHandler {
	private HashSet<String> interactions = new HashSet<>();

	public InteractionControls() {
		interactions.addAll(ChannelInfo.getChatterDefaults()
									   .keySet());
		interactions.addAll(ChannelInfo.getCommandDefaults()
									   .keySet());
		interactions.add("hugs");
		interactions.remove("bored");
		interactions.remove("chainstory");
		interactions.remove("commandment");
		interactions.remove("dance");
		interactions.remove("dice");
		interactions.remove("eightball");
		interactions.remove("expound");
		interactions.remove("markov");
		interactions.remove("ping");
		interactions.remove("sing");
		interactions.remove("woot");
	}

	public static void helpSection(GenericUserEvent event) {
		String[] strs = {
			"Interaction Control Command:",
			"    This command allows you to control the ways Timmy interacts with you, in case you don't enjoy certain of his behaviors.",
			"    !interactionflag - Command for viewing and controlling the interactions.",
			};

		for (String str : strs) {
			event.getUser()
				 .send()
				 .message(str);
		}
	}

	public static boolean interactWithUser(String username, String interaction) {
		if (!Tim.db.userInteractionSettings.containsKey(username.toLowerCase())) {
			return true;
		}

		HashMap<String, Boolean> userSettings = Tim.db.userInteractionSettings.get(username.toLowerCase());
		if (!userSettings.containsKey(interaction)) {
			return true;
		}

		return userSettings.get(interaction);
	}

	@Override
	public boolean handleCommand(CommandData commandData) {
		if (commandData.type != CommandData.CommandType.TIMMY_USER) {
			return false;
		}

		switch (commandData.command) {
			case "interactionflag":
				if (checkVerified(commandData.getMessageEvent())) {
					if (commandData.args.length == 1 && commandData.args[0].equalsIgnoreCase("list")) {
						String target = commandData.issuer;
						if (Tim.db.userInteractionSettings.containsKey(target.toLowerCase())) {
							HashMap<String, Boolean> data = Tim.db.userInteractionSettings.get(target.toLowerCase());

							commandData.event.respond("Sending status of interaction settings via private message.");

							data.keySet()
								.forEach((setting) -> commandData.getUserEvent()
																 .getUser()
																 .send()
																 .message(setting + ": " + data.get(setting)
																							   .toString()));
						} else {
							commandData.event.respond("No settings stored for that user.");
						}
					} else if (commandData.args.length == 3 && commandData.args[0].equalsIgnoreCase("set")) {
						String target = commandData.issuer;
						if (!Tim.db.userInteractionSettings.containsKey(target.toLowerCase())) {
							Tim.db.userInteractionSettings.put(target.toLowerCase(), new HashMap<>());
						}

						HashMap<String, Boolean> userData = Tim.db.userInteractionSettings.get(target.toLowerCase());

						boolean flag = false;
						if (!"0".equals(commandData.args[2])) {
							flag = true;
						}

						if (commandData.args[1].equalsIgnoreCase("all")) {
							for (String key : sortedInteractions()) {
								userData.put(key, flag);
							}

							Tim.db.saveInteractionSettings();

							commandData.event.respond("All interaction flags updated.");
						} else {
							if (interactions.contains(commandData.args[1])) {
								if (!flag) {
									userData.put(commandData.args[1], false);
								} else {
									userData.remove(commandData.args[1]);
								}

								Tim.db.saveInteractionSettings();

								commandData.event.respond("Interaction flag updated.");
							} else {
								commandData.event.respond("I'm sorry, but I don't have a setting for " + commandData.args[1]);
							}
						}
					} else {
						commandData.event.respond("Usage: !interactionflag list OR !interactionflag set <type> <0/1>");
						commandData.event.respond("Valid Interaction Types: all, " + String.join(", ", sortedInteractions()));
					}
				} else {
					commandData.event.respond("You must be logged in via NickServ to use these commands.");
				}

				return true;
		}

		return false;
	}

	private boolean checkVerified(GenericMessageEvent event) {
		return event.getUser() != null && event.getUser()
											   .isVerified();
	}

	private ArrayList<String> sortedInteractions() {
		ArrayList<String> sorted = new ArrayList<>(interactions);
		Collections.sort(sorted);
		return sorted;
	}
}
