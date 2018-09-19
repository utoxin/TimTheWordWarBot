package Tim.Commands.Writing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Tim.Commands.ICommandHandler;
import Tim.Data.CommandData;
import Tim.Data.UserData;
import Tim.Data.WordWar;
import Tim.Tim;
import Tim.Data.ChannelInfo;
import Tim.Utility.Permissions;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.types.GenericUserEvent;

public class War implements ICommandHandler {
	private HashSet<String> handledCommands = new HashSet<>();

	public War() {
		handledCommands.add("war");
		handledCommands.add("listall");
		handledCommands.add("startwar");
		handledCommands.add("endwar");
		handledCommands.add("chainwar");
		handledCommands.add("listwars");
		handledCommands.add("joinwar");
		handledCommands.add("leavewar");
	}

	public static void helpSection(GenericUserEvent event) {
		String[] strs = {
			"Word War Commands:",
			"    These commands deal with the creation and interaction with word wars (aka sprints).",
			"    For more details on each command, use it without any args.", "    !war start - Creates a new war.",
			"    !war cancel - Cancels an existing war.", "    !war join - Signs up for notifications about a war.",
			"    !war leave - Cancels the notifications about a war.",
			"    !war report - Report your wordcount for a war.",
			"    !war list [all] - List wars in current channel or globally.",
			};

		for (String str : strs) {
			event.getUser()
				 .send()
				 .message(str);
		}
	}

	@Override
	public boolean handleCommand(CommandData commandData) {
		if (!handledCommands.contains(commandData.command)) {
			return false;
		}

		String[] args = commandData.args;

		switch (commandData.command) {
			case "endwar":
				commandData.event.respond("The syntax for war-related commands has changed. Try !war cancel.");
				return true;

			case "startwar":
			case "chainwar":
				commandData.event.respond("The syntax for war-related commands has changed. Try !war start.");
				return true;

			case "listall":
			case "listwars":
				commandData.event.respond("The syntax for war-related commands has changed. Try !war list.");
				return true;

			case "joinwar":
				commandData.event.respond("The syntax for war-related commands has changed. Try !war join.");
				return true;

			case "leavewar":
				commandData.event.respond("The syntax for war-related commands has changed. Try !war leave.");
				return true;

			case "war":
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
								int     time;
								int     to_start      = 60;
								byte    total_chains  = 1;
								int     delay;
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

								delay = time / 2;

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
												Tim.logErrorString(String.format(
													"Start Delay Match -- Input: ''%s'' Found String: ''%s''",
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
												Tim.logErrorString(
													String.format("Chain Match -- Input: ''%s'' Found String: ''%s''",
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
												Tim.logErrorString(
													String.format("Break Match -- Input: ''%s'' Found String: ''%s''",
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
												Tim.logErrorString(String.format(
													"Randomness Match -- Input: ''%s'' Found String: ''%s''",
													commandData.argString, m.group(1)));
											}
											continue;
										}

										if (warName.toString()
												   .equals("")) {
											warName = new StringBuilder(commandData.args[i]);
										} else {
											warName.append(" ")
												   .append(commandData.args[i]);
										}
									}
								}

								if (warName.toString()
										   .equals("")) {
									warName = new StringBuilder(commandData.issuer + "'s War");
								}

								if (warName.toString()
										   .matches("^\\d+$")) {
									commandData.event.respond(String.format(
										"War names must be more than a number. It's possible you meant to specify the "
										+ "start delay. Try: !war start %s start:%s",
										args[1], warName.toString()));
									return true;
								}

								long currentEpoch = System.currentTimeMillis() / 1000;
								ChannelInfo cdata = Tim.db.channel_data.get(commandData.event.getChannel()
																							 .getName()
																							 .toLowerCase());
								WordWar war;

								if (total_chains <= 1) {
									war = new WordWar(cdata, commandData.getMessageEvent()
																		.getUser(), warName.toString(), time,
													  currentEpoch + to_start);
								} else {
									war = new WordWar(cdata, commandData.getMessageEvent()
																		.getUser(), warName.toString(), time,
													  currentEpoch + to_start, total_chains, delay, do_randomness);
								}

								Tim.warticker.wars.add(war);

								if (to_start > 0) {
									commandData.event.respond(
										String.format("Your word war, %s, will start in %f minutes. The ID is: %d-%d.",
													  war.getSimpleName(), to_start / 60.0, war.year, war.warId));
								}
							} else {
								commandData.event.respond("Usage: !war start <duration in min> [<options>] [<name>]");
								commandData.event.respond("Options, all wars: start:<minutes>");
								commandData.event.respond(
									"Options, chain wars: chains:<chain count>, random:1, break:<minutes>");
							}
							break;

						case "cancel":
							if (commandData.args.length > 1) {
								String name = StringUtils.join(
									Arrays.copyOfRange(commandData.args, 1, commandData.args.length), " ");
								String channel = commandData.getChannelEvent()
															.getChannel()
															.getName();

								WordWar warByName = this.findWarByName(name, channel);
								WordWar warById   = this.findWarById(name);

								if (warByName != null) {
									this.removeWar(commandData, warByName);
								} else if (warById != null) {
									this.removeWar(commandData, warById);
								} else {
									commandData.event.respond("I don't know of a war with name or ID '" + name + "'.");
								}
							} else {
								commandData.event.respond("Syntax: !war cancel <name or war id>");
							}
							break;

						case "join":
							if (commandData.args.length == 1) {
								commandData.event.respond("Usage: !war join <war id>");
							} else {
								WordWar war = this.findWarById(args[1]);

								if (war == null) {
									commandData.event.respond("That war id was not found.");
								} else {
									war.addMember(commandData.issuer);
									commandData.event.respond("You have joined the war.");
								}
							}
							break;

						case "leave":
							if (commandData.args.length == 1) {
								commandData.event.respond("Usage: !war leave <war id>");
							} else {
								WordWar war = this.findWarById(args[1]);

								if (war == null) {
									commandData.event.respond("That war id was not found.");
								} else {
									war.removeMember(commandData.issuer);
									commandData.event.respond("You have left the war.");
								}
							}
							break;

						case "report":
							if (commandData.args.length < 3) {
								commandData.event.respond("Usage: !war report <war id> <wordcount>");
								commandData.event.respond(
									"Note: Wordcount should be words written during the war, not total count.");
							} else {
								WordWar  war      = Tim.db.loadWar(args[1]);
								UserData userData = Tim.userDirectory.findUserData(commandData.issuer);
								ChannelInfo cdata = Tim.db.channel_data.get(commandData.event.getChannel()
																							 .getName()
																							 .toLowerCase());

								int wordCount;

								if (userData == null || !userData.raptorAdopted) {
									commandData.event.respond(
										"I'm sorry, you must have adopted a raptor to record your stats... (!raptor "
										+ "command)");
								} else if (war == null) {
									commandData.event.respond("That war couldn't be found...");
								} else {
									try {
										wordCount = (int) Double.parseDouble(commandData.args[2]);
									} catch (NumberFormatException e) {
										commandData.event.respond(
											"I could not understand the wordcount. Was it numeric?");
										return true;
									}

									if (userData.recordedWars.containsKey(war.uuid)) {
										userData.totalSprintWordcount -= userData.recordedWars.get(war.uuid);
									} else {
										userData.totalSprints++;
										userData.totalSprintDuration += war.duration() / 60;
									}

									userData.totalSprintWordcount += wordCount;
									userData.recordedWars.put(war.uuid, wordCount);

									userData.save();

									cdata.raptorStrengthBoost += Math.min(10, war.baseDuration / 600);

									commandData.event.respond(String.format(
										"%s pulls out their %s notebook and makes a note of that wordcount.",
										userData.raptorName, userData.raptorFavoriteColor));

									if (!cdata.isMuzzled()) {
										Tim.raptors.warReport(userData, war, wordCount);
									}
								}
							}
							break;

						case "list":
							boolean all = false;
							boolean responded = false;

							if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
								all = true;
							}

							if (Tim.warticker.wars != null && Tim.warticker.wars.size() > 0) {
								int maxIdLength       = 1;
								int maxDurationLength = 1;

								for (WordWar war : Tim.warticker.wars) {
									String warId = String.format("%d-%d", war.year, war.warId);
									if (warId.length() > maxIdLength) {
										maxIdLength = warId.length();
									}

									if (war.getDurationText(war.duration())
										   .length() > maxDurationLength) {
										maxDurationLength = war.getDurationText(war.duration())
															   .length();
									}
								}

								for (WordWar war : Tim.warticker.wars) {
									if (all || war.getChannel()
												  .equalsIgnoreCase(commandData.event.getChannel()
																					 .getName())) {
										commandData.event.respond(
											all ? war.getDescriptionWithChannel(maxIdLength, maxDurationLength)
												: war.getDescription(maxIdLength, maxDurationLength));
										responded = true;
									}
								}
							}

							if (!responded) {
								commandData.event.respond("No wars are currently available.");
							}
							break;

						default:
							commandData.event.respond("Valid subcommands: " + StringUtils.join(subcommands, ", "));
					}
				}
				return true;

