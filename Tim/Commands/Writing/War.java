package Tim.Commands.Writing;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Tim.Commands.ICommandHandler;
import Tim.Data.CommandData;
import Tim.Data.WordWar;
import Tim.Tim;
import Tim.Data.ChannelInfo;
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
						int time;
						int to_start = 60;
						byte total_chains = 1;
						int delay;
						boolean do_randomness = false;

						StringBuilder warName = new StringBuilder();

						try {
							time = (int) (Double.parseDouble(commandData.args[1]) * 60);
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
								m = startDelayPattern.matcher(commandData.args[i]);
								if (m.find()) {
									try {
										to_start = (int) (Double.parseDouble(m.group(1)) * 60);
									} catch (NumberFormatException ex) {
										Tim.printStackTrace(ex);
										Tim.logErrorString(String.format("Start Delay Match -- Input: ''%s'' Found String: ''%s''",
											commandData.argString, m.group(1)));
									}
									continue;
								}

								m = chainPattern.matcher(commandData.args[i]);
								if (m.find()) {
									try {
										total_chains = (byte) Double.parseDouble(m.group(1));
									} catch (NumberFormatException ex) {
										Tim.printStackTrace(ex);
										Tim.logErrorString(String.format("Chain Match -- Input: ''%s'' Found String: ''%s''",
											commandData.argString, m.group(1)));
									}
									continue;
								}

								m = breakPattern.matcher(commandData.args[i]);
								if (m.find()) {
									try {
										delay = (int) (Double.parseDouble(m.group(1)) * 60);
									} catch (NumberFormatException ex) {
										Tim.printStackTrace(ex);
										Tim.logErrorString(String.format("Break Match -- Input: ''%s'' Found String: ''%s''",
											commandData.argString, m.group(1)));
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
										Tim.logErrorString(String.format("Randomness Match -- Input: ''%s'' Found String: ''%s''",
											commandData.argString, m.group(1)));
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

						if (!Tim.warticker.wars.containsKey(warName.toString().toLowerCase())) {
							long currentEpoch = System.currentTimeMillis() / 1000;
							ChannelInfo cdata = Tim.db.channel_data.get(commandData.event.getChannel().getName().toLowerCase());
							WordWar war;

							if (total_chains <= 1) {
								war = new WordWar(cdata, commandData.getMessageEvent().getUser(), warName.toString(), time, currentEpoch + to_start);
							} else {
								war = new WordWar(cdata, commandData.getMessageEvent().getUser(), warName.toString(), time, currentEpoch + to_start, total_chains, delay, do_randomness);
							}

							Tim.warticker.wars.put(war.getInternalName(), war);

							if (to_start > 0) {
								commandData.event.respond(String.format("Your word war, %s, will start in %f minutes. The ID is: %d-%d.", war.getSimpleName(), to_start / 60.0, war.year, war.warId));
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
