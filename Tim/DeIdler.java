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

import java.util.*;

/**
 *
 * @author Matthew Walker
 */
public class DeIdler {

	private static final DeIdler instance;
	public DeIdler.IdleClockThread idleticker;
	private final Timer ticker;
	private final Calendar cal;

	static {
		instance = new DeIdler();
	}

	public DeIdler() {
		this.cal = Calendar.getInstance();
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

		private final DeIdler parent;

		public IdleClockThread(DeIdler parent) {
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

	public void _tick() {
		boolean isNovember = (10 == cal.get(Calendar.MONTH));
		boolean aheadOfPace = (((cal.get(Calendar.DAY_OF_MONTH) + 1) * (50000 / 30)) < Tim.story.wordcount());

		if (isNovember && ((!aheadOfPace && Tim.rand.nextInt(100) < 8) || (aheadOfPace && Tim.rand.nextInt(100) < 4))) {
			String name = Tim.story.getRandomName();
			String new_text;
			if (Tim.rand.nextInt(100) < 75) {
				new_text = name + " " + Tim.markov.generate_markov("emote", Tim.rand.nextInt(350) + 150, 0) + ".";
			} else if (Tim.rand.nextBoolean()) {
				new_text = "\"" + Tim.markov.generate_markov("say", Tim.rand.nextInt(250) + 50, 0) + ",\" " + name + " muttered quietly.";
			} else {
				new_text = "\"" + Tim.markov.generate_markov("say", Tim.rand.nextInt(250) + 50, 0) + ",\" " + name + " said.";
			}

			Tim.story.storeLine(new_text, "Timmy");
			Tim.db.channel_data.values().stream().filter((cdata) -> (Tim.rand.nextInt(100) < 15 && cdata.chatter_enabled.get("chainstory") && !cdata.muzzled && cdata.randomChatterLevel >= 0)).forEach((cdata) -> {
				Tim.bot.sendIRC().action(cdata.channel, "opens up his novel file, considers for a minute, and then rapidly types in several words. (Help Timmy out by using the Chain Story commands. See !help for information.)");
			});
		}

		if (Tim.rand.nextInt(100) < 1) {
			Tim.twitterstream.sendDeidleTweet(Tim.markov.generate_markov("say"));
		}

		/**
		 * This loop is used to reduce the chatter odds on idle channels, by periodically triggering idle chatter in
		 * channels. If they currently have chatter turned off, this simply decreases their timer, and then goes on.
		 * That way, the odds don't build up to astronomical levels while people are idle or away, resulting in lots of
		 * spam when they come back.
		 */
		for (ChannelInfo cdata : Tim.db.channel_data.values()) {
			cdata = Tim.db.channel_data.get(cdata.channel);

			// Maybe cure the stale channel mode warnings?
			Tim.bot.getUserChannelDao().getChannel(cdata.channel).getMode();

			if (Tim.rand.nextInt(100) < cdata.randomChatterLevel) {
				ArrayList<String> actions = new ArrayList<>();

				if (cdata.muzzled) {
					continue;
				}

				if (cdata.chatter_enabled.get("markov")) {
					actions.add("markov");
				}
				
				if (cdata.amusement_chatter_available()) {
					actions.add("amusement");
				}
				
				if (cdata.chatter_enabled.get("velociraptor")) {
					actions.add("velociraptors");
				}
				
				if (actions.isEmpty()) {
					continue;
				}

				String action = actions.get(Tim.rand.nextInt(actions.size()));
				switch (action) {
					case "markov":
						Tim.markov.randomAction(cdata.channel, Tim.rand.nextBoolean() ? "say" : "emote", "");
						break;
					case "amusement":
						Tim.amusement.randomAction(null, cdata.channel);
						break;
					case "velociraptors":
						Tim.raptors.swarm(cdata.channel);
						break;
				}
			}
		}
	}
}
