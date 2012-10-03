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
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
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

	public static void main(String[] args) {
		instance = new Tim();
	}

	public Tim() {
		rand = new Random();
		story = new ChainStory();
		challenge = new Challenge();
		markhov = new MarkhovChains();
		amusement = new Amusement();

		bot = new PircBotX();

		bot.getListenerManager().addListener(new AdminCommandListener());
		bot.getListenerManager().addListener(new UserCommandListener());
		bot.getListenerManager().addListener(new ReactionListener());
		
		bot.setEncoding(Charset.forName("UTF-8"));
		bot.setLogin("WarMech");
		bot.setMessageDelay(Long.parseLong(db.getSetting("max_rate")));
		bot.setName(db.getSetting("nickname"));
		bot.setVerbose(true);

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

	public static void sendDelayedMessage(Channel channel, String message, int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		bot.sendMessage(channel, message);
	}

	public static void sendDelayedAction(Channel channel, String message, int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		bot.sendAction(channel, message);
	}

	public static void sendDelayedNotice(User user, String message, int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		bot.sendNotice(user, message);
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
//		if (!this.ignore_list.contains(sender)) {
//			// Other fun stuff we can make him do
//			} else {
//			}
//
//			if (!sender.equals(this.getNick()) && !"".equals(channel)) {
//			}
//		}
//	}
//
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
}
