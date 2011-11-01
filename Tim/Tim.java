/**
 *  This file is part of Timmy, the Wordwar Bot.
 *
 *  Timmy is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Timmy is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Timmy.  If not, see <http://www.gnu.org/licenses/>.
 */
package Tim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jibble.pircbot.*;

public class Tim extends PircBot {
	public static AppConfig config = AppConfig.getInstance();

	public class WarClockThread extends TimerTask {
		private Tim parent;

		public WarClockThread(Tim pparent) {
			this.parent = pparent;
		}

		public void run() {
			try {
				this.parent._tick();
			}
			catch (Throwable t) {
				System.out.println("&&& THROWABLE CAUGHT in DelayCommand.run:");
				t.printStackTrace(System.out);
				System.out.flush();
			}
		}
	}

	protected enum ActionType
	{
		MESSAGE, ACTION, NOTICE
	};

	public class DelayCommand extends TimerTask {
		private ActionType type;
		private Tim parent;
		private String text;
		private String target;

		public DelayCommand(Tim pparent, String target, String text,
				ActionType type) {
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
			}
			catch (Throwable t) {
				System.out.println("&&& THROWABLE CAUGHT in DelayCommand.run:");
				t.printStackTrace(System.out);
				System.out.flush();
				this.parent.sendMessage(this.target,
						"I couldn't schedule your command for some reason:");
				this.parent.sendMessage(this.target, t.toString());
			}

		}
	}

	private Set<String> admin_list = new HashSet<String>(16);
	private Set<String> ignore_list = new HashSet<String>(16);
	private Set<String> adult_channels = new HashSet<String>(128);
	private List<String> colours = new ArrayList<String>();
	private List<String> eightballs = new ArrayList<String>();
	private List<String> greetings = new ArrayList<String>();
	private List<String> commandments = new ArrayList<String>();
	private List<String> aypwips = new ArrayList<String>();
	private List<String> flavours = new ArrayList<String>();
	private List<String> deities = new ArrayList<String>();
	private List<String> approved_items = new ArrayList<String>();
	private List<String> pending_items = new ArrayList<String>();
	private Map<String, WordWar> wars;
	private Map<String, Object> settings;
	private WarClockThread warticker;
	private Timer ticker;
	private Semaphore wars_lock;
	private Random rand;
	private boolean shutdown;
	private String password;
	private String debugChannel;
	private long chatterTimer;
	private Connection mysql;
	private int chatterMaxBaseOdds;
	private int chatterNameMultiplier;
	private int chatterTimeMultiplier;
	private int chatterTimeDivisor;

	public Tim() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String url = "jdbc:mysql://" + Tim.config.getString("sql_server")
					+ ":3306/" + Tim.config.getString("sql_database");
			try {
				this.mysql = DriverManager.getConnection(url,
						Tim.config.getString("sql_user"),
						Tim.config.getString("sql_password"));
			}
			catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null,
						ex);
			}
		}
		catch (ClassNotFoundException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		this.setName(this.getSetting("nickname"));
		this.password = this.getSetting("password");
		this.debugChannel = this.getSetting("debug_channel");
		if (this.debugChannel.equals("")) {
			// Ideally, we should fail here...
			this.debugChannel = "#timmydebug";
		}

		this.refreshDbLists();

		this.wars = Collections.synchronizedMap(new HashMap<String, WordWar>());

		this.warticker = new WarClockThread(this);
		this.ticker = new Timer(true);
		this.ticker.scheduleAtFixedRate(this.warticker, 0, 1000);
		this.wars_lock = new Semaphore(1, true);
		this.chatterTimer = System.currentTimeMillis() / 1000;

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
	 * @param target
	 *            The user or channel to send the message to
	 * @param action
	 *            The string for the text of the message
	 * @param delay
	 *            The delay in milliseconds
	 */
	public void sendDelayedMessage(String target, String message, int delay) {
		DelayCommand talk = new DelayCommand(this, target, message,
				ActionType.MESSAGE);
		this.ticker.schedule(talk, delay);
	}

	/**
	 * Sends an action with a delay
	 * 
	 * @param target
	 *            The user or channel to send the action to
	 * @param action
	 *            The string for the text of the action
	 * @param delay
	 *            The delay in milliseconds
	 */
	public void sendDelayedAction(String target, String action, int delay) {
		DelayCommand act = new DelayCommand(this, target, action,
				ActionType.ACTION);
		this.ticker.schedule(act, delay);
	}

	/**
	 * Sends a notice with a delay
	 * 
	 * @param target
	 *            The user or channel to send the notice to
	 * @param action
	 *            The string for the text of the notice
	 * @param delay
	 *            The delay in milliseconds
	 */
	public void sendDelayedNotice(String target, String action, int delay) {
		DelayCommand act = new DelayCommand(this, target, action,
				ActionType.NOTICE);
		this.ticker.schedule(act, delay);
	}

	@Override
	protected void onAction(String sender, String login, String hostname,
			String target, String action) {
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
			this.interact(sender, target, action);
		}
	}

	@Override
	public void onMessage(String channel, String sender, String login,
			String hostname, String message) {
		if (this.ignore_list.contains(sender)) {
			return;
		}
		else {
			// Find all messages that start with ! and pass them to a method for
			// further processing.
			if (message.charAt(0) == '!') {
				this.doCommand(channel, sender, "!", message);
				return;
			} // Notation for wordcounts
			else if (message.charAt(0) == '@') {
				this.doCommand(channel, sender, "@", message);
				return;
			}
			else if (message.charAt(0) == '$') {
				this.doAdmin(channel, sender, '$', message.substring(1));
				return;
			}

			// Other fun stuff we can make him do
			if (message.toLowerCase().contains("hello")
					&& message.toLowerCase().contains(
							this.getNick().toLowerCase())) {
				this.sendMessage(channel, "Hi, " + sender + "!");
				return;
			}
			else if (message.toLowerCase().contains("how many lights")) {
				this.sendMessage(channel, "There are FOUR LIGHTS!");
				return;
			}
			else if (message.contains(":(")) {
				this.sendAction(channel, "gives " + sender + " a hug");
				return;
			}
			else if (message.contains(":'(")) {
				this.sendAction(channel, "passes " + sender + " a tissue");
				return;
			}
			else if (message.toLowerCase().contains(
					"are you thinking what i'm thinking")
					|| message.toLowerCase().contains(
							"are you pondering what i'm pondering")) {
				int i = this.rand.nextInt(this.aypwips.size());
				this.sendMessage(channel,
						String.format(this.aypwips.get(i), sender));
				return;
			}
			else {
				if (Pattern
						.matches(
								"(?i).*how do i (change|set) my (nick|name).*",
								message)) {
					this.sendMessage(
							channel,
							String.format(
									"%s: To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere",
									sender));
					return;
				}
				else if (Pattern.matches("(?i)" + this.getNick() + ".*[?]",
						message)) {
					int r = this.rand.nextInt(100);

					if (r < 50) {
						this.eightball(channel, sender, null);
						return;
					}
				}
			}

			if (!sender.equals(this.getNick()) && !"".equals(channel)) {
				this.interact(sender, channel, message);
			}
		}
	}

	private void doAdmin(String channel, String sender, char c, String message) {
		// Method for processing admin commands.
		if (this.admin_list.contains(sender)) {
			String command;
			String[] args = null;

			int space = message.indexOf(" ");
			if (space > 0) {
				command = message.substring(0, space).toLowerCase();
				args = message.substring(space + 1).split(" ", 0);
			}
			else {
				command = message.substring(0).toLowerCase();
			}

			if (command.equals("setadultflag")) {
				if (args != null && args.length == 2) {
					this.setChannelAdultFlagParse(channel, sender, args);
				}
				else {
					this.sendMessage(channel,
							"Use: $setadultflag <#channel> <0/1>");
				}
			}
			else if (command.equals("shutdown")) {
				this.sendMessage(channel, "Shutting down...");
				this.shutdown = true;
				this.quitServer("I am shutting down! Bye!");
				System.exit(0);
			}
			else if (command.equals("reload") || command.equals("refreshdb")) {
				this.sendMessage(channel, "Reading database tables ...");
				this.refreshDbLists();
				this.sendDelayedMessage(channel, "Tables reloaded.", 1000);
			}
			else if (command.equals("reset")) {
				this.sendMessage(channel, "Restarting internal timer...");

				try {
					this.warticker.cancel();
					this.warticker = null;

					this.ticker.cancel();
					this.ticker = null;
				}
				catch (Exception e) {
				}

				this.warticker = new WarClockThread(this);
				this.ticker = new Timer(true);
				this.ticker.scheduleAtFixedRate(this.warticker, 0, 1000);
				this.refreshDbLists();

				this.sendDelayedMessage(channel, "Can you hear me now?", 2000);
			}
			else if (command.equals("listitems")) {
				int pages = (this.approved_items.size() + 9) / 10;
				int wantPage = 0;
				if (args != null && args.length > 0) {
					try {
						wantPage = Integer.parseInt(args[0]) - 1;
					}
					catch (NumberFormatException ex) {
						this.sendMessage(channel,
								"Page number was not numeric.");
						return;
					}
				}

				if (wantPage > pages) {
					wantPage = pages;
				}

				int list_idx = wantPage * 10;
				this.sendMessage(channel, String.format(
						"Showing page %d of %d (%d items total)", wantPage + 1,
						pages, this.approved_items.size()));
				for (int i = 0; i < 10 && list_idx < this.approved_items.size(); ++i, list_idx = wantPage
						* 10 + i) {
					this.sendMessage(channel, String.format("%d: %s", list_idx,
							this.approved_items.get(list_idx)));
				}
			}
			else if (command.equals("listpending")) {
				int pages = (this.pending_items.size() + 9) / 10;
				int wantPage = 0;
				if (args != null && args.length > 0) {
					try {
						wantPage = Integer.parseInt(args[0]) - 1;
					}
					catch (NumberFormatException ex) {
						this.sendMessage(channel,
								"Page number was not numeric.");
						return;
					}
				}

				if (wantPage > pages) {
					wantPage = pages;
				}

				int list_idx = wantPage * 10;
				this.sendMessage(channel, String.format(
						"Showing page %d of %d (%d items total)", wantPage + 1,
						pages, this.pending_items.size()));
				for (int i = 0; i < 10 && list_idx < this.pending_items.size(); ++i, list_idx = wantPage
						* 10 + i) {
					this.sendMessage(channel, String.format("%d: %s", list_idx,
							this.pending_items.get(list_idx)));
				}
			}
			else if (command.equals("approveitem")) {
				if (args != null && args.length > 0) {
					int idx = 0;
					String item = "";
					try {
						idx = Integer.parseInt(args[0]);
						item = this.pending_items.get(idx);
					}
					catch (NumberFormatException ex) {
						// Must be a string
						item = args[0];
						for (int i = 1; i < args.length; ++i) {
							item = item + " " + args[i];
						}
						idx = this.pending_items.indexOf(item);
					}
					if (idx >= 0) {
						this.setItemApproved(item, true);
						this.pending_items.remove(idx);
						this.approved_items.add(item);
					}
					else {
						this.sendMessage(channel, String.format(
								"Item %s is not pending approval.", args[0]));
					}
				}
			}
			else if (command.equals("disapproveitem")) {
				if (args != null && args.length > 0) {
					int idx = 0;
					String item = "";
					try {
						idx = Integer.parseInt(args[0]);
						item = this.approved_items.get(idx);
					}
					catch (NumberFormatException ex) {
						// Must be a string
						item = args[0];
						for (int i = 1; i < args.length; ++i) {
							item = item + " " + args[i];
						}
						idx = this.approved_items.indexOf(item);
					}
					if (idx >= 0) {
						this.setItemApproved(item, false);
						this.pending_items.add(item);
						this.approved_items.remove(idx);
					}
					else {
						this.sendMessage(channel, String.format(
								"Item %s is not pending approval.", args[0]));
					}
				}
			}
			else if (command.equals("deleteitem")) {
				if (args != null && args.length > 0) {
					int idx = 0;
					String item = "";
					try {
						idx = Integer.parseInt(args[0]);
						item = this.pending_items.get(idx);
					}
					catch (NumberFormatException ex) {
						// Must be a string
						item = args[0];
						for (int i = 1; i < args.length; ++i) {
							item = item + " " + args[i];
						}
						idx = this.pending_items.indexOf(item);
					}
					if (idx >= 0) {
						this.removeItem(item);
						this.pending_items.remove(item);
					}
					else {
						this.sendMessage(channel, String.format(
								"Item %s is not pending approval.", args[0]));
					}
				}
			}
			else if (command.equals("ignore")) {
				if (args != null && args.length > 0) {
					String users = "";
					for (int i = 0; i < args.length; ++i) {
						users += " " + args[i];
						this.ignore_list.add(args[i]);
						this.setIgnore(args[i]);
					}
					this.sendMessage(channel,
							"The following users have been ignored:" + users);
				}
				else {
					this.sendMessage(channel,
							"Usage: $ignore <user 1> [ <user 2> [<user 3> [...] ] ]");
				}
			}
			else if (command.equals("unignore")) {
				if (args != null && args.length > 0) {
					String users = "";
					for (int i = 0; i < args.length; ++i) {
						users += " " + args[i];
						this.ignore_list.remove(args[i]);
						this.removeIgnore(args[i]);
					}
					this.sendMessage(channel,
							"The following users have been unignored:" + users);
				}
				else {
					this.sendMessage(channel,
							"Usage: $unignore <user 1> [ <user 2> [<user 3> [...] ] ]");
				}
			}
			else if (command.equals("listignores")) {
				this.sendMessage(channel,
						"There are " + this.ignore_list.size()
								+ " users ignored.");
				Iterator<String> iter = this.ignore_list.iterator();
				while (iter.hasNext()) {
					this.sendMessage(channel, iter.next());
				}
			}
		}
		else {
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

	private void interact(String sender, String channel, String message) {
		long elapsed = System.currentTimeMillis() / 1000 - this.chatterTimer;
		long odds = (long) Math.log(elapsed) * this.chatterTimeMultiplier;
		if (odds > this.chatterMaxBaseOdds) {
			odds = this.chatterMaxBaseOdds;
		}

		if (message.toLowerCase().contains(this.getNick().toLowerCase())) {
			odds = odds * this.chatterNameMultiplier;
		}

		int i = this.rand.nextInt(200);
		if (i < odds) {
			int j = this.rand.nextInt(200);

			if (j > 90) {
				this.getItem(channel, sender, null);
			}
			else if (j > 40) {
				int r = this.rand.nextInt(this.eightballs.size());
				this.sendDelayedAction(channel, "mutters under his breath, \""
						+ this.eightballs.get(r) + "\"",
						this.rand.nextInt(1500));
			}
			else if (j > 20) {
				this.throwFridge(channel, sender, sender.split(" ", 0), false);
			}
			else if (j > 14) {
				this.defenestrate(channel, sender, sender.split(" "), false);
			}
			else if (j > 6) {
				this.sing(channel);
			}
			else {
				this.foof(channel, sender, sender.split(" "), false);
			}

			this.sendMessage(
					this.debugChannel,
					"Elapsed Time: " + Long.toString(elapsed) + "  Odds: "
							+ Long.toString(odds) + "  Chatter Timer: "
							+ Long.toString(this.chatterTimer));
			this.chatterTimer = this.chatterTimer
					+ this.rand
							.nextInt((int) elapsed / this.chatterTimeDivisor);
			this.sendMessage(this.debugChannel, "Updated Chatter Timer: "
					+ Long.toString(this.chatterTimer));
		}
	}

	@Override
	protected void onPrivateMessage(String sender, String login,
			String hostname, String message) {
		if (this.admin_list.contains(sender)) {
			String[] args = message.split(" ");
			if (args != null && args.length > 2) {
				String msg = "";
				for (int i = 2; i < args.length; i++) {
					msg += args[i] + " ";
				}
				if (args[0].equalsIgnoreCase("say")) {
					this.sendMessage(args[1], msg);
				}
				else if (args[0].equalsIgnoreCase("act")) {
					this.sendAction(args[1], msg);
				}
			}
		}
	}

	@Override
	protected void onInvite(String targetNick, String sourceNick,
			String sourceLogin, String sourceHostname, String channel) {
		if (!this.ignore_list.contains(sourceNick)
				&& targetNick.equals(this.getNick())) {
			String[] chanlist = this.getChannels();
			boolean isIn = false;
			for (int i = 0; i < chanlist.length; i++) {
				if (chanlist[i].equalsIgnoreCase(channel)) {
					isIn = true;
					break;
				}
			}
			if (!isIn) {
				this.joinChannel(channel);
				this.saveChannel(channel);
			}
		}
	}

	@Override
	protected void onPart(String channel, String sender, String login,
			String hostname) {
		if (!sender.equals(this.getNick())) {
			User[] userlist = this.getUsers(channel);
			if (userlist.length <= 1) {
				this.partChannel(channel);
				this.deleteChannel(channel);
			}
		}
	}

	@Override
	public void onNotice(String sender, String nick, String hostname,
			String target, String notice) {
		if (sender.equals("NickServ") && notice.contains("This nick")) {
			this.sendMessage("NickServ", "identify \"" + this.password + "\"");
		}
	}

	@Override
	protected void onDisconnect() {
		if (!this.shutdown) {
			this.connectToServer();
		}
	}

	@Override
	public void onJoin(String channel, String sender, String login,
			String hostname) {
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
					message += " There " + (plural ? "are" : "is") + " "
							+ warscount + " war" + (plural ? "s" : "")
							+ " currently " + "running in this channel"
							+ (warscount > 0 ? ": " + winfo : ".");
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

	public void doCommand(String channel, String sender, String prefix,
			String message) {
		String command;
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			args = message.substring(space + 1).split(" ", 0);
		}
		else {
			command = message.substring(1).toLowerCase();
		}
		if (prefix.equals("!")) {
			if (command.equals("startwar")) {
				if (args != null && args.length > 1) {
					this.startWar(channel, sender, args);
				}
				else {
					this.sendMessage(channel,
							"Use: !startwar <duration in min> [<time to start in min> [<name>]]");
				}
			}
			else if (command.equals("boxodoom")) {
				this.boxodoom(channel, sender, args);
			}
			else if (command.equals("startjudgedwar")) {
				this.sendMessage(channel, "Not done yet, sorry!");
			}
			else if (command.equals("endwar")) {
				this.endWar(channel, sender, args);
			}
			else if (command.equals("listwars")) {
				this.listWars(channel, sender, args, false);
			}
			else if (command.equals("listall")) {
				this.listAllWars(channel, sender, args);
			}
			else if (command.equals("eggtimer")) {
				double time = 15;
				if (args != null) {
					try {
						time = Double.parseDouble(args[0]);
					}
					catch (Exception e) {
						this.sendMessage(channel,
								"Could not understand first parameter. Was it numeric?");
						return;
					}
				}
				this.sendMessage(channel, sender + ": your timer has been set.");
				this.sendDelayedNotice(sender, "Your timer has expired!",
						(int) (time * 60 * 1000));
			}
			else if (command.equals("settopic")) {
				if (args != null && args.length > 0) {
					String topic = args[0];
					for (int i = 1; i < args.length; i++) {
						topic += " " + args[i];
					}
					this.setTopic(channel, topic + " --" + sender);
				}
			}
			else if (command.equals("sing")) {
				this.sing(channel);
			}
			else if (command.equals("eightball") || command.equals("8-ball")) {
				this.eightball(channel, sender, args);
			}
			else if (command.charAt(0) == 'd'
					&& Pattern.matches("d\\d+", command)) {
				this.dice(command.substring(1), channel, sender, args);
			}
			else if (command.equals("woot")) {
				this.sendAction(channel, "cheers! Hooray!");
			}
			else if (command.equals("get")) {
				this.getItem(channel, sender, args);
			}
			else if (command.equals("getfor")) {
				if (args != null && args.length > 0) {
					if (args.length > 1) {
						// Want a new args array less the first old element.
						String[] newargs = new String[args.length - 1];
						for (int i = 1; i < args.length; ++i) {
							newargs[i - 1] = args[i];
						}
						this.getItem(channel, args[0], newargs);
					}
					else {
						this.getItem(channel, args[0], null);
					}
				}
			}
			else if (command.equals("fridge")) {
				this.throwFridge(channel, sender, args, true);
			}
			else if (command.equals("dance")) {
				this.sendAction(channel, "dances a cozy jig");
			}
			else if (command.equals("lick")) {
				this.lick(channel, sender, args);
			}
			else if (command.equals("commandment")) {
				this.commandment(channel, sender, args);
			} // add additional commands above here!!
			else if (command.equals("help")) {
				int delay = 0;
				int msgdelay = 9;
				String str = "I am a robot trained by the WordWar Monks of Honolulu. You have "
						+ "never heard of them. It is because they are awesome. I am capable "
						+ "of running the following commands:";
				this.sendMessage(channel, str);
				str = "!startwar <duration> <time to start> <an optional name> - Starts a word war";
				this.sendDelayedMessage(channel, str, delay += msgdelay);
				str = "!listwars - I will tell you about the wars currently in progress.";
				this.sendMessage(channel, str);
				str = "!boxodoom <difficulty> <duration> - Difficulty is easy/average/hard, duration in minutes.";
				this.sendMessage(channel, str);
				str = "!eggtimer <time> - I will send you a message after <time> minutes.";
				this.sendDelayedMessage(channel, str, delay += msgdelay);
				str = "!get <anything> - I will fetch you whatever you like.";
				this.sendDelayedMessage(channel, str, delay += msgdelay);
				str = "!getfor <someone> <anything> - I will give someone whatever you like.";
				this.sendDelayedMessage(channel, str, delay += msgdelay);
				str = "!eightball <your question> - I can tell you (with some degree of inaccuracy) how likely something is.";
				this.sendDelayedMessage(channel, str, delay += msgdelay);
				str = "!settopic <topic> - If able, I will try to set the channel's topic.";
				this.sendDelayedMessage(channel, str, delay += msgdelay);
				str = "!credits - Details of my creators, and where to find my source code.";
				this.sendDelayedMessage(channel, str, delay += msgdelay);
				str = "I... I think there might be other tricks I know... You'll have to find them!";
				this.sendDelayedMessage(channel, str, delay += msgdelay);
				str = "I will also respond to the /invite command if you would like to see me in another channel.";
				this.sendDelayedMessage(channel, str, delay += msgdelay);
			}
			else if (command.equals("credits")) {
				this.sendMessage(
						channel,
						"I was created by MysteriousAges in 2008 using PHP, and ported to the Java PircBot library in 2009. Utoxin started helping during NaNoWriMo 2010. Sourcecode is available here: https://github.com/MysteriousAges/TimTheWordWarBot");
			}
			else if (command.equals("anything") || command.equals("jack")
					|| command.equals("squat") || command.equals("much")) {
				this.sendMessage(channel, "Nice try, " + sender
						+ ", trying to get me to look stupid.");

				int r = this.rand.nextInt(100);
				if (r < 10) {
					this.defenestrate(channel, sender, sender.split(" ", 0),
							false);
				}
				else if (r < 20) {
					this.throwFridge(channel, sender, sender.split(" ", 0),
							false);
				}
			}
			else if (command.equals("defenestrate")) {
				this.defenestrate(channel, sender, args, true);
			}
			else if (command.equals("summon")) {
				this.summon(channel, sender, args, true);
			}
			else if (command.equals("foof")) {
				this.foof(channel, sender, args, true);
			}
			else {
				this.sendMessage(channel, sender + ": I don't know !" + command
						+ ".");
			}
		}
		else if (prefix.equals("@")) {
			long wordcount;
			try {
				wordcount = (long) Double.parseDouble(command);
				for (Map.Entry<String, WordWar> wm : this.wars.entrySet()) {
					this.sendMessage(channel, wm.getKey());
				}
			}
			catch (Exception e) {
			}

		}
	}

	private void dice(String number, String channel, String sender,
			String[] args) {
		int max = 0;
		try {
			max = Integer.parseInt(number);
			int rand = this.rand.nextInt(max);
			this.sendMessage(channel, sender + ": Your result is " + rand);
		}
		catch (NumberFormatException ex) {
			this.sendMessage(channel, number
					+ " is not a number I could understand.");
		}
	}

	private void getItem(String channel, String target, String[] args) {
		String item = "";
		if (args != null) {
			item = args[0];
			for (int i = 1; i < args.length; ++i) {
				item = item + " " + args[i];
			}
			if (!(this.approved_items.contains(item) || this.pending_items
					.contains(item)) && item.length() < 300) {
				this.insertPendingItem(item);
				this.pending_items.add(item);
			}
		}

		if (item.isEmpty()) {
			// Find a random item.
			int i = this.rand.nextInt(this.approved_items.size());
			item = this.approved_items.get(i);
		}

		this.sendAction(channel, String.format("gets %s %s", target, item));
	}

	private void lick(String channel, String sender, String[] args) {
		if (this.adult_channels.contains(channel)) {
			if (args.length >= 1) {
				String argStr = this.implodeArray(args);

				if (args[0].equalsIgnoreCase("MysteriousAges")) {
					this.sendAction(channel, "licks " + argStr
							+ ". Tastes like... like...");
					this.sendDelayedMessage(channel, "Like the Apocalypse.",
							1000);
					this.sendDelayedAction(channel, "cowers in fear", 2400);
				}
				else if (args[0].equalsIgnoreCase(this.getNick())) {
					this.sendAction(channel, "licks " + args[0]
							+ ". Tastes like meta.");
				}
				else if (this.admin_list.contains(args[0])) {
					this.sendAction(channel, "licks " + argStr
							+ ". Tastes like perfection, pure and simple.");
				}
				else {
					this.sendAction(
							channel,
							"licks "
									+ argStr
									+ ". Tastes like "
									+ this.flavours.get(this.rand
											.nextInt(this.flavours.size())));
				}
			}
			else {
				this.sendAction(
						channel,
						"licks "
								+ sender
								+ "! Tastes like "
								+ this.flavours.get(this.rand
										.nextInt(this.flavours.size())));
			}
		}
		else {
			this.sendMessage(channel, "Sorry, I don't do that here.");
		}
	}

	private void eightball(String channel, String sender, String[] args) {
		int r = this.rand.nextInt(this.eightballs.size());
		this.sendMessage(channel, this.eightballs.get(r));
	}

	private void sing(String channel) {
		int r = this.rand.nextInt(100);
		String response = "";
		if (r > 90) {
			response = "sings a beautiful song";
		}
		else if (r > 60) {
			response = "chants a snappy ditty";
		}
		else if (r > 30) {
			response = "starts singing 'It's a Small World'";
		}
		else {
			response = "screeches, and all the windows shatter";
		}
		this.sendAction(channel, response);
	}

	private void commandment(String channel, String sender, String[] args) {
		int r = this.rand.nextInt(this.commandments.size());
		if (args != null && args.length == 1 && Double.parseDouble(args[0]) > 0
				&& Double.parseDouble(args[0]) <= this.commandments.size()) {
			r = (int) Double.parseDouble(args[0]) - 1;
		}
		this.sendMessage(channel, this.commandments.get(r));
	}

	private void throwFridge(String channel, String sender, String[] args,
			Boolean righto) {
		String target = sender;
		if (args != null && args.length > 0) {
			if (!args[0].equalsIgnoreCase(this.getNick())
					&& !args[0].equalsIgnoreCase("himself")
					&& !args[0].equalsIgnoreCase("herself")
					&& !this.admin_list.contains(args[0])
					&& !args[0].equalsIgnoreCase("myst")) {
				target = this.implodeArray(args) + " ";
			}
		}

		if (righto) {
			this.sendMessage(channel, "Righto...");
		}

		int time = 2 + this.rand.nextInt(15);
		time *= 1000;
		this.sendDelayedAction(channel,
				"looks back and forth, then slinks off...", time);
		time += this.rand.nextInt(10) * 500 + 1500;
		String colour = this.colours
				.get(this.rand.nextInt(this.colours.size()));
		switch (colour.charAt(0)) {
			case 'a':
			case 'e':
			case 'i':
			case 'o':
			case 'u':
				colour = "n " + colour;
				break;
			default:
				colour = " " + colour;
		}
		int i = this.rand.nextInt(100);
		String act = "";
		if (i > 20) {
			act = "hurls a" + colour + " coloured fridge at " + target;
		}
		else if (i > 3) {
			target = sender;
			act = "hurls a" + colour + " coloured fridge at " + target
					+ " and runs away giggling";
		}
		else {
			act = "trips and drops a" + colour + " fridge on himself";
		}
		this.sendDelayedAction(channel, act, time);
	}

	private void defenestrate(String channel, String sender, String[] args,
			Boolean righto) {
		String target = sender;
		if (args != null && args.length > 0) {
			if (!args[0].equalsIgnoreCase(this.getNick())
					&& !args[0].equalsIgnoreCase("himself")
					&& !args[0].equalsIgnoreCase("herself")
					&& !this.admin_list.contains(args[0])) {
				target = this.implodeArray(args);
			}
		}

		if (righto) {
			this.sendMessage(channel, "Righto...");
		}

		int time = 2 + this.rand.nextInt(15);
		time *= 1000;
		this.sendDelayedAction(channel,
				"looks around for a convenient window, then slinks off...",
				time);
		time += this.rand.nextInt(10) * 500 + 1500;

		int i = this.rand.nextInt(100);
		String act = "";
		String colour = this.colours
				.get(this.rand.nextInt(this.colours.size()));

		if (i > 20) {
			act = "throws "
					+ target
					+ " through the nearest window, where they land on a giant pile of fluffy "
					+ colour + " coloured pillows.";
		}
		else if (i > 3) {
			target = sender;
			act = "laughs maniacally then throws "
					+ target
					+ " through the nearest window, where they land on a giant pile of fluffy "
					+ colour + " coloured pillows.";
		}
		else {
			act = "trips and falls out the window!";
		}
		this.sendDelayedAction(channel, act, time);
	}

	private void summon(String channel, String sender, String[] args,
			Boolean righto) {
		String target = "";
		if (args == null || args.length == 0) {
			target = this.deities.get(this.rand.nextInt(this.deities.size()));
		}
		else {
			target = this.implodeArray(args);
		}

		if (righto) {
			this.sendMessage(channel, "Righto...");
		}

		int time = 2 + this.rand.nextInt(15);
		time *= 1000;
		this.sendDelayedAction(channel,
				"prepares the summoning circle required to bring " + target
						+ " into the world...", time);
		time += this.rand.nextInt(10) * 500 + 1500;

		int i = this.rand.nextInt(100);
		String act = "";

		if (i > 50) {
			act = "completes the ritual successfully, drawing " + target
					+ " through, and binding them into the summoning circle!";
		}
		else if (i > 30) {
			act = "completes the ritual, drawing "
					+ target
					+ " through, but something goes wrong and they fade away after just a few moments.";
		}
		else {
			String target2 = this.deities.get(this.rand.nextInt(this.deities
					.size()));
			act = "attempts to summon "
					+ target
					+ ", but something goes horribly wrong. After the smoke clears, "
					+ target2
					+ " is left standing on the smoldering remains of the summoning circle.";
		}
		this.sendDelayedAction(channel, act, time);
	}

	private void foof(String channel, String sender, String[] args,
			Boolean righto) {
		String target = sender;
		if (args != null && args.length > 0) {
			if (!args[0].equalsIgnoreCase(this.getNick())
					&& !args[0].equalsIgnoreCase("himself")
					&& !args[0].equalsIgnoreCase("herself")
					&& !this.admin_list.contains(args[0])) {
				target = this.implodeArray(args);
			}
		}

		if (righto) {
			this.sendMessage(channel, "Righto...");
		}

		int time = 2 + this.rand.nextInt(15);
		time *= 1000;
		this.sendDelayedAction(
				channel,
				"surreptitiously works his way over to the couch, looking ever so casual...",
				time);
		time += this.rand.nextInt(10) * 500 + 1500;

		int i = this.rand.nextInt(100);
		String act = "";
		String colour = this.colours
				.get(this.rand.nextInt(this.colours.size()));

		if (i > 20) {
			act = "grabs a " + colour + " pillow, and throws it at " + target
					+ ", hitting them squarely in the back of the head.";
		}
		else if (i > 3) {
			target = sender;
			act = "laughs maniacally then throws a " + colour + " pillow at "
					+ target
					+ ", then runs off and hides behind the nearest couch.";
		}
		else {
			act = "trips and lands on a " + colour + " pillow. Oof!";
		}
		this.sendDelayedAction(channel, act, time);
	}

	// !endwar <name>
	private void endWar(String channel, String sender, String[] args) {
		if (args != null && args.length > 0) {
			String name = this.implodeArray(args);
			if (this.wars.containsKey(name.toLowerCase())) {
				if (sender.equalsIgnoreCase(this.wars.get(name.toLowerCase())
						.getStarter())
						|| this.admin_list.contains(sender)
						|| this.admin_list.contains(channel)) {
					WordWar war = this.wars.remove(name.toLowerCase());
					this.sendMessage(channel, "The war '" + war.getName()
							+ "' has been ended.");
				}
				else {
					this.sendMessage(channel, sender
							+ ": Only the starter of a war can end it early.");
				}
			}
			else {
				this.sendMessage(channel, sender
						+ ": I don't know of a war with name: '" + name + "'");
			}
		}
		else {
			this.sendMessage(channel, sender + ": I need a war name to end.");
		}
	}

	private void setChannelAdultFlagParse(String channel, String sender,
			String[] args) {
		boolean flag;

		if (!"0".equals(args[1])) {
			flag = true;
		}
		else {
			flag = false;
		}

		this.setChannelAdultFlag(args[0], flag);
		this.sendMessage(channel, sender + ": Channel adult flag updated.");
	}

	private void startWar(String channel, String sender, String[] args) {
		long time;
		long to_start = 5000;
		String warname = "";
		try {
			time = (long) (Double.parseDouble(args[0]) * 60);
		}
		catch (Exception e) {
			this.sendMessage(
					channel,
					sender
							+ ": could not understand the duration parameter. Was it numeric?");
			return;
		}
		if (args.length >= 2) {
			try {
				to_start = (long) (Double.parseDouble(args[1]) * 60);
			}
			catch (Exception e) {
				this.sendMessage(
						channel,
						sender
								+ ": could not understand the time to start parameter. Was it numeric?");
				return;
			}

		}
		if (args.length >= 3) {
			warname = args[2];
			for (int i = 3; i < args.length; i++) {
				warname = warname + " " + args[i];
			}
		}
		else {
			warname = sender + "'s war";
		}

		if (Double.parseDouble(args[0]) < 1 || Double.parseDouble(args[1]) < 1) {
			this.sendMessage(channel, sender
					+ ": Start delay and duration most both be at least 1.");
			return;
		}

		if (!this.wars.containsKey(warname.toLowerCase())) {
			WordWar war = new WordWar(time, to_start, warname, sender, channel);
			this.wars.put(war.getName().toLowerCase(), war);
			if (to_start > 0) {
				this.sendMessage(channel, sender
						+ ": your wordwar will start in " + to_start / 60.0
						+ " minutes.");
			}
			else {
				this.beginWar(war);
			}
		}
		else {
			this.sendMessage(channel, sender
					+ ": there is already a war with the name '" + warname
					+ "'");
		}
	}

	private void listAllWars(String channel, String sender, String[] args) {
		this.listWars(channel, sender, args, true);
	}

	private void listWars(String channel, String sender, String[] args,
			boolean all) {
		String target = args != null ? sender : channel;
		if (this.wars != null && this.wars.size() > 0) {
			for (Map.Entry<String, WordWar> wm : this.wars.entrySet()) {
				if (all || wm.getValue().getChannel().equalsIgnoreCase(channel)) {
					this.sendMessage(target, all ? wm.getValue()
							.getDescriptionWithChannel() : wm.getValue()
							.getDescription());
				}
			}
		}
		else {
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
							default:
								if ((int) war.time_to_start % 300 == 0) {
									this.warStartCount(war);
								}
								break;
						}
						if (war.time_to_start == 0) {
							this.beginWar(war);
						}
					}
					else if (war.remaining > 0) {
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
			}
			catch (Throwable e) {
				this.wars_lock.release();
			}
		}
	}

	private void warStartCount(WordWar war) {
		if (war.time_to_start < 60) {
			this.sendMessage(war.getChannel(), war.getName() + ": Starting in "
					+ war.time_to_start
					+ (war.time_to_start == 1 ? " second" : " seconds") + "!");
		}
		else {
			int time_to_start = (int) war.time_to_start / 60;
			this.sendMessage(war.getChannel(), war.getName() + ": Starting in "
					+ time_to_start
					+ (time_to_start == 1 ? " minute" : " minutes") + "!");
		}
	}

	private void warEndCount(WordWar war) {
		if (war.remaining < 60) {
			this.sendMessage(war.getChannel(), war.getName() + ": "
					+ war.remaining
					+ (war.remaining == 1 ? " second" : " seconds")
					+ " remaining!");
		}
		else {
			int remaining = (int) war.remaining / 60;
			this.sendMessage(war.getChannel(), war.getName() + ": " + remaining
					+ (remaining == 1 ? " minute" : " minutes") + " remaining.");
		}
	}

	private void beginWar(WordWar war) {
		this.sendNotice(war.getChannel(), "WordWar: '" + war.getName()
				+ " 'starts now! (" + war.getDuration() / 60 + " minutes)");
	}

	private void endWar(WordWar war) {
		this.sendNotice(war.getChannel(), "WordWar: '" + war.getName()
				+ "' is over!");
		this.wars.remove(war.getName().toLowerCase());
	}

	private void boxodoom(String channel, String sender, String[] args) {
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
			PreparedStatement s = this.mysql
					.prepareStatement("SELECT `challenge` FROM `box_of_doom` WHERE `difficulty` = ? ORDER BY rand() LIMIT 1");
			s.setString(1, args[0]);
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				value = rs.getString("challenge");
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		base_wpm = (long) Double.parseDouble(value);
		modifier = 1.0 / Math.log(duration + 1.0) / 1.5 + 0.68;
		goal = (int) (duration * base_wpm * modifier / 10) * 10;

		this.sendMessage(channel,
				sender + ": Your goal is " + String.valueOf(goal));
	}

	private void useBackupNick() {
		this.setName(this.getSetting("backup_nickname"));
	}

	private void connectToServer() {
		try {
			this.connect(this.getSetting("server"));
		}
		catch (Exception e) {
			this.useBackupNick();
			try {
				this.connect(this.getSetting("server"));
			}
			catch (Exception ex) {
				System.err.print("Could not connect - name & backup in use");
				System.exit(1);
			}
		}

		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT * FROM `channels`");

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				this.joinChannel(rs.getString("channel"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		this.joinChannel(this.debugChannel);
	}

	public static void main(String[] args) {
		Tim bot = new Tim();

		bot.setLogin("ThereAreSomeWhoCallMeTim_Bot");
		bot.setVerbose(true);
		bot.setMessageDelay(350);
		bot.connectToServer();
	}

	private String implodeArray(String[] inputArray) {
		String AsImplodedString;
		if (inputArray.length == 0) {
			AsImplodedString = "";
		}
		else {
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

	private String getSetting(String key) {
		String value = "";
		try {
			PreparedStatement s = this.mysql
					.prepareStatement("SELECT `value` FROM `settings` WHERE `key` = ?");
			s.setString(1, key);
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				value = rs.getString("value");
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		return value;
	}

	private void updateSetting(String setting, String value) {
		try {
			PreparedStatement s = this.mysql
					.prepareStatement("UPDATE `settings` SET value = ? WHERE `key` = ?");
			s.setString(1, value);
			s.setString(2, setting);
			s.executeUpdate();

			this.settings.put(setting, value);
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getApprovedItems() {
		try {
			String value = "";
			this.approved_items.clear();

			PreparedStatement s = this.mysql
					.prepareStatement("SELECT `item` FROM `items` WHERE `approved` = TRUE");
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("item");
				this.approved_items.add(value);
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		return;
	}

	private void getPendingItems() {
		try {
			String value = "";
			this.pending_items.clear();

			PreparedStatement s = this.mysql
					.prepareStatement("SELECT `item` FROM `items` WHERE `approved` = FALSE");
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("item");
				this.pending_items.add(value);
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		return;
	}

	private void insertPendingItem(String item) {
		try {
			PreparedStatement s = this.mysql
					.prepareStatement("INSERT INTO `items` (`item`, `approved`) VALUES (?, FALSE)");
			s.setString(1, item);
			s.executeUpdate();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		return;
	}

	private void setItemApproved(String item, Boolean approved) {
		try {
			PreparedStatement s = this.mysql
					.prepareStatement("UPDATE `items` SET `approved` = ? WHERE `item` = ?");
			s.setBoolean(1, approved);
			s.setString(2, item);
			s.executeUpdate();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		return;
	}

	private void removeItem(String item) {
		try {
			PreparedStatement s = this.mysql
					.prepareStatement("DELETE FROM `items` WHERE `item` = ?");
			s.setString(1, item);
			s.executeUpdate();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		return;
	}

	private void refreshDbLists() {
		this.getAdminList();
		this.getIgnoreList();
		this.getAdultChannelList();
		this.getAypwipList();
		this.getColourList();
		this.getCommandmentList();
		this.getDeityList();
		this.getEightballList();
		this.getFlavourList();
		this.getGreetingList();
		this.getApprovedItems();
		this.getPendingItems();

		this.chatterMaxBaseOdds = Integer.parseInt(this
				.getSetting("chatterMaxBaseOdds"));
		this.chatterNameMultiplier = Integer.parseInt(this
				.getSetting("chatterNameMultiplier"));
		this.chatterTimeMultiplier = Integer.parseInt(this
				.getSetting("chatterTimeMultiplier"));
		this.chatterTimeDivisor = Integer.parseInt(this
				.getSetting("chatterTimeDivisor"));

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

		this.sendMessage(this.debugChannel,
				"Max Base Odds: " + Integer.toString(this.chatterMaxBaseOdds));
		this.sendMessage(
				this.debugChannel,
				"Name Multiplier: "
						+ Integer.toString(this.chatterNameMultiplier));
		this.sendMessage(
				this.debugChannel,
				"Time Multiplier: "
						+ Integer.toString(this.chatterTimeMultiplier));
		this.sendMessage(this.debugChannel,
				"Time Divisor: " + Integer.toString(this.chatterTimeDivisor));

	}

	private void getAdminList() {
		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT `name` FROM `admins`");

			ResultSet rs = s.getResultSet();

			this.admin_list.clear();
			while (rs.next()) {
				this.admin_list.add(rs.getString("name"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getIgnoreList() {
		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT `name` FROM `ignores`");

			ResultSet rs = s.getResultSet();
			this.ignore_list.clear();
			while (rs.next()) {
				this.ignore_list.add(rs.getString("name"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void setIgnore(String username) {
		try {
			PreparedStatement s = this.mysql
					.prepareStatement("INSERT INTO `ignores` (`name`) VALUES (?);");
			s.setString(1, username);
			s.executeUpdate();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void removeIgnore(String username) {
		try {
			PreparedStatement s = this.mysql
					.prepareStatement("DELETE FROM `ignores` WHERE `name` = ?;");
			s.setString(1, username);
			s.executeUpdate();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getAdultChannelList() {
		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT `channel` FROM `channels` WHERE `adult`=1");

			ResultSet rs = s.getResultSet();
			this.adult_channels.clear();
			while (rs.next()) {
				this.adult_channels.add(rs.getString("channel"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getAypwipList() {
		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT `string` FROM `aypwips`");

			ResultSet rs = s.getResultSet();
			this.aypwips.clear();
			while (rs.next()) {
				this.aypwips.add(rs.getString("string"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getColourList() {
		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT `string` FROM `colours`");

			ResultSet rs = s.getResultSet();
			this.colours.clear();
			while (rs.next()) {
				this.colours.add(rs.getString("string"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getCommandmentList() {
		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT `string` FROM `commandments`");

			ResultSet rs = s.getResultSet();
			this.commandments.clear();
			while (rs.next()) {
				this.commandments.add(rs.getString("string"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getDeityList() {
		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT `string` FROM `deities`");

			ResultSet rs = s.getResultSet();
			this.deities.clear();
			while (rs.next()) {
				this.deities.add(rs.getString("string"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getEightballList() {
		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT `string` FROM `eightballs`");

			ResultSet rs = s.getResultSet();
			this.eightballs.clear();
			while (rs.next()) {
				this.eightballs.add(rs.getString("string"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getFlavourList() {
		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT `string` FROM `flavours`");

			ResultSet rs = s.getResultSet();
			this.flavours.clear();
			while (rs.next()) {
				this.flavours.add(rs.getString("string"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getGreetingList() {
		try {
			Statement s = this.mysql.createStatement();
			s.executeQuery("SELECT `string` FROM `greetings`");

			ResultSet rs = s.getResultSet();
			this.greetings.clear();
			while (rs.next()) {
				this.greetings.add(rs.getString("string"));
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void saveChannel(String channel) {
		try {
			PreparedStatement s = this.mysql
					.prepareStatement("INSERT INTO `channels` (`channel`, `adult`) VALUES (?, 0)");
			s.setString(1, channel);
			s.executeUpdate();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void deleteChannel(String channel) {
		try {
			PreparedStatement s = this.mysql
					.prepareStatement("DELETE FROM `channels` WHERE `channel` = ?");
			s.setString(1, channel);
			s.executeUpdate();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void setChannelAdultFlag(String channel, boolean adult) {
		try {
			PreparedStatement s = this.mysql
					.prepareStatement("UPDATE `channels` SET adult = ? WHERE `channel` = ?");
			s.setBoolean(1, adult);
			s.setString(2, channel);
			s.executeUpdate();

			if (adult) {
				this.adult_channels.add(channel);
			}
			else {
				this.adult_channels.remove(channel);
			}
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
