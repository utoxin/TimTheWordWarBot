package Tim;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import Tim.Data.WordWar;

public class WarTicker {
	public HashSet<WordWar> wars;
	WarClockThread warTicker;

	private WarTicker() {
		Timer ticker;

		this.wars = Tim.db.loadWars();

		this.warTicker = new WarClockThread(this);
		ticker = new Timer(true);
		ticker.scheduleAtFixedRate(this.warTicker, 0, 1000);
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static WarTicker getInstance() {
		return SingletonHelper.instance;
	}

	private void _tick() {
		this._warsUpdate();
	}

	private void _warsUpdate() {
		if (this.wars != null && this.wars.size() > 0) {
			long currentEpoch = System.currentTimeMillis() / 1000;

			for (WordWar war : this.wars) {
				if (war.startEpoch >= currentEpoch) {
					long timeDifference = war.startEpoch - currentEpoch;

					switch ((int) timeDifference) {
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
							this.beginWar(war);
							break;

						default:
							if (timeDifference >= (60 * 60)) {
								// Alert hourly at 1+ hours of wait
								if (timeDifference % (60 * 60) == 0) {
									this.warStartCount(war);
								}
							} else if (timeDifference >= (60 * 30)) {
								// Alert at 30 minutes
								if (timeDifference % (60 * 30) == 0) {
									this.warStartCount(war);
								}
							} else if (timeDifference == (60 * 10)) {
								// Alert at 10 minutes
								this.warStartCount(war);
							} else if (timeDifference == (60 * 5)) {
								// Alert at 5 minutes
								this.warStartCount(war);
							}
					}
				} else {
					if (war.endEpoch >= currentEpoch) {
						if (war.warState == WordWar.State.PENDING) {
							// The war should have already started at this point, but for some reason hasn't.
							this.beginWar(war);
						} else {
							long timeDifference = war.endEpoch - currentEpoch;

							switch ((int) timeDifference) {
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
									if (timeDifference >= (60 * 60)) {
										if (timeDifference % (60 * 60) == 0) {
											this.warEndCount(war);
										}
									} else if (timeDifference >= (60 * 30)) {
										if (timeDifference % (60 * 30) == 0) {
											this.warEndCount(war);
										}
									} else if (timeDifference == (60 * 10)) {
										this.warEndCount(war);
									} else if (timeDifference == (60 * 5)) {
										this.warEndCount(war);
									}
							}
						}
					} else {
						this.endWar(war);
					}
				}
			}
		}
	}

	private void warStartCount(WordWar war) {
		long timeToStart = war.startEpoch - (System.currentTimeMillis() / 1000);

		if (timeToStart < 60) {
			Tim.bot.sendIRC()
				   .message(war.getChannel(), war.getSimpleName() + ": Starting in " + timeToStart + (timeToStart == 1 ? " second" : " seconds") + ".");
		} else {
			int minutes = (int) timeToStart / 60;
			if (minutes * 60 == timeToStart) {
				Tim.bot.sendIRC()
					   .message(war.getChannel(), war.getName(false, true) + ": Starting in " + minutes + (minutes == 1 ? " minute" : " minutes") + ".");
			} else {
				Tim.bot.sendIRC()
					   .message(war.getChannel(),
								war.getName(false, true) + ": Starting in " + new DecimalFormat("###.#").format(timeToStart / 60.0) + " minutes.");
			}
		}
	}

	private void beginWar(WordWar war) {
		long   currentEpoch = System.currentTimeMillis() / 1000;
		long   lateStart    = currentEpoch - war.startEpoch;
		String appendString = "";

		if (lateStart >= 5) {
			Tim.logErrorString(String.format("War starting %d seconds late in %s.", lateStart, war.channel));
			appendString = String.format(" (%d seconds late. Sorry!)", lateStart);
		}

		Tim.bot.sendIRC()
			   .message(war.getChannel(), war.getSimpleName() + ": Starting now!" + appendString);
		this.notifyWarMembers(war, war.getSimpleName() + " starts now!" + appendString);
		war.beginWar();
	}

	private void warEndCount(WordWar war) {
		long timeToEnd = war.endEpoch - (System.currentTimeMillis() / 1000);

		if (timeToEnd < 60) {
			Tim.bot.sendIRC()
				   .message(war.getChannel(), war.getSimpleName() + ": " + timeToEnd + (timeToEnd == 1 ? " second" : " seconds") + " remaining!");
		} else {
			int remaining = (int) timeToEnd / 60;
			Tim.bot.sendIRC()
				   .message(war.getChannel(), war.getSimpleName() + ": " + remaining + (remaining == 1 ? " minute" : " minutes") + " remaining.");
		}
	}

	private void endWar(WordWar war) {
		long currentEpoch = System.currentTimeMillis() / 1000;
		long lateEnd      = currentEpoch - war.endEpoch;

		String appendString = "";
		if (lateEnd >= 5) {
			Tim.logErrorString(String.format("War ending %d seconds late in %s.", lateEnd, war.channel));
			appendString = String.format(" (%d seconds late. Sorry!)", lateEnd);
		}

		Tim.bot.sendIRC()
			   .message(war.getChannel(), war.getSimpleName() + ": Ending now!" + appendString);
		this.notifyWarMembers(war, war.getSimpleName() + " is over!" + appendString);

		if (war.currentChain >= war.totalChains) {
			war.endWar();
			this.wars.remove(war);
		} else {
			war.currentChain++;

			if (war.randomness) {
				war.startEpoch = war.endEpoch + (long) ((war.baseBreak) + (war.baseBreak * ((Tim.rand.nextInt(20) - 10) / 100.0)));
				war.endEpoch = war.startEpoch + (long) (war.baseDuration + (war.baseDuration * ((Tim.rand.nextInt(20) - 10) / 100.0)));
			} else {
				war.startEpoch = war.endEpoch + war.baseBreak;
				war.endEpoch = war.startEpoch + war.baseDuration;
			}

			war.chainWarBreak();
			warStartCount(war);
		}
	}

	private void notifyWarMembers(WordWar war, String notice) {
		for (String nick : war.warMembers) {
			Tim.bot.sendIRC()
				   .message(nick, notice);
		}
	}

	private static class SingletonHelper {
		private static final WarTicker instance = new WarTicker();
	}

	class WarClockThread extends TimerTask {
		private final WarTicker parent;

		WarClockThread(WarTicker parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			try {
				this.parent._tick();
			} catch (Throwable ex) {
				Tim.printStackTrace(ex);
			}
		}
	}
}
