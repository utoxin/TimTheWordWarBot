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
		bot.getListenerManager().addListener(new ServerListener());
		
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
}