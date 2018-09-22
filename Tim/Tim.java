package Tim;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import Tim.Commands.Amusement.Raptor;
import Tim.Commands.Writing.Challenge;
import Tim.Data.ChannelStorage;
import Tim.Utility.UserDirectory;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.pircbotx.Configuration;
import org.pircbotx.Configuration.Builder;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.BackgroundListenerManager;

public class Tim {
	public static  PircBotX           bot;
	public static  DBAccess           db     = DBAccess.getInstance();
	public static  Random             rand;
	public static  UserDirectory      userDirectory;
	public static  WarTicker          warticker;
	public static  TwitterIntegration twitterStream;
	public static  Raptor             raptors;
	public static  Semaphore          connectSemaphore;
	// Has to go first, because it's needed for DBAccess below
	static         AppConfig          config = AppConfig.getInstance();
	static         Amusement          amusement;
	static         Challenge          challenge;
	static         MarkovChains       markov;
	static         MarkovProcessor    markovProcessor;
	static         ChainStory         story;
	static         DeIdler            deidler;
	static         ChannelStorage     channelStorage;
	private static Tim                instance;
	private static Thread             markovThread;

	public Tim() {
		rand = new Random();
		story = new ChainStory();
		challenge = new Challenge();
		markov = new MarkovChains();
		markovProcessor = new MarkovProcessor();
		amusement = new Amusement();
		raptors = new Raptor();
		channelStorage = new ChannelStorage();
		userDirectory = new UserDirectory();

		BackgroundListenerManager backgroundListenerManager = new BackgroundListenerManager();

		// Stuff that needs to fully process all events
		backgroundListenerManager.addListener(userDirectory, true);

		// Foreground events that can stomp on eachother
		backgroundListenerManager.addListener(new UserCommandListener(), false);
		backgroundListenerManager.addListener(new ReactionListener(), false);
		backgroundListenerManager.addListener(new AdminCommandListener(), false);
		backgroundListenerManager.addListener(new ServerListener(), false);
		backgroundListenerManager.addListener(new ConnectListener(), false);

		Builder configBuilder = new Configuration.Builder().setName(db.getSetting("nickname"))
														   .setLogin("WarMech")
														   .setNickservPassword(db.getSetting("password"))
														   .addServer(db.getSetting("server"))
														   .setServerPassword(db.getSetting("server_password"))
														   .setEncoding(Charset.forName("UTF-8"))
														   .setMessageDelay(Long.parseLong(db.getSetting("max_rate")))
														   .setAutoNickChange(true)
														   .setListenerManager(backgroundListenerManager);

		db.refreshDbLists();

		// Join our channels
		db.channel_data.forEach((key, value) -> configBuilder.addAutoJoinChannel(value.channel));
		connectSemaphore = new Semaphore(1 - db.channel_data.size());

		bot = new PircBotX(configBuilder.buildConfiguration());

		Runtime.getRuntime()
			   .addShutdownHook(new Thread(() -> {
				   if (Tim.bot.isConnected()) {
					   try {
						   Tim.shutdown();
						   Thread.sleep(1000);
					   } catch (InterruptedException ex) {
						   Logger.getLogger(Tim.class.getName())
								 .log(Level.SEVERE, null, ex);
					   }
				   }
			   }));

		try {
			markovThread = new Thread(markovProcessor);
			markovThread.start();

			bot.startBot();
		} catch (Exception ex) {
			Logger.getLogger(Tim.class.getName())
				  .log(Level.SEVERE, null, ex);
		}
	}

	static void shutdown() {
		if (Tim.bot.isConnected()) {
			markovThread.interrupt();
			Tim.bot.stopBotReconnect();
			Tim.bot.sendIRC()
				   .quitServer("HELP! Utoxin just murdered me! (Again!!!)");
			Tim.warticker.warTicker.cancel();
			Tim.deidler.idleTicker.cancel();
			Tim.twitterStream.publicStream.shutdown();
		}
	}

	public static void main(String[] args) {
		instance = new Tim();
	}

	public static void printStackTrace(Throwable exception) {
		String   stackTrace      = ExceptionUtils.getStackTrace(exception);
		String[] stackTraceLines = stackTrace.split("\n");
		for (String line : stackTraceLines) {
			Tim.bot.send()
				   .message("#commandcenter", line);
		}
	}

	public static void logErrorString(String error) {
		Tim.bot.send()
			   .message("#commandcenter", error);
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
