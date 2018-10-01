package Tim;

import java.util.*;

import Tim.Data.ChannelInfo;
import org.pircbotx.Channel;

/**
 * @author Matthew Walker
 */
class DeIdler {
	DeIdler.IdleClockThread idleTicker;

	private DeIdler() {
		Timer ticker;

		this.idleTicker = new DeIdler.IdleClockThread(this);
		ticker = new Timer(true);
		ticker.scheduleAtFixedRate(this.idleTicker, 0, 60000);
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static DeIdler getInstance() {
		return SingletonHelper.instance;
	}

	private void _tick() {
		Calendar cal = Calendar.getInstance();

		boolean isNovember  = (Calendar.NOVEMBER == cal.get(Calendar.MONTH));
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
			Tim.db.channel_data.values()
							   .stream()
							   .filter((cdata) -> (Tim.rand.nextInt(100) < 15 && cdata.chatter_enabled.get("chainstory") && !cdata.isMuzzled()
												   && cdata.randomChatterLevel >= 0))
							   .forEach((cdata) -> Tim.bot.sendIRC()
														  .action(cdata.channel,
																  "opens up his novel file, considers for a minute, and then rapidly types in several words. "
																  + "(Help Timmy out by using the Chain Story commands. See !help for information.)"));
		}

		if (Tim.rand.nextInt(100) < 1) {
			if (Tim.twitterStream != null) {
				Tim.twitterStream.sendDeidleTweet(Tim.markov.generate_markov());
			}
		}

		Collection<ChannelInfo> channels = Tim.db.channel_data.values();
		for (ChannelInfo cdata : channels) {
			cdata = Tim.db.channel_data.get(cdata.channel);

			if (cal.get(Calendar.MINUTE) % 10 == 0 && cdata.raptorStrengthBoost > 0) {
				cdata.raptorStrengthBoost--;
			}

			// Maybe cure the stale channel mode warnings?
			if (cdata.channel != null && Tim.channelStorage.channelList.get(cdata.channel) != null) {
				Tim.channelStorage.channelList.get(cdata.channel)
											  .getMode();
			} else {
				Tim.logErrorString(String.format("Failed to load cdata for channel %s", cdata.channel));
			}

			cdata.clearTimedMuzzle();

			if (cdata.isMuzzled()) {
				continue;
			}

			if ((Tim.rand.nextFloat() * 100) < cdata.randomChatterLevel) {
				ArrayList<String> actions = new ArrayList<>();

				if (cdata.chatter_enabled.get("markov")) {
					actions.add("markov");
				}

				if (cdata.amusement_chatter_available()) {
					actions.add("amusement");
				}

				if (cdata.chatter_enabled.get("bored")) {
					actions.add("bored");
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
					case "bored":
						Channel sendChannel = Tim.channelStorage.channelList.get(cdata.channel);
						sendChannel.send()
								   .message("I'm bored.");
						break;
				}
			}
		}
	}

	private static class SingletonHelper {
		private static final DeIdler instance = new DeIdler();
	}

	class IdleClockThread extends TimerTask {
		private final DeIdler parent;

		IdleClockThread(DeIdler parent) {
			this.parent = parent;
		}

		@Override
		public void run() {
			try {
				this.parent._tick();
			} catch (Throwable t) {
				Tim.printStackTrace(t);
			}
		}
	}
}
