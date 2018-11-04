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

		// This logic makes Timmy less likely to write the more ahead of pace he is.
		// This should keep him ahead of pace, without making him race ludicrously far ahead of the goal.
		double relativePace = Math.max(0.5, Tim.story.wordcount() / ((cal.get(Calendar.DAY_OF_MONTH) + 1) * (50000 / 30.0)));
		double oddsOfWriting = (1 / (relativePace * relativePace)) * 2.5;

		if (isNovember && (Tim.rand.nextFloat() * 100) < oddsOfWriting) {
			String new_text;
			int seedWord = Tim.markov.getSeedWord(Tim.story.getLastLines(), "novel", 0);
			new_text = Tim.markov.generate_markov("novel", seedWord);

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

		if (Tim.rand.nextInt(1000) < 5) {
			if (Tim.twitterStream != null) {
				Tim.twitterStream.sendDeidleTweet(Tim.markov.generateTwitterMarkov());
			}
		}

		Collection<ChannelInfo> channels = Tim.db.channel_data.values();
		for (ChannelInfo cdata : channels) {
			cdata = Tim.db.channel_data.get(cdata.channel);

			if (cal.get(Calendar.MINUTE) % 30 == 0 && cdata.raptorStrengthBoost > 0) {
				cdata.raptorStrengthBoost--;
				Tim.db.saveChannelSettings(cdata);
			}

			// Maybe cure the stale channel mode warnings?
			if (cdata.channel != null && Tim.channelStorage.channelList.get(cdata.channel) != null) {
				Tim.channelStorage.channelList.get(cdata.channel)
											  .getMode();
			} else {
				Tim.logErrorString(String.format("Failed to load cdata for channel %s", cdata.channel));
				continue;
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
