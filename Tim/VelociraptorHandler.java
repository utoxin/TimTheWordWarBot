package Tim;

import Tim.Data.ChannelInfo;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.pircbotx.Channel;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class VelociraptorHandler {
	void sighting(Event event) {
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

		if (Tim.rand.nextInt(100) < cdata.velociraptor_odds) {
			cdata.recordVelociraptorSighting();

			if (action) {
				event.respond(actionResponses[Tim.rand.nextInt(actionResponses.length)]);
			} else {
				event.respond(messageResponses[Tim.rand.nextInt(messageResponses.length)]);
			}

			cdata.velociraptor_odds--;

			if (Tim.rand.nextInt(100) < 2) {
				swarm(cdata.channel);
			}
		}
	}

	private int oddsIncreasing(int input) {
		return (int) Math.floor(0.001 * input * input);
	}

	private int oddsDecreasing(int input) {
		return (int) Math.floor(Math.log(input) * 8);
	}

	void swarm(String channel) {
		ChannelInfo cdata = Tim.db.channel_data.get(channel.toLowerCase());

		Collection<ChannelInfo> attackCandidates = Tim.db.channel_data.entrySet()
																	  .stream()
																	  .filter(map -> map.getValue().chatter_enabled.get(
																		  "velociraptor") &&
																					 map.getValue().activeVelociraptors
																					 >= cdata.activeVelociraptors * 0.9)
																	  .collect(Collectors.toMap(Map.Entry::getKey,
																								Map.Entry::getValue))
																	  .values();

		Collection<ChannelInfo> migrateCandidates = Tim.db.channel_data.entrySet()
																	   .stream()
																	   .filter(map ->
																				   map.getValue().chatter_enabled.get(
																					   "velociraptor")
																				   && map.getValue().activeVelociraptors
																					  <= cdata.activeVelociraptors
																						 * 0.25)
																	   .collect(Collectors.toMap(Map.Entry::getKey,
																								 Map.Entry::getValue))
																	   .values();

		if (cdata.activeVelociraptors > 10 && Tim.rand.nextInt(100) < oddsDecreasing(cdata.activeVelociraptors)) {
			int attackCount = Tim.rand.nextInt(cdata.activeVelociraptors / 2);

			if (migrateCandidates.size() > 0 && Tim.rand.nextInt(100) < oddsIncreasing(cdata.activeVelociraptors)) {
				ChannelInfo victimCdata = (ChannelInfo) migrateCandidates.toArray()[Tim.rand.nextInt(
					migrateCandidates.size())];

				cdata.recordSwarmKills(attackCount, 0);
				victimCdata.recordSwarmDeaths(-attackCount);

				Tim.bot.sendIRC()
					   .message(channel, String.format(
						   "Apparently feeling crowded, %d of the velociraptors head off in search of new territory. "
						   + "After a searching, they settle in %s.", attackCount, victimCdata.channel));

				if (victimCdata.chatter_enabled.get("velociraptor") && !victimCdata.isMuzzled()) {
					Tim.bot.sendIRC()
						   .message(victimCdata.channel, String.format(
							   "A swarm of %d velociraptors appears from the direction of %s. "
							   + "The local raptors are nervous, but the strangers simply want to join the colony.",
							   attackCount, cdata.channel));
				}
			} else if (attackCandidates.size() > 0) {
				ChannelInfo victimCdata = (ChannelInfo) attackCandidates.toArray()[Tim.rand.nextInt(
					attackCandidates.size())];

				int returnCount    = Math.max(0, Tim.rand.nextInt(attackCount / 2) - Tim.rand.nextInt(
					(int) Math.floor(Math.sqrt(cdata.activeVelociraptors))));
				int defendingCount = victimCdata.activeVelociraptors;

				double killPercent = (((double) attackCount / (double) defendingCount) * 25.0) + (
					Math.log(Tim.rand.nextInt(attackCount)) * 5);

				if (killPercent > 100) {
					killPercent = 100;
				}

				int killCount = (int) (defendingCount * (killPercent / 100.0));
				if (killCount < 0) {
					return;
				}

				cdata.recordSwarmKills(attackCount - returnCount, killCount);
				victimCdata.recordSwarmDeaths(killCount);

				Tim.bot.sendIRC()
					   .message(channel, AttackMessage(victimCdata.channel, attackCount, killCount, returnCount));

				if (victimCdata.chatter_enabled.get("velociraptor") && !victimCdata.isMuzzled()) {
					Tim.bot.sendIRC()
						   .message(victimCdata.channel, DefenseMessage(cdata.channel, attackCount, killCount));
				}
			}
		} else {
			if (cdata.activeVelociraptors > 1) {
				int newCount = Tim.rand.nextInt(cdata.activeVelociraptors / 2);

				if (cdata.activeVelociraptors >= 4) {
					newCount -= Tim.rand.nextInt(cdata.activeVelociraptors / 4);
				}

				if (newCount < 1) {
					return;
				}

				cdata.recordVelociraptorSighting(newCount);

				Tim.bot.sendIRC()
					   .message(channel, HatchingRaptors(newCount));
			} else if (cdata.activeVelociraptors < 0) {
				cdata.activeVelociraptors = 0;
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
			"In a completely unjustified attack from %(channel), %(attackCount) raptors charge in, savage %(deathCount) of the local raptors, and then run off into the sunset."
		};

		DecimalFormat formatter = new DecimalFormat("#,###");

		Map<String, String> values = new HashMap<>();
		values.put("channel", channel);
		values.put("attackCount", formatter.format(attackCount));
		values.put("deathCount", formatter.format(deathCount));

		StrSubstitutor sub = new StrSubstitutor(values, "%(", ")");
		return sub.replace(defenseMessages[Tim.rand.nextInt(defenseMessages.length)]);
	}
}
