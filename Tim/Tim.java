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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.exception.NickAlreadyInUseException;

public class Tim {
	public static Amusement amusement;
	public static PircBotX bot;
	public static Challenge challenge;
	public static AppConfig config = AppConfig.getInstance();
	public static DBAccess db = DBAccess.getInstance();
	public static Tim instance;
	public static MarkhovChains markhov;
	public static Random rand;
	public static ChainStory story;
	public static WarTicker warticker = WarTicker.getInstance();

	public static void main( String[] args ) {
		instance = new Tim();
	}

	public Tim() {
		rand = new Random();
		story = new ChainStory();
		challenge = new Challenge();
		markhov = new MarkhovChains();
		amusement = new Amusement();

		bot = new PircBotX();
		bot.getListenerManager().addListener(new SimpleResponses());
		bot.setEncoding(Charset.forName("UTF-8"));
		bot.setLogin("WarMech");
		bot.setMessageDelay(Long.parseLong(db.getSetting("max_rate")));
		bot.setName(db.getSetting("nickname"));

		try {
			bot.connect(db.getSetting("server"));
		} catch (IOException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		} catch (NickAlreadyInUseException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IrcException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		bot.identify(db.getSetting("password"));

		String post_identify = db.getSetting("post_identify");
		if (!"".equals(post_identify)) {
			bot.sendRawLineNow(post_identify);
		}

		db.refreshDbLists();

		// Join our channels
		for (Enumeration<ChannelInfo> e = db.channel_data.elements(); e.hasMoreElements();) {
			bot.joinChannel(e.nextElement().Name);
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

//	@Override
//	protected void onAction( String sender, String login, String hostname,
//							 String target, String action ) {
//
//		
//		action = Colors.removeFormattingAndColors(action);
//
//		if (this.admin_list.contains(sender)) {
//			if (action.equalsIgnoreCase("punches " + this.getNick()
//										+ " in the face!")) {
//				this.sendAction(target, "falls over and dies.  x.x");
//				this.shutdown = true;
//				this.quitServer();
//				System.exit(0);
//			}
//		}
//
//	}
//
//	@Override
//	public void onMessage( String channel, String sender, String login,
//						   String hostname, String message ) {
//
//		ChannelInfo cdata = this.channel_data.get(channel.toLowerCase());
//		
//		message = Colors.removeFormattingAndColors(message);
//
//		if (!this.ignore_list.contains(sender)) {
//			// Find all messages that start with ! and pass them to a method for
//			// further processing.
//			if (message.charAt(0) == '!') {
//				this.doCommand(channel, sender, "!", message);
//				return;
//			} // Notation for wordcounts
//			else if (message.charAt(0) == '@') {
//				this.doCommand(channel, sender, "@", message);
//				return;
//			} else if (message.charAt(0) == '$') {
//				this.doAdmin(channel, sender, '$', message.substring(1));
//				return;
//			}
//
//			// Other fun stuff we can make him do
//			} else if (message.toLowerCase().contains(
//				"are you thinking what i'm thinking")
//					   || message.toLowerCase().contains(
//				"are you pondering what i'm pondering")) {
//				int i = this.rand.nextInt(amusement.aypwips.size());
//				this.sendMessage(channel,
//					String.format(amusement.aypwips.get(i), sender));
//				return;
//			} else {
//				} else if (Pattern.matches("(?i)" + this.getNick() + ".*[?]",
//					message)) {
//					int r = this.rand.nextInt(100);
//
//					if (r < 50) {
//						amusement.eightball(channel, sender, false);
//						return;
//					}
//				} else if (Pattern.matches("(?i).*markhov test.*", message)) {
//					this.sendDelayedMessage(channel, markhov.generate_markhov("say"), this.rand.nextInt(1500));
//					return;
//				}
//			}
//
//			if (!sender.equals(this.getNick()) && !"".equals(channel)) {
//				this.interact(sender, channel, message, "say");
//				if (cdata.doMarkhov) {
//					markhov.process_markhov(message, "say");
//				}
//			}
//		}
//	}
//
//	private void doAdmin( String channel, String sender, char c, String message ) {
//		// Method for processing admin commands.
//		if (this.admin_list.contains(sender.toLowerCase()) || this.admin_list.contains(channel.toLowerCase())) {
//			String command;
//			String[] args = null;
//
//			int space = message.indexOf(" ");
//			if (space > 0) {
//				command = message.substring(0, space).toLowerCase();
//				args = message.substring(space + 1).split(" ", 0);
//			} else {
//				command = message.substring(0).toLowerCase();
//			}
//
//			if (command.equals("setadultflag")) {
//				if (args != null && args.length == 2) {
//					String target = args[0].toLowerCase();
//					if (this.channel_data.containsKey(target)) {
//						boolean flag = false;
//						if (!"0".equals(args[1])) {
//							flag = true;
//						}
//
//						this.setChannelAdultFlag(target, flag);
//						this.sendMessage(channel, sender + ": Channel adult flag updated for " + target);
//					} else {
//						this.sendMessage(channel, "I don't know about " + target);
//					}
//				} else {
//					this.sendMessage(channel,
//						"Use: $setadultflag <#channel> <0/1>");
//				}
//			} else if (command.equals("setmuzzleflag")) {
//				if (args != null && args.length == 2) {
//					String target = args[0].toLowerCase();
//					if (this.channel_data.containsKey(target)) {
//						boolean flag = false;
//						if (!"0".equals(args[1])) {
//							flag = true;
//						}
//
//						this.setChannelMuzzledFlag(target, flag);
//						this.sendMessage(channel, sender + ": Channel muzzle flag updated for " + target);
//					} else {
//						this.sendMessage(channel, "I don't know about " + target);
//					}
//				} else {
//					this.sendMessage(channel, "Usage: $setmuzzleflag <#channel> <0/1>");
//				}
//			} else if (command.equals("shutdown")) {
//				this.sendMessage(channel, "Shutting down...");
//				this.shutdown = true;
//				this.quitServer("I am shutting down! Bye!");
//				System.exit(0);
//			} else if (command.equals("reload") || command.equals("refreshdb")) {
//				this.sendMessage(channel, "Reading database tables ...");
//				this.refreshDbLists();
//				this.sendDelayedMessage(channel, "Tables reloaded.", 1000);
//			} else if (command.equals("reset")) {
//				this.sendMessage(channel, "Restarting internal timer...");
//
//				try {
//					this.warticker.cancel();
//					this.warticker = null;
//
//					this.ticker.cancel();
//					this.ticker = null;
//				} catch (Exception e) {
//				}
//
//				this.warticker = new WarClockThread(this);
//				this.ticker = new Timer(true);
//				this.ticker.scheduleAtFixedRate(this.warticker, 0, 1000);
//				this.refreshDbLists();
//
//				this.sendDelayedMessage(channel, "Can you hear me now?", 2000);
//			} else if (command.equals("ignore")) {
//				if (args != null && args.length > 0) {
//					String users = "";
//					for (int i = 0; i < args.length; ++i) {
//						users += " " + args[i];
//						this.ignore_list.add(args[i]);
//						this.setIgnore(args[i]);
//					}
//					this.sendMessage(channel,
//						"The following users have been ignored:" + users);
//				} else {
//					this.sendMessage(channel,
//						"Usage: $ignore <user 1> [ <user 2> [<user 3> [...] ] ]");
//				}
//			} else if (command.equals("unignore")) {
//				if (args != null && args.length > 0) {
//					String users = "";
//					for (int i = 0; i < args.length; ++i) {
//						users += " " + args[i];
//						this.ignore_list.remove(args[i]);
//						this.removeIgnore(args[i]);
//					}
//					this.sendMessage(channel,
//						"The following users have been unignored:" + users);
//				} else {
//					this.sendMessage(channel,
//						"Usage: $unignore <user 1> [ <user 2> [<user 3> [...] ] ]");
//				}
//			} else if (command.equals("listignores")) {
//				this.sendMessage(channel,
//					"There are " + this.ignore_list.size()
//					+ " users ignored.");
//				Iterator<String> iter = this.ignore_list.iterator();
//				while (iter.hasNext()) {
//					this.sendMessage(channel, iter.next());
//				}
//			} else if (command.equals("help")) {
//				this.printAdminCommandList(sender, channel);
//			} else if (this.amusement.parseAdminCommand(channel, sender, message)) {
//			} else if (this.story.parseAdminCommand(channel, sender, message)) {
//			} else if (this.challenge.parseAdminCommand(channel, sender, message)) {
//			} else if (this.markhov.parseAdminCommand(channel, sender, message)) {
//			} else {
//				this.sendMessage(channel, "$" + command + " is not a valid admin command - try $help");
//			}
//		} else {
//			// The sender is NOT an admin
//			this.sendMessage(
//				this.debugChannel,
//				String.format(
//				"User %s in channel %s attempted to use an admin command (%s)!",
//				sender, channel, message));
//			this.sendMessage(
//				channel,
//				String.format(
//				"%s: You are not an admin. Only Admins have access to that command.",
//				sender));
//		}
//	}
//
//	private void interact( String sender, String channel, String message, String type ) {
//		ChannelInfo cdata = this.channel_data.get(channel.toLowerCase());
//
//		long elapsed = System.currentTimeMillis() / 1000 - cdata.chatterTimer;
//		long odds = (long) Math.log(elapsed) * cdata.chatterTimeMultiplier;
//		if (odds > cdata.chatterMaxBaseOdds) {
//			odds = cdata.chatterMaxBaseOdds;
//		}
//
//		if (message.toLowerCase().contains(this.getNick().toLowerCase())) {
//			odds = odds * cdata.chatterNameMultiplier;
//		}
//
//		if (this.rand.nextInt(100) < odds) {
//			String[] actions;
//
//			if (cdata.doMarkhov && !cdata.doRandomActions) {
//				actions = new String[] {
//					"markhov"
//				};
//			} else if (cdata.doMarkhov && cdata.doRandomActions) {
//				actions = new String[] {
//					"markhov",
//					"challenge",
//					"amusement",
//					"amusement",
//				};
//			} else if (cdata.doMarkhov && cdata.doRandomActions) {
//				actions = new String[] {
//					"challenge",
//					"amusement",
//					"amusement",
//				};
//			} else {
//				return;
//			}
//
//			String action = actions[rand.nextInt(actions.length)];
//			
//			if ("markhov".equals(action)) {
//				markhov.randomAction(sender, channel, message, type);
//			} else if ("challenge".equals(action)) {
//				challenge.randomAction(sender, channel, message, type);
//			} else if ("amusement".equals(action)) {
//				amusement.randomAction(sender, channel, message, type);
//			}
//
//			cdata.chatterTimer += this.rand.nextInt((int) elapsed / cdata.chatterTimeDivisor);
//			channelLog("Chattered On: " + cdata.Name + "   Odds: " + Long.toString(odds) + "%");
//		}
//	}
//
//	@Override
//	protected void onPrivateMessage( String sender, String login,
//									 String hostname, String message ) {
//		message = Colors.removeFormattingAndColors(message);
//
//		if (this.admin_list.contains(sender)) {
//			String[] args = message.split(" ");
//			if (args != null && args.length > 2) {
//				String msg = "";
//				for (int i = 2; i < args.length; i++) {
//					msg += args[i] + " ";
//				}
//				if (args[0].equalsIgnoreCase("say")) {
//					this.sendMessage(args[1], msg);
//				} else if (args[0].equalsIgnoreCase("act")) {
//					this.sendAction(args[1], msg);
//				}
//			}
//		}
//	}
//
//	@Override
//	protected void onServerPing(String response) {
//		/**
//		 * This loop is used to reduce the chatter odds on idle channels, by periodically triggering idle chatter
//		 * in channels. If they currently have chatter turned off, this simply decreases their timer, and then
//		 * goes on. That way, the odds don't build up to astronomical levels while people are idle or away, resulting
//		 * in lots of spam when they come back.
//		 */
//		for (ChannelInfo cdata : this.channel_data.values()) {
//			cdata = this.channel_data.get(cdata.Name);
//			
//			long elapsed = System.currentTimeMillis() / 1000 - cdata.chatterTimer;
//			long odds = (long) Math.log(elapsed) * cdata.chatterTimeMultiplier;
//			if (odds > cdata.chatterMaxBaseOdds) {
//				odds = cdata.chatterMaxBaseOdds;
//			}
//			
//			channelLog("Channel: " + cdata.Name + "  Odds: " + Long.toString(odds) + "   Timer: " + Long.toString(cdata.chatterTimer));
//			
//			if (this.rand.nextInt(100) < odds) {
//				String[] actions;
//
//				cdata.chatterTimer += this.rand.nextInt((int) elapsed / cdata.chatterTimeDivisor);
//				channelLog("Idle Chattered On: " + cdata.Name + "   Odds: " + Long.toString(odds) + "%");
//
//				if (cdata.doMarkhov && !cdata.doRandomActions) {
//					actions = new String[] {
//						"markhov"
//					};
//				} else if (cdata.doMarkhov && cdata.doRandomActions) {
//					actions = new String[] {
//						"markhov",
//						"amusement"
//					};
//				} else if (cdata.doMarkhov && cdata.doRandomActions) {
//					actions = new String[] {
//						"amusement"
//					};
//				} else {
//					continue;
//				}
//
//				String action = actions[rand.nextInt(actions.length)];
//
//				if ("markhov".equals(action)) {
//					markhov.randomAction(null, cdata.Name, null, "say");
//				} else if ("amusement".equals(action)) {
//					amusement.randomAction(null, cdata.Name, null, "say");
//				}
//			}
//		}
//
//		super.onServerPing(response);
//	}
//	
//	@Override
//	protected void onInvite( String targetNick, String sourceNick,
//							 String sourceLogin, String sourceHostname, String channel ) {
//		if (!this.ignore_list.contains(sourceNick)
//			&& targetNick.equals(this.getNick())) {
//			if (!this.channel_data.containsKey(channel.toLowerCase())) {
//				this.joinChannel(channel);
//				this.saveChannel(channel.toLowerCase());
//			}
//		}
//	}
//
//	@Override
//	protected void onKick( String channel, String kickerNick, String kickerLogin,
//						   String kickerHostname, String recipientNick, String reason ) {
//		if (!kickerNick.equals(this.getNick())) {
//			this.deleteChannel(channel.toLowerCase());
//		}
//	}
//
//	@Override
//	protected void onPart( String channel, String sender, String login,
//						   String hostname ) {
//		if (!sender.equals(this.getNick())) {
//			User[] userlist = this.getUsers(channel);
//			if (userlist.length <= 1) {
//				this.partChannel(channel);
//				this.deleteChannel(channel.toLowerCase());
//			}
//		}
//	}
//
//	@Override
//	public void onJoin( String channel, String sender, String login,
//						String hostname ) {
//		if (!sender.equals(this.getName()) && !login.equals(this.getLogin())) {
//			String message = greetings.get(rand.nextInt(greetings.size()));
//			if (this.wars.size() > 0) {
//				int warscount = 0;
//				String winfo = "";
//				for (Map.Entry<String, WordWar> wm : this.wars.entrySet()) {
//					if (wm.getValue().getChannel().equalsIgnoreCase(channel)) {
//						winfo += wm.getValue().getDescription();
//						if (warscount > 0) {
//							winfo += " || ";
//						}
//						warscount++;
//					}
//				}
//
//				if (warscount > 0) {
//					boolean plural = warscount >= 2 || warscount == 0;
//					message += " There " + ( plural ? "are" : "is" ) + " "
//							   + warscount + " war" + ( plural ? "s" : "" )
//							   + " currently " + "running in this channel"
//							   + ( warscount > 0 ? ": " + winfo : "." );
//				}
//			}
//			this.sendDelayedMessage(channel, String.format(message, sender), 1000);
//
//			if (Pattern.matches("(?i)mib_......", sender)
//				|| Pattern.matches("(?i)guest.*", sender)) {
//				this.sendDelayedMessage(
//					channel,
//					String.format(
//					"%s: To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere",
//					sender), 1500);
//			}
//
//			int r = this.rand.nextInt(100);
//
//			if (r < 10) {
//				r = this.rand.nextInt(this.extra_greetings.size());
//				this.sendDelayedMessage(channel, this.extra_greetings.get(r), 2000);
//			}
//		}
//	}
//
//	public void doCommand( String channel, String sender, String prefix,
//						   String message ) {
//	}
//
//	private void printCommandList( String target, String channel ) {
//		this.sendAction(channel, "whispers in " + target + "'s ear. (Check for a new windor or tab with the help text.)");
//
//		String[] strs = {"I am a robot trained by the WordWar Monks of Honolulu. You have "
//						 + "never heard of them. It is because they are awesome.",
//						 "Core Commands:",
//						 "    !startwar <duration> <time to start> <an optional name> - Starts a word war",
//						 "    !listwars - I will tell you about the wars currently in progress.",
//						 "    !boxodoom <difficulty> <duration> - Difficulty is easy/average/hard, duration in minutes.",
//						 "    !eggtimer <time> - I will send you a message after <time> minutes.",
//						 "    !settopic <topic> - If able, I will try to set the channel's topic.",
//						 "    !credits - Details of my creators, and where to find my source code.",
//		};
//		for (int i = 0; i < strs.length; ++i) {
//			this.sendNotice(target, strs[i]);
//		}
//
//		this.story.helpSection(target, channel);
//		this.challenge.helpSection(target, channel);
//		this.amusement.helpSection(target, channel);
//		
//		String[] post = {"I... I think there might be other tricks I know... You'll have to find them!",
//						 "I will also respond to the /invite command if you would like to see me in another channel. "
//		};
//		for (int i = 0; i < post.length; ++i) {
//			this.sendNotice(target, post[i]);
//		}
//	}
//
//	private void printAdminCommandList( String target, String channel ) {
//		this.sendAction(channel, "whispers in " + target + "'s ear. (Check for a new windor or tab with the help text.)");
//
//		String[] helplines = {"Core Admin Commands:",
//							  "    $setadultflag <#channel> <0/1> - clears/sets adult flag on channel",
//							  "    $setmuzzleflag <#channel> <0/1> - clears/sets muzzle flag on channel",
//							  "    $shutdown - Forces bot to exit",
//							  "    $reload - Reloads data from MySQL (also $refreshdb)",
//							  "    $reset - Resets internal timer for wars, and reloads data from MySQL",
//							  "    $listitems [ <page #> ] - lists all currently approved !get/!getfor items",
//							  "    $listpending [ <page #> ] - lists all unapproved !get/!getfor items",
//							  "    $approveitem <item # from $listpending> - removes item from pending list and marks as approved for !get/!getfor",
//							  "    $disapproveitem <item # from $listitems> - removes item from approved list and marks as pending for !get/!getfor",
//							  "    $deleteitem <item # from $listpending> - permanently removes an item from the pending list for !get/!getfor",
//							  "    $ignore <username> - Places user on the bot's ignore list",
//							  "    $unignore <username> - Removes user from bot's ignore list",
//							  "    $listignores - Prints the list of ignored users"
//		};
//
//		for (int i = 0; i < helplines.length; ++i) {
//			this.sendNotice(target, helplines[i]);
//		}
//
//		this.challenge.adminHelpSection(target, channel);
//		this.markhov.adminHelpSection(target, channel);
//	}
//
//	protected boolean isChannelAdult( String channel ) {
//		boolean val = false;
//		ChannelInfo cdata = this.channel_data.get(channel.toLowerCase());
//		if (cdata != null) {
//			val = cdata.isAdult;
//		}
//		return val;
//	}
}
