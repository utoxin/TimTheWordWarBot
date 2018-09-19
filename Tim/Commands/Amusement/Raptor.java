package Tim.Commands.Amusement;

import Tim.Commands.ICommandHandler;
import Tim.Data.ChannelInfo;
import Tim.Data.CommandData;
import Tim.Data.UserData;
import Tim.Data.WordWar;
import Tim.Tim;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.pircbotx.Channel;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Raptor implements ICommandHandler {
	private HashSet<String> handledCommands = new HashSet<>();

	public Raptor() {
		handledCommands.add("raptor");
	}

	@Override
	public boolean handleCommand(CommandData commandData) {
		if (handledCommands.contains(commandData.command)) {
			UserData userData = Tim.userDirectory.findUserData(commandData.issuer);

			if (userData == null) {
				commandData.event.respond(
					"I'm sorry, you must be registered before you can work with raptors... (Please ensure you are "
					+ "registered and identified with NickServ.)");
			} else if (commandData.args.length < 1) {
				commandData.event.respond(
					"Raptors recently became fascinated by writing, so we created a raptor adoption program. They will"
					+ " monitor your word sprints, and share the details with you when asked. Don't worry, they "
					+ "probably won't eat your cat.");
				commandData.event.respond("Valid subcommands: adopt, release, details, rename");
			} else {
				String subcommand = commandData.args[0];
				switch (subcommand) {
					case "adopt":
						if (userData.raptorAdopted) {
							commandData.event.respond(
								"Due to safety regulations, you may only adopt a single raptor" + ".");
						} else if (commandData.args.length < 2) {
							commandData.event.respond("You need to provide a name to adopt your new raptor.");
						} else {
							userData.raptorAdopted = true;
							userData.raptorName = commandData.args[1];
							userData.raptorFavoriteColor = Tim.db.dynamic_lists.get("color")
																			   .get(Tim.rand.nextInt(
																				   Tim.db.dynamic_lists.get("color")
																									   .size()));
							userData.save();

							commandData.event.respond(
								"Excellent! I've noted your raptor's name. Just so you know, their favorite color is "
								+ userData.raptorFavoriteColor + ".");
						}
						break;

					case "release":
						if (!userData.raptorAdopted) {
							commandData.event.respond(
								"I don't have any record of you having a raptor. Are you sure that isn't your cat?");
						} else {
							commandData.event.respond("I'll release " + userData.raptorName
													  + " back into the wild. I'm sure they'll adjust well...");

							userData.raptorAdopted = false;
							userData.raptorName = null;
							userData.raptorFavoriteColor = null;
							userData.raptorBunniesStolen = 0;
							userData.lastBunnyRaid = null;
							userData.save();
						}
						break;

					case "rename":
						if (!userData.raptorAdopted) {
							commandData.event.respond(
								"I don't have any record of you having a raptor. I can't rename your children.");
						} else if (commandData.args.length < 2) {
							commandData.event.respond("You need to provide a new name.");
						} else {
							userData.raptorName = commandData.args[1];
							userData.save();

							commandData.event.respond(
								"Okay... your raptor should respond to the name " + commandData.args[1]
								+ " now. Don't do this too often. You'll confuse the poor thing.");
						}
						break;

					case "details":
						commandData.event.respond("TODO");
						break;

					default:
						commandData.event.respond("Valid subcommands: adopt, release, details");
				}
			}

			return true;
		}

		return false;
	}

	public void sighting(Event event) {
		String[] actionResponses = {
			"jots down a note in a red notebook labeled 'Velociraptor Sighting Log'.",
			};

		String[] messageResponses = {
			"Velociraptor sighted! Incident has been logged.",
			};

		Channel channel = null;
		boolean action  = false;

		if (event instanceof MessageEvent) {
			channel = ((MessageEvent) event).getChannel();
		} else if (event instanceof ActionEvent) {
			channel = ((ActionEvent) event).getChannel();
			action = true;
		}

		if (channel == null) {
			return;
		}

		ChannelInfo cdata = Tim.db.channel_data.get(channel.getName()
														   .toLowerCase());

		if (Tim.rand.nextInt(100) < Math.max(10, 100 - cdata.activeVelociraptors)) {
			cdata.recordSighting();

			if (action) {
				event.respond(actionResponses[Tim.rand.nextInt(actionResponses.length)]);
			} else {
				event.respond(messageResponses[Tim.rand.nextInt(messageResponses.length)]);
			}
		}
	}

	public void swarm(String channel) {
		ChannelInfo cdata = Tim.db.channel_data.get(channel.toLowerCase());

		if (Tim.rand.nextInt(100) <= 33) {
			int magicNumber = Tim.rand.nextInt(100);

			// 10-45% odds of attack or colonize, remainder odds to hatching
			int thresholdNumber = 10 + (int) (35.0 * (Math.max(0.0, 100.0 - cdata.activeVelociraptors) / 100.0));

			if (magicNumber < thresholdNumber) {
				this.attackChannel(cdata);
			} else if (magicNumber < (thresholdNumber * 2)) {
				this.colonizeChannel(cdata);
			} else {
				this.hatchRaptors(cdata);
			}
		}
	}

	private void hatchRaptors(ChannelInfo cdata) {
		if (cdata.activeVelociraptors > 1) {
			int newCount = Tim.rand.nextInt(cdata.activeVelociraptors / 2);

			if (cdata.activeVelociraptors >= 4) {
				newCount -= Tim.rand.nextInt(cdata.activeVelociraptors / 4);
			}

			if (newCount < 1) {
				return;
			}

			cdata.recordSighting(newCount);

			Tim.bot.sendIRC()
				   .message(cdata.channel, HatchingRaptors(newCount));
		}
	}

	private void colonizeChannel(ChannelInfo cdata) {
		int colonyCount = Tim.rand.nextInt(cdata.activeVelociraptors / 4);

		if (colonyCount > 0) {
			ChannelInfo colonizeChannel = this.selectLowPopulationRaptorChannel(cdata);

			if (colonizeChannel != null) {
				colonizeChannel.recordNewRaptors(colonyCount);
				cdata.recordLeavingRaptors(colonyCount);

				Tim.bot.sendIRC()
					   .message(cdata.channel, String.format(
						   "Apparently feeling crowded, %d of the velociraptors head off in search of new territory. "
						   + "After a searching, they settle in %s.", colonyCount, colonizeChannel.channel));

				Tim.bot.sendIRC()
					   .message(colonizeChannel.channel, String.format(
						   "A swarm of %d velociraptors appears from the direction of %s. The local raptors are nervous, but the strangers simply want to join the colony.",
						   colonyCount, cdata.channel));
			}
		}
	}

	private void attackChannel(ChannelInfo attackingChannel) {
		int attackCount = Tim.rand.nextInt(attackingChannel.activeVelociraptors / 4);

		if (attackCount > 0) {
			ChannelInfo defendingChannel = this.selectHighPopulationRaptorChannel(attackingChannel);

			if (defendingChannel != null) {
				int attackerDeaths = 0;
				int defenderDeaths = 0;

				int attackerBonus = attackingChannel.raptorStrengthBoost;
				int defenderBonus = defendingChannel.raptorStrengthBoost;

				int fightCount = Math.min(attackCount, defendingChannel.activeVelociraptors);

				for (int i = 0; i < fightCount; i++) {
					int attackRoll = (int) Math.round(Tim.rand.nextFloat() * 100 * ((1 + attackerBonus) / 100.0));
					int defenseRoll = (int) Math.round(Tim.rand.nextFloat() * 100 * ((1 + defenderBonus) / 100.0));

					if (attackRoll > defenseRoll) {
						defenderDeaths++;
					} else {
						attackerDeaths++;
					}
				}

				attackingChannel.recordKills(defenderDeaths);
				defendingChannel.recordDeaths(defenderDeaths);
				defendingChannel.recordKills(attackerDeaths);

				Tim.bot.sendIRC()
					   .message(attackingChannel.channel, AttackMessage(defendingChannel.channel, attackCount, defenderDeaths, attackCount - attackerDeaths));

				Tim.bot.sendIRC()
					   .message(defendingChannel.channel, DefenseMessage(attackingChannel.channel, attackCount, defenderDeaths));
			}
		}
	}

	private String HatchingRaptors(int quantity) {
		String[] singularHatchingStrings = {
			"Something is going on in the swarm... hey, where did that baby raptor come from?! Clever girls.",
			"You hear an odd chirping sound, and after looking around, discover a newly hatched raptor.",
			"Hold on... where did that baby raptor come from?",
			"You could have sworn that baby raptor wasn't there a few minutes ago.",
			"A female raptor whistles proudly, showing off her freshly hatched child."
		};

		String[] pluralHatchingSttrings = {
			"Something is going on in the swarm... hey, where did those %d baby raptors come from?! Clever girls.",
			"You hear a chorus of chirps, and it doesn't take long to discover a flock of %d baby raptors. Oh dear.",
			"Back away carefully... there's %d baby raptors right there in the corner.",
			"You could have sworn there weren't %d baby raptors here a few minutes ago...",
			"You are momentarily deafened by the proud new mothers, arguing over which of the %d baby raptors is "
			+ "cutest."
		};

		if (quantity == 1) {
			return singularHatchingStrings[Tim.rand.nextInt(singularHatchingStrings.length)];
		} else {
			return String.format(pluralHatchingSttrings[Tim.rand.nextInt(pluralHatchingSttrings.length)], quantity);
		}
	}

	private String AttackMessage(String channel, int attackCount, int killCount, int returnCount) {
		String[] attackMessages = {
			"Suddenly, %(attackCount) of the raptors go charging off to attack a group in %(channel)! After a horrific"
			+ " battle, they manage to kill %(killCount) of them, and %(returnCount) return home!",
			"A group of %(attackCount) local raptors are restless, and head off to %(channel), where they kill %"
			+ "(killCount) raptors. Eventually, %(returnCount) of them make their way back home.",
			"Outraged by previous attacks, %(attackCount) of the local raptors head off to %(channel), killing %"
			+ "(killCount) others to satisfy their thirst for revenge. Only %(returnCount) return home."
		};

		DecimalFormat formatter = new DecimalFormat("#,###");

		Map<String, String> values = new HashMap<>();
		values.put("channel", channel);
		values.put("attackCount", formatter.format(attackCount));
		values.put("killCount", formatter.format(killCount));
		values.put("returnCount", formatter.format(returnCount));

		StrSubstitutor sub = new StrSubstitutor(values, "%(", ")");
		return sub.replace(attackMessages[Tim.rand.nextInt(attackMessages.length)]);
	}

	private String DefenseMessage(String channel, int attackCount, int deathCount) {
		String[] defenseMessages = {
			"A swarm of %(attackCount) raptors suddenly appears from the direction of %(channel). The local raptors do"
			+ " their best to fight them off, and %(deathCount) of them die before the swarm disappears.",
			"A group of %(attackCount) restless raptors from %(channel) shows up without warning, and manage to kill %"
			+ "(deathCount) local raptors before they move on.",
			"In a completely unjustified attack from %(channel), %(attackCount) raptors charge in, savage %"
			+ "(deathCount) of the local raptors, and then run off into the sunset."
		};

		DecimalFormat formatter = new DecimalFormat("#,###");

		Map<String, String> values = new HashMap<>();
		values.put("channel", channel);
		values.put("attackCount", formatter.format(attackCount));
		values.put("deathCount", formatter.format(deathCount));

		StrSubstitutor sub = new StrSubstitutor(values, "%(", ")");
		return sub.replace(defenseMessages[Tim.rand.nextInt(defenseMessages.length)]);
	}

	public void warReport(UserData userData, WordWar war, int wordCount) {
		ArrayList<String> actions = new ArrayList<>();
		actions.add("brag");
		actions.add("bunnies");
		actions.add("recruit");

		int wpm = wordCount / (war.baseDuration / 60);

		Collections.shuffle(actions);

		for (String action : actions) {
			if (Tim.rand.nextInt(100) < (wpm / 10)) {
				switch (action) {
					case "brag":
						this.bragAboutWordcount(userData, war, wordCount);
						break;

					case "bunnies":
						this.stealPlotBunnies(userData, war);
						break;

					case "recruit":
						this.recruitRaptors(userData, war);
						break;
				}

				break;
			}
		}
	}

	private void recruitRaptors(UserData userData, WordWar war) {
		ChannelInfo selectedChannel = this.selectHighPopulationRaptorChannel(war.cdata);

		if (selectedChannel == null) {
			return;
		}

		double[] thresholds = {0, 1, 2};

		ArrayList<String[]> originStrings = new ArrayList<>();
		originStrings.add(new String[]{
			"%s hurries off to %s and after a short time away comes back, dejected, with the %d raptors they tempted to join them.",
			"%s hurries off to %s and after a short time away comes back, leading the %d raptor they tempted to join them.",
			"%s hurries off to %s and after a short time away comes back with %d raptors they tempted to join them."
		});

		ArrayList<String[]> destinationStrings = new ArrayList<>();
		destinationStrings.add(new String[]{
			"%s slips in from %s and makes encouraging noises at the local raptors, but they have no effect. They leave with %d raptors.",
			"%s sneaks in from %s and is approached by %d raptor, who they lead off with them after a few minutes of encouraging noises.",
			"%s hurries in from %s and after a short time, manages to lure %d raptors to follow them back home."
		});

		int recruitedRaptors = Tim.rand.nextInt(selectedChannel.activeVelociraptors / 10);
		Collections.shuffle(originStrings);
		Collections.shuffle(destinationStrings);

		ChoiceFormat originString = new ChoiceFormat(thresholds, originStrings.get(0));
		ChoiceFormat destinationString = new ChoiceFormat(thresholds, destinationStrings.get(0));

		Tim.bot.sendIRC().message(war.channel, String.format(originString.format(recruitedRaptors), userData.raptorName, selectedChannel.channel, recruitedRaptors));
		Tim.bot.sendIRC().message(selectedChannel.channel, String.format(destinationString.format(recruitedRaptors), userData.raptorName, war.channel, recruitedRaptors));

		war.cdata.recordSighting(recruitedRaptors);

		selectedChannel.activeVelociraptors -= recruitedRaptors;
		if (selectedChannel.activeVelociraptors < 0) {
			selectedChannel.activeVelociraptors = 0;
		}
		Tim.db.saveChannelSettings(selectedChannel);
	}

	private void stealPlotBunnies(UserData userData, WordWar war) {
		ChannelInfo selectedChannel = this.selectRandomRaptorChannel(war.cdata);

		if (selectedChannel == null) {
			return;
		}

		double[] thresholds = {0, 1, 2};

		ArrayList<String[]> originStrings = new ArrayList<>();
		originStrings.add(new String[]{
			"%s is running out of ideas, but after raiding %s they come back with %d plot bunnies. Sad day.",
			"%s needs some ideas, so they run off to %s and managed to find %d plot bunny to bring home.",
			"%s is desparate for ideas, so they charge off to %s and come home with %d plot bunnies to add to their collection."
		});

		ArrayList<String[]> destinationStrings = new ArrayList<>();
		destinationStrings.add(new String[]{
			"A raptor named %s charges in from %s, looking for something. But they miss seeing the plot bunnies, and leave with %d of them.",
			"The raptor %s runs in from %s and finds %d lonely plot bunny, which they throw in their pack, before heading back home.",
			"A raptor wearing a nametag that says %s heads in from the direction of %s, throws %d plot bunnies into their bag, and then runs off again."
		});

		int plotBunnies = Tim.rand.nextInt(10);
		Collections.shuffle(originStrings);
		Collections.shuffle(destinationStrings);

		ChoiceFormat originString = new ChoiceFormat(thresholds, originStrings.get(0));
		ChoiceFormat destinationString = new ChoiceFormat(thresholds, destinationStrings.get(0));

		Tim.bot.sendIRC().message(war.channel, String.format(originString.format(plotBunnies), userData.raptorName, selectedChannel.channel, plotBunnies));
		Tim.bot.sendIRC().message(selectedChannel.channel, String.format(destinationString.format(plotBunnies), userData.raptorName, war.channel, plotBunnies));

		userData.raptorBunniesStolen += plotBunnies;
		userData.lastBunnyRaid = new Date();
		userData.save();
	}

	private void bragAboutWordcount(UserData userData, WordWar war, int wordCount) {
		ChannelInfo selectedChannel = this.selectRandomRaptorChannel(war.cdata);
		// TODO Implement this
	}

	private ChannelInfo selectHighPopulationRaptorChannel(ChannelInfo firstChannel) {
		ArrayList<ChannelInfo> candidates;

		candidates = (ArrayList<ChannelInfo>) Tim.db.channel_data.values()
																 .stream()
																 .filter(o -> o.chatter_enabled.get("velociraptor")
																			  && o.activeVelociraptors > 0 && !o.isMuzzled())
																 .sorted(
																	 Comparator.comparing(o -> o.activeVelociraptors))
																 .collect(Collectors.toList());

		candidates.remove(firstChannel);
		Collections.reverse(candidates);

		int totalRaptors = candidates.stream()
									 .mapToInt(o -> o.activeVelociraptors)
									 .sum();

		if (totalRaptors == 0) {
			return null;
		} else {
			int magicNumber   = Tim.rand.nextInt(totalRaptors);
			int currentNumber = 0;

			for (ChannelInfo channel : candidates) {
				currentNumber += channel.activeVelociraptors;
				if (currentNumber <= magicNumber) {
					return channel;
				}
			}

			return candidates.get(0);
		}
	}

	private ChannelInfo selectLowPopulationRaptorChannel(ChannelInfo firstChannel) {
		ArrayList<ChannelInfo> candidates;

		candidates = (ArrayList<ChannelInfo>) Tim.db.channel_data.values()
																 .stream()
																 .filter(o -> o.chatter_enabled.get("velociraptor")
																			  && o.activeVelociraptors > 0 && !o.isMuzzled())
																 .sorted(
																	 Comparator.comparing(o -> o.activeVelociraptors))
																 .collect(Collectors.toList());

		candidates.remove(firstChannel);

		int maxRaptors = candidates.stream()
								   .mapToInt(o -> o.activeVelociraptors)
								   .max()
								   .orElse(0);

		int totalRaptors = candidates.stream()
									 .mapToInt(o -> o.activeVelociraptors)
									 .sum();

		if (totalRaptors == 0) {
			return null;
		} else {
			int magicNumber   = Tim.rand.nextInt(totalRaptors);
			int currentNumber = 0;

			for (ChannelInfo channel : candidates) {
				currentNumber += (maxRaptors - channel.activeVelociraptors);
				if (currentNumber <= magicNumber) {
					return channel;
				}
			}

			return candidates.get(0);
		}
	}

	private ChannelInfo selectRandomRaptorChannel(ChannelInfo firstChannel) {
		ArrayList<ChannelInfo> candidates;

		candidates = (ArrayList<ChannelInfo>) Tim.db.channel_data.values()
																 .stream()
																 .filter(o -> o.chatter_enabled.get("velociraptor")
																			  && o.activeVelociraptors > 0 && !o.isMuzzled())
																 .collect(Collectors.toList());

		candidates.remove(firstChannel);

		if (candidates.size() == 0) {
			return null;
		} else {
			Collections.shuffle(candidates);
			return candidates.get(0);
		}
	}
}