			default:
				return false;
		}
	}

	/**
	 * Looks for a war by name, in the specified channel. Non-unique matches result in a null.
	 *
	 * @param name    The name of the war to find
	 * @param channel Which channel the war must be in
	 *
	 * @return The identified war, or null if no unique result found
	 */
	private WordWar findWarByName(String name, String channel) {
		WordWar returnWar = null;
		for (WordWar war : Tim.warticker.wars) {
			if (war.getInternalName()
				   .equalsIgnoreCase(name) && war.channel.equalsIgnoreCase(channel)) {
				if (returnWar != null) {
					return null;
				} else {
					returnWar = war;
				}
			}
		}

		return returnWar;
	}

	/**
	 * @param id The id of the war to search for
	 *
	 * @return The war if found
	 */
	private WordWar findWarById(String id) {
		for (WordWar war : Tim.warticker.wars) {
			String warId = String.format("%d-%d", war.year, war.warId);
			if (warId.equals(id)) {
				return war;
			}
		}

		return null;
	}

	private void removeWar(CommandData commandData, WordWar war) {
		if (commandData.issuer.equalsIgnoreCase(war.getStarter()) || Permissions.isAdmin(commandData)) {
			Tim.warticker.wars.remove(war);

			war.cancelWar();
			commandData.event.respond(war.getSimpleName() + " has been ended.");
		} else {
			commandData.event.respond("Only the starter of a war can end it early.");
		}
	}
}
