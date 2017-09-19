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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.events.MessageEvent;

class WarTicker {
	WarClockThread warTicker;
	ConcurrentHashMap<String, WordWar> wars;

	private WarTicker() {
		Timer ticker;

		this.wars = Tim.db.loadWars();
		this.warTicker = new WarClockThread(this);
		ticker = new Timer(true);
		ticker.scheduleAtFixedRate(this.warTicker, 0, 1000);
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
							if ((war.time_to_start <= (60 * 20) && war.time_to_start % 300 == 0) || (war.time_to_start % 600 == 0)) {
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
								warsToEnd.add(war.getName(false).toLowerCase());
							}
							this.endWar(war);
							break;
						default:
							if ((war.remaining <= (60 * 20) && war.remaining % 300 == 0) || (war.remaining % 600 == 0)) {
								this.warEndCount(war);
							}
							// do nothing
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
			Tim.bot.sendIRC().message(war.getChannel(), war.getName() + ": Starting in " + war.time_to_start + (war.time_to_start == 1 ? " second" : " seconds") + "!");
		} else {
			int time_to_start = (int) war.time_to_start / 60;
			if (time_to_start * 60 == war.time_to_start) {
				Tim.bot.sendIRC().message(war.getChannel(), war.getName() + ": Starting in " + time_to_start + (time_to_start == 1 ? " minute" : " minutes") + "!");
			} else {
				Tim.bot.sendIRC().message(war.getChannel(), war.getName() + ": Starting in " + new DecimalFormat("###.#").format(war.time_to_start / 60.0) + " minutes!");
			}
		}
	}

	private void warEndCount(WordWar war) {
		if (war.remaining < 60) {
			Tim.bot.sendIRC().message(war.getChannel(), war.getName() + ": " + war.remaining + (war.remaining == 1 ? " second" : " seconds") + " remaining!");
		} else {
			int remaining = (int) war.remaining / 60;
			Tim.bot.sendIRC().message(war.getChannel(), war.getName() + ": " + remaining + (remaining == 1 ? " minute" : " minutes") + " remaining.");
		}
	}

	private void beginWar(WordWar war) {
		Tim.bot.sendIRC().message(war.getChannel(), "WordWar '" + war.getName() + "' starts now!");

		if (war.cdata.auto_muzzle_wars && (!war.cdata.muzzled || war.cdata.auto_muzzled)) {
			war.cdata.setMuzzleFlag(true, true);
		}
	}

	private void endWar(WordWar war) {
		Tim.bot.sendIRC().message(war.getChannel(), "WordWar '" + war.getName() + "' is over!");
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

	// !endwar <name>
	void endWar(MessageEvent event, String[] args) {
		if (event.getUser() == null) {
			return;
		}

		if (args != null && args.length > 0) {
			String name = StringUtils.join(args, " ");
			if (this.wars.containsKey(name.toLowerCase())) {
				if (event.getUser().getNick().equalsIgnoreCase(this.wars.get(name.toLowerCase()).getStarter())
					|| Tim.db.admin_list.contains(event.getUser().getNick())
					|| Tim.db.admin_list.contains(event.getChannel().getName().toLowerCase())) {
					WordWar war = this.wars.remove(name.toLowerCase());
					war.endWar();
					event.respond("The war '" + war.getName(false) + "' has been ended.");
				} else {
					event.respond("Only the starter of a war can end it early.");
				}
			} else {
				event.respond("I don't know of a war with name: '" + name + "'");
			}
		} else {
			event.respond("I need a war name to end.");
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
		boolean do_randomness = true;

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
						if (Integer.parseInt(m.group(1)) == 0) {
							do_randomness = false;
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
			this.wars.put(war.getName(false).toLowerCase(), war);
			if (to_start > 0) {
				event.respond("Your wordwar '" + warName + "' will start in " + to_start / 60.0 + " minutes.");
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
			this.wars.put(war.getName(false).toLowerCase(), war);
			if (to_start > 0) {
				event.respond("Your wordwar will start in " + to_start / 60.0 + " minutes.");
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
			for (WordWar war : this.wars.values()) {
				if (all || war.getChannel().equalsIgnoreCase(event.getChannel().getName())) {
					event.respond(all ? war.getDescriptionWithChannel() : war.getDescription());
					responded = true;
				}
			}
		}

		if (!responded) {
			event.respond("No wars are currently available.");
		}
	}
}
