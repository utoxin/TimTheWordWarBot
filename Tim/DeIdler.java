/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Tim;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Matthew Walker
 */
public class DeIdler {
	private static DeIdler instance;
	public DeIdler.IdleClockThread idleticker;
	private Timer ticker;

	static {
		instance = new DeIdler();
	}

	public DeIdler() {
		this.idleticker = new DeIdler.IdleClockThread(this);
		this.ticker = new Timer(true);
		this.ticker.scheduleAtFixedRate(this.idleticker, 0, 60000);
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static DeIdler getInstance() {
		return instance;
	}

	public class IdleClockThread extends TimerTask {
		private DeIdler parent;

		public IdleClockThread( DeIdler parent ) {
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
		if (Tim.rand.nextInt(100) < 7) {
			String new_text;
			if (Tim.rand.nextBoolean()) {
				new_text = "\"" + Tim.markhov.generate_markhov("say") + ",\" Timmy said.";
			} else {
				new_text = "Timmy " + Tim.markhov.generate_markhov("emote") + ".";
			}
			
			Tim.story.storeLine(new_text, "Timmy");
			for (ChannelInfo cdata : Tim.db.channel_data.values()) {
				if (Tim.rand.nextInt(100) < 10) {
					Tim.bot.sendAction(cdata.channel, "opens up his novel file, considers for a minute, and then rapidly types in several words. (Help Timmy out by using the Chain Story commands. See !help for information.)");
				}
			}
		}
		
		/**
		 * This loop is used to reduce the chatter odds on idle channels, by periodically triggering idle chatter in
		 * channels. If they currently have chatter turned off, this simply decreases their timer, and then goes on.
		 * That way, the odds don't build up to astronomical levels while people are idle or away, resulting in lots of
		 * spam when they come back.
		 */
		for (ChannelInfo cdata : Tim.db.channel_data.values()) {
			cdata = Tim.db.channel_data.get(cdata.channel.getName().toLowerCase());

			long elapsed = System.currentTimeMillis() / 1000 - cdata.chatterTimer;
			long odds = Math.round(Math.sqrt(elapsed) / (6 - cdata.chatterLevel));

			if (odds < (cdata.chatterLevel * 4)) {
				continue;
			}

			if (Tim.rand.nextInt(100) < odds) {
				String[] actions;

				int newDivisor = cdata.chatterTimeDivisor;
				if (newDivisor > 1) {
					newDivisor -= 1;
				}
				cdata.chatterTimer += Tim.rand.nextInt((int) elapsed / newDivisor);
				elapsed = System.currentTimeMillis() / 1000 - cdata.chatterTimer;
				cdata.chatterTimer += Math.round(elapsed / 2);

				if (50 < Tim.rand.nextInt(100) || cdata.chatterLevel == 0) {
					continue;
				}

				if (cdata.doMarkov && !cdata.doRandomActions) {
					actions = new String[] {
						"markhov",};
				} else if (cdata.doMarkov && cdata.doRandomActions) {
					actions = new String[] {
						"markhov",
						"amusement",
						"amusement",};
				} else if (!cdata.doMarkov && cdata.doRandomActions) {
					actions = new String[] {
						"amusement",};
				} else {
					continue;
				}

				String action = actions[Tim.rand.nextInt(actions.length)];

				if ("markhov".equals(action)) {
					Tim.markhov.randomAction(cdata.channel, "say");
				} else if ("amusement".equals(action)) {
					Tim.amusement.randomAction(null, cdata.channel);
				}
			}
		}
	}
}
