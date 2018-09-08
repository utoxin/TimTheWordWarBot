package Tim;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Tim.Commands.ICommandHandler;
import Tim.Data.CommandData;
import Tim.Utility.Permissions;
import org.apache.commons.lang3.StringUtils;

class WarTicker implements ICommandHandler {
	WarClockThread warTicker;
	ConcurrentHashMap<String, WordWar> wars;
	ConcurrentHashMap<Integer, WordWar> wars_by_id = new ConcurrentHashMap<>();

	private WarTicker() {
		Timer ticker;

		this.wars = Tim.db.loadWars();
		for (WordWar war : this.wars.values()) {
			this.wars_by_id.put(war.db_id, war);
		}

		this.warTicker = new WarClockThread(this);
		ticker = new Timer(true);
		ticker.scheduleAtFixedRate(this.warTicker, 0, 1000);
	}

	@Override
	public boolean handleCommand(CommandData commandData) {
		String[] args = commandData.args;

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

	private static class SingletonHelper {
		private static final WarTicker instance = new WarTicker();
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static WarTicker getInstance() {
		return SingletonHelper.instance;
	}

	@SuppressWarnings("WeakerAccess")
	class WarClockThread extends TimerTask {
		private final WarTicker parent;

		WarClockThread(WarTicker parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			try {
				this.parent._tick();
			} catch (Throwable t) {
				System.out.println("&&& THROWABLE CAUGHT in DelayCommand.run:");
				t.printStackTrace(System.out);
				System.out.flush();
			}
		}
	}

	private void _tick() {
		this._warsUpdate();
	}

	private void _warsUpdate() {
		HashSet<String> warsToEnd = new HashSet<>(8);
		if (this.wars != null && this.wars.size() > 0) {
			Iterator<String> itr = this.wars.keySet().iterator();
			WordWar war;
			while (itr.hasNext()) {
				war = this.wars.get(itr.next());
				if (war.time_to_start > 0) {
					war.time_to_start--;
					switch ((int) war.time_to_start) {
						case 60:
						case 30:
						case 5:
						case 4:
						case 3:
						case 2:
						case 1:
							this.warStartCount(war);
							break;
						case 0:
							break;
						default:
							if (war.time_to_start >= (60 * 60)) {
								if (war.time_to_start % (60 * 30) == 0) {
									this.warStartCount(war);
								}
							} else if (war.time_to_start >= (60 * 20)) {
								if (war.time_to_start % (60 * 20) == 0) {
									this.warStartCount(war);
								}
							} else if (war.time_to_start == (60 * 10)) {
								this.warStartCount(war);
							} else if (war.time_to_start == (60 * 5)) {
								this.warStartCount(war);
							}
							break;
					}
					if (war.time_to_start == 0) {
						this.beginWar(war);
					}
				} else if (war.remaining > 0) {
					war.remaining--;
					switch ((int) war.remaining) {
						case 60:
						case 5:
						case 4:
						case 3:
						case 2:
						case 1:
							this.warEndCount(war);
							break;
						case 0:
							if (war.current_chain >= war.total_chains) {
								warsToEnd.add(war.getInternalName());
							}
							this.endWar(war);
							break;
						default:
							if (war.remaining >= (60 * 60)) {
								if (war.remaining % (60 * 30) == 0) {
									this.warEndCount(war);
								}
							} else if (war.remaining >= (60 * 20)) {
								if (war.remaining % (60 * 20) == 0) {
									this.warEndCount(war);
								}
							} else if (war.remaining == (60 * 10)) {
								this.warEndCount(war);
							} else if (war.remaining == (60 * 5)) {
								this.warEndCount(war);
							}
							break;
					}
				}
				war.updateDb();
			}

			warsToEnd.forEach((warName) -> this.wars.remove(warName));
		}
	}

	private void warStartCount(WordWar war) {
		if (war.time_to_start < 60) {
			Tim.bot.sendIRC().message(war.getChannel(), war.getSimpleName() + ": Starting in " + war.time_to_start + (war.time_to_start == 1 ? " second" : " seconds") + "!");
		} else {
			int time_to_start = (int) war.time_to_start / 60;
			if (time_to_start * 60 == war.time_to_start) {
				Tim.bot.sendIRC().message(war.getChannel(), war.getName(false ,true) + ": Starting in " + time_to_start + (time_to_start == 1 ? " minute" : " minutes") + "!");
			} else {
				Tim.bot.sendIRC().message(war.getChannel(), war.getName(false ,true) + ": Starting in " + new DecimalFormat("###.#").format(war.time_to_start / 60.0) + " minutes!");
			}
		}
	}

	private void warEndCount(WordWar war) {
		if (war.remaining < 60) {
			Tim.bot.sendIRC().message(war.getChannel(), war.getSimpleName() + ": " + war.remaining + (war.remaining == 1 ? " second" : " seconds") + " remaining!");
		} else {
			int remaining = (int) war.remaining / 60;
			Tim.bot.sendIRC().message(war.getChannel(), war.getSimpleName() + ": " + remaining + (remaining == 1 ? " minute" : " minutes") + " remaining.");
		}
	}

	private void beginWar(WordWar war) {
		Tim.bot.sendIRC().message(war.getChannel(), war.getSimpleName() + ": Starting now!");
		notifyWarMembers(war, war.getSimpleName() + " starts now!");

		if (war.cdata.auto_muzzle_wars && (!war.cdata.muzzled || war.cdata.auto_muzzled)) {
			war.cdata.setMuzzleFlag(true, true);
		}
	}

	private void endWar(WordWar war) {
		Tim.bot.sendIRC().message(war.getChannel(), war.getSimpleName() + ": Ending now!");
		notifyWarMembers(war, war.getSimpleName() + " is over!");

		if (war.current_chain >= war.total_chains) {
			war.endWar();
		} else {
			if (war.cdata.muzzled && war.cdata.auto_muzzled) {
				war.cdata.setMuzzleFlag(false, false);
			}

			war.current_chain++;

			if (war.do_randomness) {
				war.duration = (long) (war.base_duration + (war.base_duration * ((Tim.rand.nextInt(20) - 10) / 100.0)));
				war.time_to_start = (long) ((war.break_duration) + (war.break_duration * ((Tim.rand.nextInt(20) - 10) / 100.0)));
			} else {
				war.duration = war.base_duration;
				war.time_to_start = war.break_duration;
			}

			war.remaining = war.duration;
			war.updateDb();
			warStartCount(war);
		}
	}

	private void notifyWarMembers(WordWar war, String notice) {
		for (Map.Entry<String, Integer> entry : war.members.entrySet()) {
			Tim.bot.sendIRC().message(entry.getKey(), notice);
		}
	}

	private void joinWar(CommandData commandData) {
		if (commandData.args == null || commandData.args.length == 0) {
			commandData.event.respond("Usage: !joinwar <war id> [<starting wordcount>]");
		} else {
			int war_id, wordcount;

			try {
				war_id = Integer.parseInt(commandData.args[0]);

				if (!wars_by_id.containsKey(war_id)) {
					commandData.event.respond("That war id was not found.");
					return;
				}
			} catch (NumberFormatException exception) {
				commandData.event.respond("Could not understand first parameter. Was it a number?");
				return;
			}

			if (commandData.args.length == 2) {
				try {
					wordcount = Integer.parseInt(commandData.args[1]);
				} catch (NumberFormatException exception) {
					commandData.event.respond("Could not understand second parameter. Was it a number?");
				}
			}

			// TODO: This is a hack to get this working. Replace once we have working stats
			wordcount = 0;

			wars_by_id.get(war_id).addMember(commandData.issuer, wordcount);
			commandData.event.respond("You have joined the war.");
		}
	}

	private void leaveWar(CommandData commandData) {
		if (commandData.args == null || commandData.args.length == 0) {
			commandData.event.respond("Usage: !leavewar <war id>");
		} else {
			Integer war_id, wordcount = null;

			try {
				war_id = Integer.parseInt(commandData.args[0]);

				if (!wars_by_id.containsKey(war_id)) {
					commandData.event.respond("That war id was not found.");
					return;
				}
			} catch (NumberFormatException exception) {
				commandData.event.respond("Could not understand first parameter. Was it a number?");
				return;
			}

			wars_by_id.get(war_id).removeMember(commandData.issuer);
			commandData.event.respond("You have left the war.");
		}
	}

	// !endwar <name>
	private void endWar(CommandData commandData) {
		if (commandData.args != null && commandData.args.length > 0) {
			String name = StringUtils.join(commandData.args, " ");

			if (this.wars.containsKey(name.toLowerCase())) {
				removeWar(commandData, this.wars.get(name.toLowerCase()));
			} else {
				int war_id = 0;
				try {
					war_id = Integer.parseInt(name);
				} catch (NumberFormatException exception) {
					Logger.getLogger(WarTicker.class.getName()).log(Level.SEVERE, null, exception);
				}

				if (this.wars_by_id.containsKey(war_id)) {
					removeWar(commandData, this.wars_by_id.get(war_id));
				} else {
					commandData.event.respond("I don't know of a war with name or ID '" + name + "'.");
				}
			}
		} else {
			commandData.event.respond("Syntax: !endwar <name or war id>");
		}
	}

	private void removeWar(CommandData commandData, WordWar war) {
		if (commandData.issuer.equalsIgnoreCase(war.getStarter())
			|| Permissions.isAdmin(commandData)) {
			this.wars.remove(war.getInternalName());
			this.wars_by_id.remove(war.db_id);

			war.endWar();
			commandData.event.respond(war.getSimpleName() + " has been ended.");
		} else {
			commandData.event.respond("Only the starter of a war can end it early.");
		}
	}

	private void startChainWar(CommandData commandData) {
		long time;
		long to_start = 60;
		int total_chains;
		int delay;
		boolean do_randomness = false;

		StringBuilder warName = new StringBuilder();

		try {
			time = (long) (Double.parseDouble(commandData.args[0]) * 60);
		} catch (NumberFormatException e) {
			commandData.event.respond("I could not understand the duration parameter. Was it numeric?");
			return;
		}

		try {
			total_chains = Integer.parseInt(commandData.args[1]);
		} catch (NumberFormatException e) {
			commandData.event.respond("I could not understand the time to start parameter. Was it numeric?");
			return;
		}

		if (time < 60) {
			commandData.event.respond("Duration must be at least 1 minute.");
			return;
		}

		delay = (int) time / 2;

		Pattern breakPattern = Pattern.compile("^break:([0-9.]+)$");
		Pattern randomnessPattern = Pattern.compile("^random:([01])$");
		Pattern startDelayPattern = Pattern.compile("^start:([0-9.]+)$");
		Matcher m;

		if (commandData.args.length >= 3) {
			for (int i = 2; i < commandData.args.length; i++) {
				m = breakPattern.matcher(commandData.args[i]);
				if (m.find()) {
					try {
						delay = (int) (Double.parseDouble(m.group(1)) * 60);
					} catch (NumberFormatException e) {
						Logger.getLogger(WarTicker.class.getName()).log(Level.INFO, "Input: ''{0}'' Found String: ''{1}''", new Object[]{commandData.args[1], m.group(1)});
					}
					continue;
				}

				m = startDelayPattern.matcher(commandData.args[i]);
				if (m.find()) {
					try {
						to_start = (int) (Double.parseDouble(m.group(1)) * 60);
					} catch (NumberFormatException e) {
						Logger.getLogger(WarTicker.class.getName()).log(Level.INFO, "Input: ''{0}'' Found String: ''{1}''", new Object[]{commandData.args[1], m.group(1)});
					}
					continue;
				}

				m = randomnessPattern.matcher(commandData.args[i]);
				if (m.find()) {
					try {
						if (Integer.parseInt(m.group(1)) == 1) {
							do_randomness = true;
						}
					} catch (NumberFormatException e) {
						Logger.getLogger(WarTicker.class.getName()).log(Level.INFO, "Input: ''{0}'' Found String: ''{1}''", new Object[]{commandData.args[1], m.group(1)});
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
	}

	private void startWar(CommandData commandData) {
		long time;
		long to_start = 60;
		StringBuilder warname;
		try {
			time = (long) (Double.parseDouble(commandData.args[0]) * 60);
		} catch (NumberFormatException e) {
			commandData.event.respond("I could not understand the duration parameter. Was it numeric?");
			return;
		}

		if (commandData.args.length >= 2) {
			try {
				to_start = (long) (Double.parseDouble(commandData.args[1]) * 60);
			} catch (NumberFormatException e) {
				if (commandData.args[1].equalsIgnoreCase("now")) {
					to_start = 0;
				} else {
					commandData.event.respond("I could not understand the time to start parameter. Was it numeric?");
					return;
				}
			}
		}

		if (time < 60) {
			commandData.event.respond("Duration must be at least 1 minute.");
			return;
		}

		if (commandData.args.length >= 3) {
			warname = new StringBuilder(commandData.args[2]);
			for (int i = 3; i < commandData.args.length; i++) {
				warname.append(" ").append(commandData.args[i]);
			}
		} else {
			warname = new StringBuilder(commandData.getUserEvent().getUser().getNick() + "'s War");
		}

		if (!this.wars.containsKey(warname.toString().toLowerCase())) {
			WordWar war = new WordWar(time, to_start, 1, 1, 0, false, warname.toString(),
				commandData.getUserEvent().getUser(), commandData.event.getChannel().getName());
			this.wars.put(war.getInternalName(), war);
			war.addMember(commandData.issuer, 0);

			if (to_start > 0) {
				commandData.event.respond(String.format("Your word war, %s, will start in " + to_start / 60.0 + " minutes. The ID is: %d.", war.getSimpleName(), war.db_id));
			} else {
				this.beginWar(war);
			}
		} else {
			commandData.event.respond("There is already a war with the name '" + warname + "'");
		}
	}

	private void listAllWars(CommandData commandData) {
		this.listWars(commandData, true);
	}

	private void listWars(CommandData commandData, boolean all) {
		boolean responded = false;

		if (this.wars != null && this.wars.size() > 0) {
			int maxIdLength = 1;
			int maxDurationLength = 1;

			for (WordWar war : this.wars.values()) {
				if (war.db_id.toString().length() > maxIdLength) {
					maxIdLength = war.db_id.toString().length();
				}

				if (war.getDurationText(war.duration).length() > maxDurationLength) {
					maxDurationLength = war.getDurationText(war.duration).length();
				}
			}

			for (WordWar war : this.wars.values()) {
				if (all || war.getChannel().equalsIgnoreCase(commandData.event.getChannel().getName())) {
					commandData.event.respond(all ? war.getDescriptionWithChannel(maxIdLength, maxDurationLength) : war.getDescription(maxIdLength, maxDurationLength));
					responded = true;
				}
			}
		}

		if (!responded) {
			commandData.event.respond("No wars are currently available.");
		}
	}
}
