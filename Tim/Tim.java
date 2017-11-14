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

import java.nio.charset.Charset;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import Tim.Data.ChannelStorage;
import Tim.Commands.Writing.Challenge;
import org.pircbotx.Configuration;
import org.pircbotx.Configuration.Builder;
import org.pircbotx.PircBotX;

public class Tim {
	// Has to go first, because it's needed for DBAccess below
	static AppConfig config = AppConfig.getInstance();

	public static PircBotX bot;
	public static DBAccess db = DBAccess.getInstance();
	public static Random rand;

	static Amusement amusement;
	static Challenge challenge;
	static MarkovChains markov;
	static MarkovProcessor markovProcessor;
	static ChainStory story;
	static WarTicker warticker;
	static DeIdler deidler;
	static TwitterIntegration twitterStream;
	static VelociraptorHandler raptors;
	static ChannelStorage channelStorage;

	private static Tim instance;
	private static Thread markovThread;

	public static void main(String[] args) {
		instance = new Tim();
	}

	public Tim() {
		rand = new Random();
		story = new ChainStory();
		challenge = new Challenge();
		markov = new MarkovChains();
		markovProcessor = new MarkovProcessor();
		amusement = new Amusement();
		raptors = new VelociraptorHandler();
		channelStorage = new ChannelStorage();

		Builder configBuilder = new Configuration.Builder()
			.setName(db.getSetting("nickname"))
			.setLogin("WarMech")
			.setNickservPassword(db.getSetting("password"))
			.addServer(db.getSetting("server"))
			.setServerPassword(db.getSetting("server_password"))
			.setEncoding(Charset.forName("UTF-8"))
			.setMessageDelay(Long.parseLong(db.getSetting("max_rate")))
			.setAutoNickChange(true)
			.addListener(new AdminCommandListener())
			.addListener(new UserCommandListener())
			.addListener(new ReactionListener())
			.addListener(new ServerListener());

		db.refreshDbLists();

		// Join our channels
		db.channel_data.forEach((key, value) -> configBuilder.addAutoJoinChannel(value.channel));

		bot = new PircBotX(configBuilder.buildConfiguration());

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (Tim.bot.isConnected()) {
				try {
					Tim.shutdown();
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}));

		try {
			markovThread = new Thread(markovProcessor);
			markovThread.start();

			bot.startBot();
		} catch (Exception ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	static void shutdown() {
		if (Tim.bot.isConnected()) {
			markovThread.interrupt();
			Tim.bot.stopBotReconnect();
			Tim.bot.sendIRC().quitServer("HELP! Utoxin just murdered me! (Again!!!)");
			Tim.warticker.warTicker.cancel();
			Tim.deidler.idleTicker.cancel();
			Tim.twitterStream.userStream.shutdown();
			Tim.twitterStream.publicStream.shutdown();
		}
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static Tim getInstance() {
		return instance;
	}
}
