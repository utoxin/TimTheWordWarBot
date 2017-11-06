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

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Tim.Utility.Permissions;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.events.MessageEvent;

class WarTicker {
	WarClockThread warTicker;
	ConcurrentHashMap<String, WordWar> wars;
	ConcurrentHashMap<Integer, WordWar> wars_by_id = new ConcurrentHashMap<Integer, WordWar>();

	private WarTicker() {
		Logger.getLogger(WarTicker.class.getName()).log(Level.INFO, "War Ticker Loading...");

		Timer ticker;

		this.wars = Tim.db.loadWars();
		for (WordWar war : this.wars.values()) {
			this.wars_by_id.put(war.db_id, war);
		}

		Logger.getLogger(WarTicker.class.getName()).log(Level.INFO, "War Ticker Scheduling...");

		this.warTicker = new WarClockThread(this);
		ticker = new Timer(true);
		ticker.scheduleAtFixedRate(this.warTicker, 0, 1000);

		Logger.getLogger(WarTicker.class.getName()).log(Level.INFO, "War Ticker Loaded");
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

	void joinWar(MessageEvent event, String[] args) {
		if (event.getUser() == null) {
			return;
		}

		if (args == null || args.length == 0) {
			event.respond("Usage: !joinwar <war id> [<starting wordcount>]");
		} else {
			Integer war_id, wordcount = null;

			try {
				war_id = Integer.parseInt(args[0]);

				if (!wars_by_id.containsKey(war_id)) {
					event.respond("That war id was not found.");
					return;
				}
			} catch (NumberFormatException exception) {
				event.respond("Could not understand first parameter. Was it a number?");
				return;
			}

			if (args.length == 2) {
				try {
					wordcount = Integer.parseInt(args[1]);
				} catch (NumberFormatException exception) {
					event.respond("Could not understand second parameter. Was it a number?");
				}
			}

			// TODO: This is a hack to get this working. Replace once we have working stats
			wordcount = 0;

			wars_by_id.get(war_id).addMember(event.getUser().getNick(), wordcount);
			event.respond("You have joined the war.");
		}
	}

	void leaveWar(MessageEvent event, String[] args) {
		if (event.getUser() == null) {
			return;
		}

		if (args == null || args.length == 0) {
			event.respond("Usage: !leavewar <war id>");
		} else {
			Integer war_id, wordcount = null;

			try {
				war_id = Integer.parseInt(args[0]);

				if (!wars_by_id.containsKey(war_id)) {
					event.respond("That war id was not found.");
					return;
				}
			} catch (NumberFormatException exception) {
				event.respond("Could not understand first parameter. Was it a number?");
				return;
			}

			wars_by_id.get(war_id).removeMember(event.getUser().getNick());
			event.respond("You have left the war.");
		}
	}

	// !endwar <name>
	void endWar(MessageEvent event, String[] args) {
		if (event.getUser() == null) {
			return;
		}

		if (args != null && args.length > 0) {
			String name = StringUtils.join(args, " ");

			if (this.wars.containsKey(name.toLowerCase())) {
				removeWar(event, this.wars.get(name.toLowerCase()));
			} else {
				int war_id = 0;
				try {
					war_id = Integer.parseInt(name);
				} catch (NumberFormatException exception) {
					Logger.getLogger(WarTicker.class.getName()).log(Level.SEVERE, null, exception);
				}

				if (this.wars_by_id.containsKey(war_id)) {
					removeWar(event, this.wars_by_id.get(war_id));
				} else {
					event.respond("I don't know of a war with name or ID '" + name + "'.");
				}
			}
		} else {
			event.respond("Syntax: !endwar <name or war id>");
		}
	}

	private void removeWar(MessageEvent event, WordWar war) {
		if (event.getUser() == null) {
			return;
		}

		if (event.getUser().getNick().equalsIgnoreCase(war.getStarter())
			|| Permissions.isAdmin(event)) {
			this.wars.remove(war.getInternalName());
			this.wars_by_id.remove(war.db_id);

			war.endWar();
			event.respond(war.getSimpleName() + " has been ended.");
		} else {
			event.respond("Only the starter of a war can end it early.");
		}
	}

	void startChainWar(MessageEvent event, String[] args) {
		if (event.getUser() == null) {
			return;
		}

		long time;
		long to_start = 60;
		int total_chains;
		int delay;
		boolean do_randomness = false;

		StringBuilder warName = new StringBuilder();

		try {
			time = (long) (Double.parseDouble(args[0]) * 60);
		} catch (NumberFormatException e) {
			event.respond("I could not understand the duration parameter. Was it numeric?");
			return;
		}

		try {
			total_chains = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			event.respond("I could not understand the time to start parameter. Was it numeric?");
			return;
		}

		if (time < 60) {
			event.respond("Duration must be at least 1 minute.");
			return;
		}

		delay = (int) time / 2;

		Pattern breakPattern = Pattern.compile("^break:([0-9.]+)$");
		Pattern randomnessPattern = Pattern.compile("^random:([01])$");
		Pattern startDelayPattern = Pattern.compile("^start:([0-9.]+)$");
		Matcher m;

		if (args.length >= 3) {
			for (int i = 2; i < args.length; i++) {
				m = breakPattern.matcher(args[i]);
				if (m.find()) {
					try {
						delay = (int) (Double.parseDouble(m.group(1)) * 60);
					} catch (NumberFormatException e) {
						Logger.getLogger(WarTicker.class.getName()).log(Level.INFO, "Input: ''{0}'' Found String: ''{1}''", new Object[]{args[1], m.group(1)});
					}
					continue;
				}

				m = startDelayPattern.matcher(args[i]);
				if (m.find()) {
					try {
						to_start = (int) (Double.parseDouble(m.group(1)) * 60);
					} catch (NumberFormatException e) {
						Logger.getLogger(WarTicker.class.getName()).log(Level.INFO, "Input: ''{0}'' Found String: ''{1}''", new Object[]{args[1], m.group(1)});
					}
					continue;
				}

				m = randomnessPattern.matcher(args[i]);
				if (m.find()) {
					try {
						if (Integer.parseInt(m.group(1)) == 1) {
							do_randomness = true;
						}
					} catch (NumberFormatException e) {
						Logger.getLogger(WarTicker.class.getName()).log(Level.INFO, "Input: ''{0}'' Found String: ''{1}''", new Object[]{args[1], m.group(1)});
					}
					continue;
				}

				if (warName.toString().equals("")) {
					warName = new StringBuilder(args[i]);
				} else {
					warName.append(" ").append(args[i]);
				}
			}
		}

		if (warName.toString().equals("")) {
			warName = new StringBuilder(event.getUser().getNick() + "'s War");
		}

		if (!this.wars.containsKey(warName.toString().toLowerCase())) {
			WordWar war = new WordWar(time, to_start, total_chains, 1, delay, do_randomness, warName.toString(), event.getUser(), event.getChannel().getName());
			this.wars.put(war.getInternalName(), war);
			if (to_start > 0) {
				event.respond(String.format("Your word war, %s, will start in " + to_start / 60.0 + " minutes. The ID is: %d.", war.getSimpleName(), war.db_id));
			} else {
				this.beginWar(war);
			}
		} else {
			event.respond("There is already a war with the name '" + warName + "'");
		}
	}

	void startWar(MessageEvent event, String[] args) {
		if (event.getUser() == null) {
			return;
		}

		long time;
		long to_start = 60;
		StringBuilder warname;
		try {
			time = (long) (Double.parseDouble(args[0]) * 60);
		} catch (NumberFormatException e) {
			event.respond("I could not understand the duration parameter. Was it numeric?");
			return;
		}

		if (args.length >= 2) {
			try {
				to_start = (long) (Double.parseDouble(args[1]) * 60);
			} catch (NumberFormatException e) {
				if (args[1].equalsIgnoreCase("now")) {
					to_start = 0;
				} else {
					event.respond("I could not understand the time to start parameter. Was it numeric?");
					return;
				}
			}
		}

		if (time < 60) {
			event.respond("Duration must be at least 1 minute.");
			return;
		}

		if (args.length >= 3) {
			warname = new StringBuilder(args[2]);
			for (int i = 3; i < args.length; i++) {
				warname.append(" ").append(args[i]);
			}
		} else {
			warname = new StringBuilder(event.getUser().getNick() + "'s War");
		}

		if (!this.wars.containsKey(warname.toString().toLowerCase())) {
			WordWar war = new WordWar(time, to_start, 1, 1, 0, false, warname.toString(), event.getUser(), event.getChannel().getName());
			this.wars.put(war.getInternalName(), war);
			if (to_start > 0) {
				event.respond(String.format("Your word war, %s, will start in " + to_start / 60.0 + " minutes. The ID is: %d.", war.getSimpleName(), war.db_id));
			} else {
				this.beginWar(war);
			}
		} else {
			event.respond("There is already a war with the name '" + warname + "'");
		}
	}

	void listAllWars(MessageEvent event) {
		this.listWars(event, true);
	}

	void listWars(MessageEvent event, boolean all) {
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
				if (all || war.getChannel().equalsIgnoreCase(event.getChannel().getName())) {
					event.respond(all ? war.getDescriptionWithChannel(maxIdLength, maxDurationLength) : war.getDescription(maxIdLength, maxDurationLength));
					responded = true;
				}
			}
		}

		if (!responded) {
			event.respond("No wars are currently available.");
		}
	}
}
