/**
 * This file is part of Timmy, the Wordwar Bot.
 *
 * Timmy is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Timmy is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Timmy. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package Tim;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.apache.commons.lang.StringUtils;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author mwalker
 */
public class WarTicker {
	private static WarTicker instance;
	public WarClockThread warticker;
	private Semaphore wars_lock;
	private Timer ticker;
	public Map<String, WordWar> wars;

	static {
		instance = new WarTicker();
	}

	public WarTicker() {
		this.wars = Tim.db.loadWars();

		this.warticker = new WarClockThread(this);
		this.wars_lock = new Semaphore(1, true);
		this.ticker = new Timer(true);
		this.ticker.scheduleAtFixedRate(this.warticker, 0, 1000);
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static WarTicker getInstance() {
		return instance;
	}

	public class WarClockThread extends TimerTask {
		private WarTicker parent;

		public WarClockThread( WarTicker parent ) {
			this.parent = parent;
		}

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

	public void _tick() {
		this._warsUpdate();
	}

	private void _warsUpdate() {
		if (this.wars != null && this.wars.size() > 0) {
			try {
				this.wars_lock.acquire();
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
								// 0 seconds until start. Don't say a damn thing.
								break;
							default:
								if ((int) war.time_to_start % 300 == 0) {
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
								this.endWar(war);
								break;
							default:
								if ((int) war.remaining % 300 == 0) {
									this.warEndCount(war);
								}
								// do nothing
								break;
						}
					}
					war.updateDb();
				}
				this.wars_lock.release();
			} catch (Throwable e) {
				this.wars_lock.release();
			}
		}
	}

	private void warStartCount( WordWar war ) {
		if (war.time_to_start < 60) {
			Tim.bot.sendMessage(war.getChannel(), war.getName() + ": Starting in " + war.time_to_start + ( war.time_to_start == 1 ? " second" : " seconds" ) + "!");
		} else {
			int time_to_start = (int) war.time_to_start / 60;
			if (time_to_start * 60 == war.time_to_start) {
				Tim.bot.sendMessage(war.getChannel(), war.getName() + ": Starting in " + time_to_start + ( time_to_start == 1 ? " minute" : " minutes" ) + "!");
			} else {
				Tim.bot.sendMessage(war.getChannel(), war.getName() + ": Starting in " + new DecimalFormat("###.#").format(war.time_to_start / 60.0) + " minutes!");
			}
		}
	}

	private void warEndCount( WordWar war ) {
		if (war.remaining < 60) {
			Tim.bot.sendMessage(war.getChannel(), war.getName() + ": " + war.remaining + ( war.remaining == 1 ? " second" : " seconds" ) + " remaining!");
		} else {
			int remaining = (int) war.remaining / 60;
			Tim.bot.sendMessage(war.getChannel(), war.getName() + ": " + remaining + ( remaining == 1 ? " minute" : " minutes" ) + " remaining.");
		}
	}

	private void beginWar( WordWar war ) {
		Tim.bot.sendMessage(war.getChannel(), "WordWar '" + war.getName() + "' starts now! (" + war.getDuration() / 60 + " minutes)");
	}

	private void endWar( WordWar war ) {
		Tim.bot.sendMessage(war.getChannel(), "WordWar '" + war.getName() + "' is over!");
		if (war.current_chain >= war.total_chains) {
			this.wars.remove(war.getName(false).toLowerCase());
			war.endWar();
		} else {
			war.current_chain++;
			war.remaining = war.duration;
			war.time_to_start = (long) ( (war.duration * 0.5) + (war.duration * ((Tim.rand.nextInt(50) - 25)/100.0) ) );
			war.updateDb();
			warStartCount(war);
		}
	}

	// !endwar <name>
	public void endWar( MessageEvent event, String[] args ) {
		if (args != null && args.length > 0) {
			String name = StringUtils.join(args, " ");
			if (this.wars.containsKey(name.toLowerCase())) {
				if (event.getUser().canEqual(this.wars.get(name.toLowerCase()).getStarter())
					|| Tim.db.admin_list.contains(event.getUser().getNick())
					|| Tim.db.admin_list.contains(event.getChannel().getName().toLowerCase())) {
					WordWar war = this.wars.remove(name.toLowerCase());
					war.endWar();
					event.respond("The war '" + war.getName() + "' has been ended.");
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

	public void startChainWar( MessageEvent event, String[] args ) {
		long time;
		long to_start = 60;
		int total_chains = 1;
		String warname;

		try {
			time = (long) ( Double.parseDouble(args[0]) * 60 );
		} catch (Exception e) {
			event.respond("I could not understand the duration parameter. Was it numeric?");
			return;
		}

		try {
			total_chains = Integer.parseInt(args[1]);
		} catch (Exception e) {
			if (args[1].equalsIgnoreCase("now")) {
				to_start = 0;
			} else {
				event.respond("I could not understand the time to start parameter. Was it numeric?");
				return;
			}
		}

		if (time < 60) {
			event.respond("Duration must be at least 1 minute.");
			return;
		}

		if (args.length >= 3) {
			warname = args[2];
			for (int i = 3; i < args.length; i++) {
				warname = warname + " " + args[i];
			}
		} else {
			warname = event.getUser().getNick() + "'s War";
		}

		if (!this.wars.containsKey(warname.toLowerCase())) {
			WordWar war = new WordWar(time, to_start, total_chains, 1, warname, event.getUser(), event.getChannel());
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
	
	public void startWar( MessageEvent event, String[] args ) {
		long time;
		long to_start = 60;
		String warname;
		try {
			time = (long) ( Double.parseDouble(args[0]) * 60 );
		} catch (Exception e) {
			event.respond("I could not understand the duration parameter. Was it numeric?");
			return;
		}

		if (args.length >= 2) {
			try {
				to_start = (long) ( Double.parseDouble(args[1]) * 60 );
			} catch (Exception e) {
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
			warname = args[2];
			for (int i = 3; i < args.length; i++) {
				warname = warname + " " + args[i];
			}
		} else {
			warname = event.getUser().getNick() + "'s War";
		}

		if (!this.wars.containsKey(warname.toLowerCase())) {
			WordWar war = new WordWar(time, to_start, 1, 1, warname, event.getUser(), event.getChannel());
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

	public void listAllWars( MessageEvent event ) {
		this.listWars(event, true);
	}

	public void listWars( MessageEvent event, boolean all ) {
		if (this.wars != null && this.wars.size() > 0) {
			for (Map.Entry<String, WordWar> wm : this.wars.entrySet()) {
				if (all || wm.getValue().getChannel().getName().toLowerCase().equals(event.getChannel().getName().toLowerCase())) {
					event.respond(all ? wm.getValue().getDescriptionWithChannel() : wm.getValue().getDescription());
				}
			}
		} else {
			event.respond("No wars are currently available.");
		}
	}
}
