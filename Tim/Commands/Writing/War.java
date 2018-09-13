package Tim.Commands.Writing;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Tim.Commands.ICommandHandler;
import Tim.Data.CommandData;
import Tim.Tim;
import Tim.WarTicker;
import Tim.WordWar;
import org.apache.commons.lang3.StringUtils;

public class War implements ICommandHandler {
	@Override
	public boolean handleCommand(CommandData commandData) {
		String[] args = commandData.args;
		HashSet<String> subcommands = new HashSet<>();
		subcommands.add("start");
		subcommands.add("cancel");
		subcommands.add("join");
		subcommands.add("report");
		subcommands.add("leave");
		subcommands.add("list");

		if (commandData.args.length < 1 || !subcommands.contains(args[0])) {
			commandData.event.respond("Valid subcommands: " + StringUtils.join(subcommands, ", "));
		} else {
			String subcommand = args[0];

			switch (subcommand) {
				case "start":
					if (commandData.args.length >= 2) {
						long time;
						long to_start = 60;
						int total_chains = 1;
						int delay;
						boolean do_randomness = false;

						StringBuilder warName = new StringBuilder();

						try {
							time = (long) (Double.parseDouble(commandData.args[1]) * 60);
						} catch (NumberFormatException e) {
							commandData.event.respond("I could not understand the duration. Was it numeric?");
							return true;
						}

						if (time < 60) {
							commandData.event.respond("Duration must be at least 1 minute.");
							return true;
						}

						delay = (int) time / 2;

						// Options for all wars
						Pattern startDelayPattern = Pattern.compile("^start:([0-9.]+)$");

						// Options for chain wars
						Pattern chainPattern      = Pattern.compile("^chain:([0-9.]+)$");
						Pattern breakPattern      = Pattern.compile("^break:([0-9.]+)$");
						Pattern randomnessPattern = Pattern.compile("^random:([01])$");

						Matcher m;

						if (commandData.args.length >= 3) {
							for (int i = 2; i < commandData.args.length; i++) {
								m = breakPattern.matcher(commandData.args[i]);
								if (m.find()) {
									try {
										delay = (int) (Double.parseDouble(m.group(1)) * 60);
									} catch (NumberFormatException ex) {
										Tim.printStackTrace(ex);
										Tim.logErrorString(String.format("Input: ''%s'' Found String: ''%s''",
																		 commandData.args[1], m.group(1)));
									}
									continue;
								}

								m = startDelayPattern.matcher(commandData.args[i]);
								if (m.find()) {
									try {
										to_start = (int) (Double.parseDouble(m.group(1)) * 60);
									} catch (NumberFormatException ex) {
										Tim.printStackTrace(ex);
										Tim.logErrorString(String.format("Input: ''%s'' Found String: ''%s''",
																		 commandData.args[1], m.group(1)));
									}
									continue;
								}

								m = randomnessPattern.matcher(commandData.args[i]);
								if (m.find()) {
									try {
										if (Integer.parseInt(m.group(1)) == 1) {
											do_randomness = true;
										}
									} catch (NumberFormatException ex) {
										Tim.printStackTrace(ex);
										Tim.logErrorString(String.format("Input: ''%s'' Found String: ''%s''",
																		 commandData.args[1], m.group(1)));
									}
									continue;
								}

								if (warName.toString().equals("")) {
									warName = new StringBuilder(commandData.args[i]);
								} else {
									warName.append(" ").append(commandData.args[i]);
								}
							}
						}

						if (warName.toString().equals("")) {
							warName = new StringBuilder(commandData.issuer + "'s War");
						}

						if (!this.wars.containsKey(warName.toString().toLowerCase())) {
							WordWar war = new WordWar(time, to_start, total_chains, 1, delay, do_randomness, warName.toString(), commandData.getUserEvent().getUser(), commandData.event.getChannel().getName());
							this.wars.put(war.getInternalName(), war);
							war.addMember(commandData.issuer, 0);

							if (to_start > 0) {
								commandData.event.respond(String.format("Your word war, %s, will start in " + to_start / 60.0 + " minutes. The ID is: %d.", war.getSimpleName(), war.db_id));
							} else {
								this.beginWar(war);
							}
						} else {
							commandData.event.respond("There is already a war with the name '" + warName + "'");
						}
					} else {
						commandData.event.respond("Usage: !war start <duration in min> [<options>] [<name>]");
						commandData.event.respond("Options, all wars: start:<minutes>");
						commandData.event.respond("Options, chain wars: chains:<chain count>, random:1, break:<minutes>");
					}
					return true;
					break;

				case "cancel":
					break;

				case "join":
					break;

				case "leave":
					break;

				case "report":
					break;

				case "list":
					break;

				default:
			}
		}

		switch (commandData.command) {
			case "startwar":
				if (args != null && args.length >= 1) {
					startWar(commandData);
				} else {
					commandData.event.respond("Usage: !startwar <duration in min> [<time to start in min> [<name>]]");
				}
				return true;

			case "chainwar":
				if (args != null && args.length > 1) {
					startChainWar(commandData);
				} else {
					commandData.event.respond("Usage: !chainwar <duration in min> <war count> [<name>]");
				}
				return true;

			case "starwar":
				commandData.event.respond("A long time ago, in a galaxy far, far away...");
				return true;

			case "endwar":
				endWar(commandData);
				return true;

			case "listwars":
				listWars(commandData, false);
				return true;

			case "listall":
				listAllWars(commandData);
				return true;

			case "joinwar":
				joinWar(commandData);
				return true;

			case "leavewar":
				leaveWar(commandData);
				return true;

			default:
				return false;
	}
}
