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

import java.sql.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import snaq.db.ConnectionPool;

public final class Tim extends PircBot {
	public static AppConfig config = AppConfig.getInstance();

	public class WarClockThread extends TimerTask {
		private Tim parent;

		public WarClockThread( Tim pparent ) {
			this.parent = pparent;
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

	protected enum ActionType {
		MESSAGE, ACTION, NOTICE
	};

	public class DelayCommand extends TimerTask {
		private ActionType type;
		private Tim parent;
		private String text;
		private String target;

		public DelayCommand( Tim pparent, String target, String text,
							 ActionType type ) {
			this.parent = pparent;
			this.text = text;
			this.target = target;
			this.type = type;
		}

		public void run() {
			try {
				switch (this.type) {
					case MESSAGE:
						this.parent.sendMessage(this.target, this.text);
						break;
					case ACTION:
						this.parent.sendAction(this.target, this.text);
						break;
					case NOTICE:
						this.parent.sendNotice(this.target, this.text);
				}
			} catch (Throwable t) {
				System.out.println("&&& THROWABLE CAUGHT in DelayCommand.run:");
				t.printStackTrace(System.out);
				System.out.flush();
				this.parent.sendMessage(this.target,
					"I couldn't schedule your command for some reason:");
				this.parent.sendMessage(this.target, t.toString());
			}

		}
	}

	/**
	 * Helps keep track of channel information.
	 */
	private class ChannelInfo {
		public String Name;
		public boolean isAdult;
		public boolean doMarkhov;
		public boolean doRandomActions;
		public boolean doCommandActions;
		public long chatterTimer;
		public int chatterMaxBaseOdds;
		public int chatterNameMultiplier;
		public int chatterTimeMultiplier;
		public int chatterTimeDivisor;

		/**
		 * Construct channel with default flags.
		 *
		 * @param name What is the name of the channel
		 */
		public ChannelInfo( String name ) {
			this.Name = name;
			this.isAdult = false;
			this.doCommandActions = true;
			this.doRandomActions = true;
			this.doMarkhov = true;
		}

		/**
		 * Construct channel by specifying values for flags.
		 *
		 * @param name    What is the name of the channel
		 * @param adult   Is the channel considered 'adult'
		 * @param markhov Should markhov chain processing and generation happen on channel
		 * @param random  Should random actions happen on this channel
		 * @param command Should 'fun' commands be processed on channel
		 */
		public ChannelInfo( String name, boolean adult, boolean markhov, boolean random, boolean command ) {
			this.Name = name;
			this.isAdult = adult;
			this.doRandomActions = random;
			this.doCommandActions = command;
			this.doMarkhov = markhov;
		}

		public void setChatterTimers( int maxBaseOdds, int nameMultiplier, int timeMultiplier, int timeDivisor ) {
			this.chatterMaxBaseOdds = maxBaseOdds;
			this.chatterNameMultiplier = nameMultiplier;
			this.chatterTimeMultiplier = timeMultiplier;
			this.chatterTimeDivisor = timeDivisor;

			if (this.chatterMaxBaseOdds == 0) {
				this.chatterMaxBaseOdds = 20;
			}

			if (this.chatterNameMultiplier == 0) {
				this.chatterNameMultiplier = 4;
			}

			if (this.chatterTimeMultiplier == 0) {
				this.chatterTimeMultiplier = 4;
			}

			if (this.chatterTimeDivisor == 0) {
				this.chatterTimeDivisor = 2;
			}

			this.chatterTimer = System.currentTimeMillis() / 1000;
		}
	}
	protected Set<String> admin_list = new HashSet<String>(16);
	private Set<String> ignore_list = new HashSet<String>(16);
	private Hashtable<String, ChannelInfo> channel_data = new Hashtable<String, ChannelInfo>(62);
	private List<String> greetings = new ArrayList<String>();
	private Map<String, WordWar> wars;
	private WarClockThread warticker;
	private Timer ticker;
	private Semaphore wars_lock;
	protected Random rand;
	private boolean shutdown;
	private String password;
	protected String debugChannel;
	protected ConnectionPool pool;
	private ChainStory story;
	private Challenge challenge;
	private MarkhovChains markhov;
	private Amusement amusement;
	private long timeout = 3000;

	public Tim() {
		Class c;
		Driver driver;

		/**
		 * Make sure the JDBC driver is initialized. Used by the connection pool.
		 *
		 * This try/catch block seems excessive to me, but it's what NetBeans suggested, and I'm not very experienced
		 * with java, so... here it is.
		 */
		try {
			c = Class.forName("com.mysql.jdbc.Driver");
			driver = (Driver) c.newInstance();
			DriverManager.registerDriver(driver);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		// Initialize the connection pool, to prevent SQL timeout issues
		String url = "jdbc:mysql://" + Tim.config.getString("sql_server") + ":3306/" + Tim.config.getString("sql_database");
		pool = new ConnectionPool("local", 2, 5, 10, 180000, url, Tim.config.getString("sql_user"), Tim.config.getString("sql_password"));

		this.setName(getSetting("nickname"));
		this.password = getSetting("password");
		this.debugChannel = getSetting("debug_channel");
		if (this.debugChannel.equals("")) {
			// Ideally, we should fail here...
			this.debugChannel = "#timmydebug";
		}

		// Read message delay from DB, but never go below 100ms.
		long delay = Long.parseLong(getSetting("max_rate"));
		delay = Math.max(delay, 100);
		this.setMessageDelay(delay);

		this.story = new ChainStory(this);
		this.challenge = new Challenge(this);
		this.markhov = new MarkhovChains(this);
		this.amusement = new Amusement(this);

		this.refreshDbLists();

		this.wars = Collections.synchronizedMap(new Hashtable<String, WordWar>());

		this.warticker = new WarClockThread(this);
		this.ticker = new Timer(true);
		this.ticker.scheduleAtFixedRate(this.warticker, 0, 1000);
		this.wars_lock = new Semaphore(1, true);

		this.rand = new Random();
		this.shutdown = false;
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
	}

	/**
	 * Sends an message with a delay
	 *
	 * @param target The user or channel to send the message to
	 * @param action The string for the text of the message
	 * @param delay  The delay in milliseconds
	 */
	public void sendDelayedMessage( String target, String message, int delay ) {
		DelayCommand talk = new DelayCommand(this, target, message,
			ActionType.MESSAGE);
		this.ticker.schedule(talk, delay);
	}

	/**
	 * Sends an action with a delay
	 *
	 * @param target The user or channel to send the action to
	 * @param action The string for the text of the action
	 * @param delay  The delay in milliseconds
	 */
	public void sendDelayedAction( String target, String action, int delay ) {
		DelayCommand act = new DelayCommand(this, target, action,
			ActionType.ACTION);
		this.ticker.schedule(act, delay);
	}

	/**
	 * Sends a notice with a delay
	 *
	 * @param target The user or channel to send the notice to
	 * @param action The string for the text of the notice
	 * @param delay  The delay in milliseconds
	 */
	public void sendDelayedNotice( String target, String action, int delay ) {
		DelayCommand act = new DelayCommand(this, target, action,
			ActionType.NOTICE);
		this.ticker.schedule(act, delay);
	}

	@Override
	protected void onAction( String sender, String login, String hostname,
							 String target, String action ) {

		ChannelInfo cdata = this.channel_data.get(target.toLowerCase());

		if (this.admin_list.contains(sender)) {
			if (action.equalsIgnoreCase("punches " + this.getNick()
										+ " in the face!")) {
				this.sendAction(target, "falls over and dies.  x.x");
				this.shutdown = true;
				this.quitServer();
				System.exit(0);
			}
		}

		if (!sender.equals(this.getNick()) && !"".equals(target)) {
			this.interact(sender, target, action, "emote");
			if (cdata.doMarkhov) {
				markhov.process_markhov(action, "emote");
			}
		}
	}

	@Override
	public void onMessage( String channel, String sender, String login,
						   String hostname, String message ) {

		ChannelInfo cdata = this.channel_data.get(channel.toLowerCase());

		if (!this.ignore_list.contains(sender)) {
			// Find all messages that start with ! and pass them to a method for
			// further processing.
			if (message.charAt(0) == '!') {
				this.doCommand(channel, sender, "!", message);
				return;
			} // Notation for wordcounts
			else if (message.charAt(0) == '@') {
				this.doCommand(channel, sender, "@", message);
				return;
			} else if (message.charAt(0) == '$') {
				this.doAdmin(channel, sender, '$', message.substring(1));
				return;
			}

			// Other fun stuff we can make him do
			if (message.toLowerCase().contains("hello")
				&& message.toLowerCase().contains(
				this.getNick().toLowerCase())) {
				this.sendMessage(channel, "Hi, " + sender + "!");
				return;
			} else if (message.toLowerCase().contains("how many lights")) {
				this.sendMessage(channel, "There are FOUR LIGHTS!");
				return;
			} else if (message.contains(":(") || message.contains("):")) {
				this.sendAction(channel, "gives " + sender + " a hug");
				return;
			} else if (message.contains(":'(")) {
				this.sendAction(channel, "passes " + sender + " a tissue");
				return;
			} else if (message.toLowerCase().contains(
				"are you thinking what i'm thinking")
					   || message.toLowerCase().contains(
				"are you pondering what i'm pondering")) {
				int i = this.rand.nextInt(amusement.aypwips.size());
				this.sendMessage(channel,
					String.format(amusement.aypwips.get(i), sender));
				return;
			} else {
				if (Pattern.matches(
					"(?i).*how do i (change|set) my (nick|name).*",
					message)) {
					this.sendMessage(
						channel,
						String.format(
						"%s: To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere",
						sender));
					return;
				} else if (Pattern.matches("(?i)" + this.getNick() + ".*[?]",
					message)) {
					int r = this.rand.nextInt(100);

					if (r < 50) {
						amusement.eightball(channel, sender, false);
						return;
					}
				} else if (Pattern.matches("(?i).*markhov test.*", message)) {
					this.sendDelayedMessage(channel, markhov.generate_markhov("say"), this.rand.nextInt(1500));
					return;
				}
			}

			if (!sender.equals(this.getNick()) && !"".equals(channel)) {
				this.interact(sender, channel, message, "say");
				if (cdata.doMarkhov) {
					markhov.process_markhov(message, "say");
				}
			}
		}
	}

	private void doAdmin( String channel, String sender, char c, String message ) {
		// Method for processing admin commands.
		if (this.admin_list.contains(sender.toLowerCase()) || this.admin_list.contains(channel.toLowerCase())) {
			String command;
			String[] args = null;

			int space = message.indexOf(" ");
			if (space > 0) {
				command = message.substring(0, space).toLowerCase();
				args = message.substring(space + 1).split(" ", 0);
			} else {
				command = message.substring(0).toLowerCase();
			}

			if (command.equals("setadultflag")) {
				if (args != null && args.length == 2) {
					String target = args[0].toLowerCase();
					if (this.channel_data.containsKey(target)) {
						boolean flag = false;
						if (!"0".equals(args[1])) {
							flag = true;
						}

						this.setChannelAdultFlag(target, flag);
						this.sendMessage(channel, sender + ": Channel adult flag updated for " + target);
					} else {
						this.sendMessage(channel, "I don't know about " + target);
					}
				} else {
					this.sendMessage(channel,
						"Use: $setadultflag <#channel> <0/1>");
				}
			} else if (command.equals("setmuzzleflag")) {
				if (args != null && args.length == 2) {
					String target = args[0].toLowerCase();
					if (this.channel_data.containsKey(target)) {
						boolean flag = false;
						if (!"0".equals(args[1])) {
							flag = true;
						}

						this.setChannelMuzzledFlag(target, flag);
						this.sendMessage(channel, sender + ": Channel muzzle flag updated for " + target);
					} else {
						this.sendMessage(channel, "I don't know about " + target);
					}
				} else {
					this.sendMessage(channel, "Usage: $setmuzzleflag <#channel> <0/1>");
				}
			} else if (command.equals("shutdown")) {
				this.sendMessage(channel, "Shutting down...");
				this.shutdown = true;
				this.quitServer("I am shutting down! Bye!");
				System.exit(0);
			} else if (command.equals("reload") || command.equals("refreshdb")) {
				this.sendMessage(channel, "Reading database tables ...");
				this.refreshDbLists();
				this.sendDelayedMessage(channel, "Tables reloaded.", 1000);
			} else if (command.equals("reset")) {
				this.sendMessage(channel, "Restarting internal timer...");

				try {
					this.warticker.cancel();
					this.warticker = null;

					this.ticker.cancel();
					this.ticker = null;
				} catch (Exception e) {
				}

				this.warticker = new WarClockThread(this);
				this.ticker = new Timer(true);
				this.ticker.scheduleAtFixedRate(this.warticker, 0, 1000);
				this.refreshDbLists();

				this.sendDelayedMessage(channel, "Can you hear me now?", 2000);
			} else if (command.equals("ignore")) {
				if (args != null && args.length > 0) {
					String users = "";
					for (int i = 0; i < args.length; ++i) {
						users += " " + args[i];
						this.ignore_list.add(args[i]);
						this.setIgnore(args[i]);
					}
					this.sendMessage(channel,
						"The following users have been ignored:" + users);
				} else {
					this.sendMessage(channel,
						"Usage: $ignore <user 1> [ <user 2> [<user 3> [...] ] ]");
				}
			} else if (command.equals("unignore")) {
				if (args != null && args.length > 0) {
					String users = "";
					for (int i = 0; i < args.length; ++i) {
						users += " " + args[i];
						this.ignore_list.remove(args[i]);
						this.removeIgnore(args[i]);
					}
					this.sendMessage(channel,
						"The following users have been unignored:" + users);
				} else {
					this.sendMessage(channel,
						"Usage: $unignore <user 1> [ <user 2> [<user 3> [...] ] ]");
				}
			} else if (command.equals("listignores")) {
				this.sendMessage(channel,
					"There are " + this.ignore_list.size()
					+ " users ignored.");
				Iterator<String> iter = this.ignore_list.iterator();
				while (iter.hasNext()) {
					this.sendMessage(channel, iter.next());
				}
			} else if (command.equals("help")) {
				this.printAdminCommandList(sender, channel);
			} else if (this.amusement.parseAdminCommand(channel, sender, message)) {
			} else if (this.story.parseAdminCommand(channel, sender, message)) {
			} else if (this.challenge.parseAdminCommand(channel, sender, message)) {
			} else {
				this.sendMessage(channel, "$" + command + " is not a valid admin command - try $help");
			}
		} else {
			// The sender is NOT an admin
			this.sendMessage(
				this.debugChannel,
				String.format(
				"User %s in channel %s attempted to use an admin command (%s)!",
				sender, channel, message));
			this.sendMessage(
				channel,
				String.format(
				"%s: You are not an admin. Only Admins have access to that command.",
				sender));
		}
	}

	private void interact( String sender, String channel, String message, String type ) {
		ChannelInfo cdata = this.channel_data.get(channel.toLowerCase());

		long elapsed = System.currentTimeMillis() / 1000 - cdata.chatterTimer;
		long odds = (long) Math.log(elapsed) * cdata.chatterTimeMultiplier;
		if (odds > cdata.chatterMaxBaseOdds) {
			odds = cdata.chatterMaxBaseOdds;
		}

		if (message.toLowerCase().contains(this.getNick().toLowerCase())) {
			odds = odds * cdata.chatterNameMultiplier;
		}

		if (this.rand.nextInt(100) < odds) {
			String[] actions;

			if (cdata.doMarkhov && !cdata.doRandomActions) {
				actions = new String[] {
					"markhov"
				};
			} else if (cdata.doMarkhov && cdata.doRandomActions) {
				actions = new String[] {
					"markhov",
					"challenge",
					"amusement"
				};
			} else if (cdata.doMarkhov && cdata.doRandomActions) {
				actions = new String[] {
					"challenge",
					"amusement"
				};
			} else {
				return;
			}

			String action = actions[rand.nextInt(actions.length)];
			
			if ("markhov".equals(action)) {
				markhov.randomAction(sender, channel, message, type);
			} else if ("challenge".equals(action)) {
				challenge.randomAction(sender, channel, message, type);
			} else if ("amusement".equals(action)) {
				amusement.randomAction(sender, channel, message, type);
			}

			cdata.chatterTimer += this.rand.nextInt((int) elapsed / cdata.chatterTimeDivisor);
			channelLog("Chattered On: " + channel);
		}
	}

	@Override
	protected void onPrivateMessage( String sender, String login,
									 String hostname, String message ) {
		if (this.admin_list.contains(sender)) {
			String[] args = message.split(" ");
			if (args != null && args.length > 2) {
				String msg = "";
				for (int i = 2; i < args.length; i++) {
					msg += args[i] + " ";
				}
				if (args[0].equalsIgnoreCase("say")) {
					this.sendMessage(args[1], msg);
				} else if (args[0].equalsIgnoreCase("act")) {
					this.sendAction(args[1], msg);
				}
			}
		}
	}

	@Override
	protected void onInvite( String targetNick, String sourceNick,
							 String sourceLogin, String sourceHostname, String channel ) {
		if (!this.ignore_list.contains(sourceNick)
			&& targetNick.equals(this.getNick())) {
			if (!this.channel_data.containsKey(channel.toLowerCase())) {
				this.joinChannel(channel);
				this.saveChannel(channel.toLowerCase());
			}
		}
	}

	@Override
	protected void onKick( String channel, String kickerNick, String kickerLogin,
						   String kickerHostname, String recipientNick, String reason ) {
		if (!kickerNick.equals(this.getNick())) {
			this.deleteChannel(channel.toLowerCase());
		}
	}

	@Override
	protected void onPart( String channel, String sender, String login,
						   String hostname ) {
		if (!sender.equals(this.getNick())) {
			User[] userlist = this.getUsers(channel);
			if (userlist.length <= 1) {
				this.partChannel(channel);
				this.deleteChannel(channel.toLowerCase());
			}
		}
	}

	@Override
	public void onNotice( String sender, String nick, String hostname,
						  String target, String notice ) {
		if (sender.equals("NickServ") && notice.contains("This nick")) {
			this.sendMessage("NickServ", "identify " + this.password);
		}
	}

	@Override
	protected void onDisconnect() {
		if (!this.shutdown) {
			this.connectToServer();
		}
	}

	@Override
	public void onJoin( String channel, String sender, String login,
						String hostname ) {
		if (!sender.equals(this.getName()) && !login.equals(this.getLogin())) {
			String message = "Hello, " + sender + "!";
			if (this.wars.size() > 0) {
				int warscount = 0;
				String winfo = "";
				for (Map.Entry<String, WordWar> wm : this.wars.entrySet()) {
					if (wm.getValue().getChannel().equalsIgnoreCase(channel)) {
						winfo += wm.getValue().getDescription();
						if (warscount > 0) {
							winfo += " || ";
						}
						warscount++;
					}
				}

				if (warscount > 0) {
					boolean plural = warscount >= 2 || warscount == 0;
					message += " There " + ( plural ? "are" : "is" ) + " "
							   + warscount + " war" + ( plural ? "s" : "" )
							   + " currently " + "running in this channel"
							   + ( warscount > 0 ? ": " + winfo : "." );
				}
			}
			this.sendDelayedMessage(channel, message, 1600);

			if (Pattern.matches("(?i)mib_......", sender)
				|| Pattern.matches("(?i)guest.*", sender)) {
				this.sendDelayedMessage(
					channel,
					String.format(
					"%s: To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere",
					sender), 2400);
			}

			int r = this.rand.nextInt(100);

			if (r < 4) {
				r = this.rand.nextInt(this.greetings.size());
				this.sendDelayedMessage(channel, this.greetings.get(r), 2400);
			}
		}
	}

	public void doCommand( String channel, String sender, String prefix,
						   String message ) {
		String command;
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			args = message.substring(space + 1).split(" ", 0);
		} else {
			command = message.substring(1).toLowerCase();
		}

		if (prefix.equals("!")) {
			if (command.equals("startwar")) {
				if (args != null && args.length > 1) {
					this.startWar(channel, sender, args);
				} else {
					this.sendMessage(channel,
						"Use: !startwar <duration in min> [<time to start in min> [<name>]]");
				}
			} else if (command.equals("boxodoom")) {
				this.boxodoom(channel, sender, args);
			} else if (command.equals("startjudgedwar")) {
				this.sendMessage(channel, "Not done yet, sorry!");
			} else if (command.equals("endwar")) {
				this.endWar(channel, sender, args);
			} else if (command.equals("listwars")) {
				this.listWars(channel, sender, args, false);
			} else if (command.equals("listall")) {
				this.listAllWars(channel, sender, args);
			} else if (command.equals("eggtimer")) {
				double time = 15;
				if (args != null) {
					try {
						time = Double.parseDouble(args[0]);
					} catch (Exception e) {
						this.sendMessage(channel,
							"Could not understand first parameter. Was it numeric?");
						return;
					}
				}
				this.sendMessage(channel, sender + ": your timer has been set.");
				this.sendDelayedNotice(sender, "Your timer has expired!",
					(int) ( time * 60 * 1000 ));
			} else if (command.equals("settopic")) {
				if (args != null && args.length > 0) {
					String topic = args[0];
					for (int i = 1; i < args.length; i++) {
						topic += " " + args[i];
					}
					this.setTopic(channel, topic + " --" + sender);
				}
			} // add additional commands above here!!
			else if (command.equals("help")) {
				this.printCommandList(sender, channel);
			} else if (command.equals("credits")) {
				this.sendMessage(
					channel,
					"I was created by MysteriousAges in 2008 using PHP, and ported to the Java PircBot library in 2009. Utoxin started helping during NaNoWriMo 2010. Sourcecode is available here: https://github.com/MysteriousAges/TimTheWordWarBot, and my NaNoWriMo profile page is here: http://www.nanowrimo.org/en/participants/timmybot");
			} else if (command.equals("anything") || command.equals("jack")
					   || command.equals("squat") || command.equals("much")) {
				this.sendMessage(channel, "Nice try, " + sender
										  + ", trying to get me to look stupid.");

				int r = this.rand.nextInt(100);
				if (r < 10) {
					this.amusement.defenestrate(channel, sender, sender.split(" ", 0),
						false);
				} else if (r < 20) {
					this.amusement.throwFridge(channel, sender, sender.split(" ", 0),
						false);
				}
			} else if (this.story.parseUserCommand(channel, sender, prefix, message)) {
			} else if (this.challenge.parseUserCommand(channel, sender, prefix, message)) {
			} else if (this.amusement.parseUserCommand(channel, sender, prefix, message)) {
			} else {
				this.sendMessage(channel, "!" + command + " was not part of my training.");
			}
		} else if (prefix.equals("@")) {
			long wordcount;
			try {
				wordcount = (long) Double.parseDouble(command);
				for (Map.Entry<String, WordWar> wm : this.wars.entrySet()) {
					this.sendMessage(channel, wm.getKey());
				}
			} catch (Exception e) {
			}

		}
	}

	private void printCommandList( String target, String channel ) {
		int msgdelay = 5;
		int delayCnt = 0;

		this.sendAction(channel, "whispers in " + target + "'s ear. (Check for a new windor or tab with the help text.)");

		String[] strs = {"I am a robot trained by the WordWar Monks of Honolulu. You have "
						 + "never heard of them. It is because they are awesome.",
						 "Core Commands:",
						 "    !startwar <duration> <time to start> <an optional name> - Starts a word war",
						 "    !listwars - I will tell you about the wars currently in progress.",
						 "    !boxodoom <difficulty> <duration> - Difficulty is easy/average/hard, duration in minutes.",
						 "    !eggtimer <time> - I will send you a message after <time> minutes.",
						 "    !settopic <topic> - If able, I will try to set the channel's topic.",
						 "    !credits - Details of my creators, and where to find my source code.",
		};
		for (int i = 0; i < strs.length; ++i, ++delayCnt) {
			this.sendDelayedMessage(target, strs[i], msgdelay * delayCnt);
		}

		delayCnt = this.story.helpSection(target, channel, delayCnt, msgdelay);
		delayCnt = this.challenge.helpSection(target, channel, delayCnt, msgdelay);
		delayCnt = this.amusement.helpSection(target, channel, delayCnt, msgdelay);
		
		String[] post = {"I... I think there might be other tricks I know... You'll have to find them!",
						 "I will also respond to the /invite command if you would like to see me in another channel. "
		};
		for (int i = 0; i < post.length; ++i, ++delayCnt) {
			this.sendDelayedMessage(target, post[i], msgdelay * delayCnt);
		}
	}

	private void printAdminCommandList( String target, String channel ) {
		int msgdelay = 5;
		int delayCnt = 0;

		this.sendAction(channel, "whispers in " + target + "'s ear. (Check for a new windor or tab with the help text.)");

		String[] helplines = {"Core Admin Commands:",
							  "    $setadultflag <#channel> <0/1> - clears/sets adult flag on channel",
							  "    $setmuzzleflag <#channel> <0/1> - clears/sets muzzle flag on channel",
							  "    $shutdown - Forces bot to exit",
							  "    $reload - Reloads data from MySQL (also $refreshdb)",
							  "    $reset - Resets internal timer for wars, and reloads data from MySQL",
							  "    $listitems [ <page #> ] - lists all currently approved !get/!getfor items",
							  "    $listpending [ <page #> ] - lists all unapproved !get/!getfor items",
							  "    $approveitem <item # from $listpending> - removes item from pending list and marks as approved for !get/!getfor",
							  "    $disapproveitem <item # from $listitems> - removes item from approved list and marks as pending for !get/!getfor",
							  "    $deleteitem <item # from $listpending> - permanently removes an item from the pending list for !get/!getfor",
							  "    $ignore <username> - Places user on the bot's ignore list",
							  "    $unignore <username> - Removes user from bot's ignore list",
							  "    $listignores - Prints the list of ignored users"
		};

		for (int i = 0; i < helplines.length; ++i, ++delayCnt) {
			this.sendDelayedMessage(target, helplines[i], msgdelay * delayCnt);
		}

		delayCnt = this.challenge.adminHelpSection(target, channel, delayCnt, msgdelay);
	}

	// !endwar <name>
	private void endWar( String channel, String sender, String[] args ) {
		if (args != null && args.length > 0) {
			String name = this.implodeArray(args);
			if (this.wars.containsKey(name.toLowerCase())) {
				if (sender.equalsIgnoreCase(this.wars.get(name.toLowerCase()).getStarter())
					|| this.admin_list.contains(sender)
					|| this.admin_list.contains(channel.toLowerCase())) {
					WordWar war = this.wars.remove(name.toLowerCase());
					this.sendMessage(channel, "The war '" + war.getName()
											  + "' has been ended.");
					this.sendMessage(this.debugChannel, "War '" + war.getName() + "' killed by " + sender + " in channel " + war.getChannel());
				} else {
					this.sendMessage(channel, sender
											  + ": Only the starter of a war can end it early.");
				}
			} else {
				this.sendMessage(channel, sender
										  + ": I don't know of a war with name: '" + name + "'");
			}
		} else {
			this.sendMessage(channel, sender + ": I need a war name to end.");
		}
	}

	private void startWar( String channel, String sender, String[] args ) {
		long time;
		long to_start = 5000;
		String warname;
		try {
			time = (long) ( Double.parseDouble(args[0]) * 60 );
		} catch (Exception e) {
			this.sendMessage(
				channel,
				sender
				+ ": could not understand the duration parameter. Was it numeric?");
			return;
		}
		if (args.length >= 2) {
			try {
				to_start = (long) ( Double.parseDouble(args[1]) * 60 );
			} catch (Exception e) {
				if (args[1].equalsIgnoreCase("now")) {
					to_start = 0;
				} else {
					this.sendMessage(
						channel,
						sender
						+ ": could not understand the time to start parameter. Was it numeric?");
					return;
				}
			}

		}
		if (args.length >= 3) {
			warname = args[2];
			for (int i = 3; i < args.length; i++) {
				warname = warname + " " + args[i];
			}
		} else {
			warname = sender + "'s war";
		}

		if (time < 60) {
			this.sendMessage(channel, sender
									  + ": Duration must be at least 1 minute.");
			return;
		}

		if (!this.wars.containsKey(warname.toLowerCase())) {
			WordWar war = new WordWar(time, to_start, warname, sender, channel);
			this.wars.put(war.getName().toLowerCase(), war);
			this.sendMessage(this.debugChannel, "War scheduled by " + sender + " in channel " + channel);
			if (to_start > 0) {
				this.sendMessage(channel, sender
										  + ": your wordwar will start in " + to_start / 60.0
										  + " minutes.");
			} else {
				this.beginWar(war);
			}
		} else {
			this.sendMessage(channel, sender
									  + ": there is already a war with the name '" + warname
									  + "'");
		}
	}

	private void listAllWars( String channel, String sender, String[] args ) {
		this.listWars(channel, sender, args, true);
	}

	private void listWars( String channel, String sender, String[] args,
						   boolean all ) {
		String target = args != null ? sender : channel;
		if (this.wars != null && this.wars.size() > 0) {
			for (Map.Entry<String, WordWar> wm : this.wars.entrySet()) {
				if (all || wm.getValue().getChannel().equalsIgnoreCase(channel)) {
					this.sendMessage(target, all ? wm.getValue().getDescriptionWithChannel() : wm.getValue().getDescription());
				}
			}
		} else {
			this.sendMessage(target, "No wars are currently available.");
		}
	}

	private void _tick() {
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
				}
				this.wars_lock.release();
			} catch (Throwable e) {
				this.wars_lock.release();
			}
		}
	}

	private void warStartCount( WordWar war ) {
		if (war.time_to_start < 60) {
			this.sendMessage(war.getChannel(), war.getName() + ": Starting in "
											   + war.time_to_start
											   + ( war.time_to_start == 1 ? " second" : " seconds" ) + "!");
		} else {
			int time_to_start = (int) war.time_to_start / 60;
			this.sendMessage(war.getChannel(), war.getName() + ": Starting in "
											   + time_to_start
											   + ( time_to_start == 1 ? " minute" : " minutes" ) + "!");
		}
	}

	private void warEndCount( WordWar war ) {
		if (war.remaining < 60) {
			this.sendMessage(war.getChannel(), war.getName() + ": "
											   + war.remaining
											   + ( war.remaining == 1 ? " second" : " seconds" )
											   + " remaining!");
		} else {
			int remaining = (int) war.remaining / 60;
			this.sendMessage(war.getChannel(), war.getName() + ": " + remaining
											   + ( remaining == 1 ? " minute" : " minutes" ) + " remaining.");
		}
	}

	private void beginWar( WordWar war ) {
		this.sendNotice(war.getChannel(), "WordWar: '" + war.getName()
										  + " 'starts now! (" + war.getDuration() / 60 + " minutes)");
		this.sendMessage(this.debugChannel, "War " + war.getName() + " started in channel " + war.getChannel());
	}

	private void endWar( WordWar war ) {
		this.sendNotice(war.getChannel(), "WordWar: '" + war.getName()
										  + "' is over!");
		this.wars.remove(war.getName().toLowerCase());
		this.sendMessage(this.debugChannel, "War '" + war.getName() + "' finished in channel " + war.getChannel());
	}

	private void boxodoom( String channel, String sender, String[] args ) {
		Connection con;
		long duration;
		long base_wpm;
		double modifier;
		int goal;

		if (args.length != 2) {
			this.sendMessage(channel, sender
									  + ": !boxodoom requires two parameters.");
			return;
		}

		if (!Pattern.matches("(?i)easy|average|hard", args[0])) {
			this.sendMessage(channel, sender
									  + ": Difficulty must be one of: easy, average, hard");
			return;
		}

		duration = (long) Double.parseDouble(args[1]);

		if (duration < 1) {
			this.sendMessage(channel, sender
									  + ": Duration must be greater than or equal to 1.");
			return;
		}

		String value = "";
		try {
			con = pool.getConnection(timeout);
			PreparedStatement s = con.prepareStatement("SELECT `challenge` FROM `box_of_doom` WHERE `difficulty` = ? ORDER BY rand() LIMIT 1");
			s.setString(1, args[0]);
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				value = rs.getString("challenge");
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		base_wpm = (long) Double.parseDouble(value);
		modifier = 1.0 / Math.log(duration + 1.0) / 1.5 + 0.68;
		goal = (int) ( duration * base_wpm * modifier / 10 ) * 10;

		this.sendMessage(channel,
			sender + ": Your goal is " + String.valueOf(goal));
	}

	private void useBackupNick() {
		this.setName(getSetting("backup_nickname"));
	}

	private void connectToServer() {
		try {
			this.connect(getSetting("server"));
		} catch (Exception e) {
			this.useBackupNick();
			try {
				this.connect(getSetting("server"));
			} catch (Exception ex) {
				System.err.print("Could not connect - name & backup in use");
				System.exit(1);
			}
		}

		// Join our channels
		for (Enumeration<ChannelInfo> e = this.channel_data.elements(); e.hasMoreElements();) {
			this.joinChannel(e.nextElement().Name);
		}

		this.joinChannel(this.debugChannel);
	}

	public static void main( String[] args ) {
		Tim bot = new Tim();

		bot.setLogin("ThereAreSomeWhoCallMeTim_Bot");
		bot.setVerbose(true);
		bot.setMessageDelay(350);
		bot.connectToServer();
	}

	protected String implodeArray( String[] inputArray ) {
		String AsImplodedString;
		if (inputArray.length == 0) {
			AsImplodedString = "";
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(inputArray[0]);
			for (int i = 1; i < inputArray.length; i++) {
				sb.append(" ");
				sb.append(inputArray[i]);
			}
			AsImplodedString = sb.toString();
		}

		return AsImplodedString;
	}

	public String getSetting( String key ) {
		Connection con;
		String value = "";

		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `value` FROM `settings` WHERE `key` = ?");
			s.setString(1, key);
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				value = rs.getString("value");
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		return value;
	}

	private void refreshDbLists() {
		this.getAdminList();
		this.getChannelList();
		this.getIgnoreList();
		this.getGreetingList();

		this.amusement.refreshDbLists();
		this.story.refreshDbLists();
		this.challenge.refreshDbLists();
	}

	private void getAdminList() {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `name` FROM `admins`");

			ResultSet rs = s.getResultSet();

			this.admin_list.clear();
			while (rs.next()) {
				this.admin_list.add(rs.getString("name").toLowerCase());
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getIgnoreList() {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `name` FROM `ignores`");

			ResultSet rs = s.getResultSet();
			this.ignore_list.clear();
			while (rs.next()) {
				this.ignore_list.add(rs.getString("name").toLowerCase());
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void setIgnore( String username ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `ignores` (`name`) VALUES (?);");
			s.setString(1, username.toLowerCase());
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void removeIgnore( String username ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `ignores` WHERE `name` = ?;");
			s.setString(1, username.toLowerCase());
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getGreetingList() {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `greetings`");

			ResultSet rs = s.getResultSet();
			this.greetings.clear();
			while (rs.next()) {
				this.greetings.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getChannelList() {
		Connection con;
		ChannelInfo ci;
		String channel;

		this.channel_data.clear();

		ci = new ChannelInfo(this.debugChannel, true, true, true, true);
		ci.setChatterTimers(
			Integer.parseInt(getSetting("chatterMaxBaseOdds")),
			Integer.parseInt(getSetting("chatterNameMultiplier")),
			Integer.parseInt(getSetting("chatterTimeMultiplier")),
			Integer.parseInt(getSetting("chatterTimeDivisor")));

		this.channel_data.put(this.debugChannel, ci);
		
		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM `channels`");

			while (rs.next()) {
				channel = rs.getString("channel").toLowerCase();
				ci = new ChannelInfo(channel, rs.getBoolean("adult"), rs.getBoolean("markhov"), rs.getBoolean("random"), rs.getBoolean("command"));
				ci.setChatterTimers(
					Integer.parseInt(getSetting("chatterMaxBaseOdds")),
					Integer.parseInt(getSetting("chatterNameMultiplier")),
					Integer.parseInt(getSetting("chatterTimeMultiplier")),
					Integer.parseInt(getSetting("chatterTimeDivisor")));

				this.channel_data.put(channel, ci);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void saveChannel( String channel ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `channels` (`channel`, `adult`, `muzzled`) VALUES (?, 0, 0)");
			s.setString(1, channel.toLowerCase());
			s.executeUpdate();

			if (!this.channel_data.containsKey(channel.toLowerCase())) {
				this.channel_data.put(channel.toLowerCase(), new ChannelInfo(channel.toLowerCase()));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void deleteChannel( String channel ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `channels` WHERE `channel` = ?");
			s.setString(1, channel.toLowerCase());
			s.executeUpdate();

			// Will do nothing if the channel is not in the list.
			this.channel_data.remove(channel.toLowerCase());

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void setChannelAdultFlag( String channel, boolean adult ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET adult = ? WHERE `channel` = ?");
			s.setBoolean(1, adult);
			s.setString(2, channel.toLowerCase());
			s.executeUpdate();

			if (adult) {
				this.channel_data.get(channel.toLowerCase()).isAdult = true;
			} else {
				this.channel_data.get(channel.toLowerCase()).isAdult = false;
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void setChannelMuzzledFlag( String channel, boolean muzzled ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET `markhov` = ?, `random` = ?, `command` = ? WHERE `channel` = ?");
			s.setBoolean(1, muzzled);
			s.setBoolean(2, muzzled);
			s.setBoolean(3, muzzled);
			s.setString(2, channel.toLowerCase());
			s.executeUpdate();

			this.channel_data.get(channel.toLowerCase()).doMarkhov = muzzled;
			this.channel_data.get(channel.toLowerCase()).doCommandActions = muzzled;
			this.channel_data.get(channel.toLowerCase()).doRandomActions = muzzled;

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected boolean isChannelAdult( String channel ) {
		boolean val = false;
		ChannelInfo cdata = this.channel_data.get(channel.toLowerCase());
		if (cdata != null) {
			val = cdata.isAdult;
		}
		return val;
	}
	
	protected void channelLog( String message ) {
		this.sendMessage(this.debugChannel, message);
	}
}
