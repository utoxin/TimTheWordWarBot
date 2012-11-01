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

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.ServerPingEvent;

/**
 *
 * @author mwalker
 */
public class MarkhovChains extends ListenerAdapter {
	private DBAccess db = DBAccess.getInstance();
	protected HashMap<String, Pattern> badwordPatterns = new HashMap<String, Pattern>();
	private long timeout = 3000;

	@Override
	public void onMessage( MessageEvent event ) {
		PircBotX bot = event.getBot();

		ChannelInfo cdata = db.channel_data.get(event.getChannel().getName().toLowerCase());

		if (!event.getUser().getNick().equals("Skynet") && !event.getUser().getNick().equals(bot.getNick()) && !"".equals(event.getChannel().getName())) {
			if (cdata.doMarkov) {
				process_markhov(Colors.removeFormattingAndColors(event.getMessage()), "say");
			}
		}
	}

	@Override
	public void onAction( ActionEvent event ) {
		PircBotX bot = event.getBot();

		ChannelInfo cdata = db.channel_data.get(event.getChannel().getName().toLowerCase());

		if (!event.getUser().getNick().equals("Skynet") && !event.getUser().getNick().equals(bot.getNick()) && !"".equals(event.getChannel().getName())) {
			if (cdata.doMarkov) {
				process_markhov(Colors.removeFormattingAndColors(event.getMessage()), "emote");
			}
		}
	}

	/**
	 * Parses user-level commands passed from the main class. Returns true if the message was handled, false if it was
	 * not.
	 *
	 * @param channel What channel this came from
	 * @param sender  Who sent the command
	 * @param prefix  What prefix was on the command
	 * @param message What was the actual content of the message
	 *
	 * @return True if message was handled, false otherwise.
	 */
	public boolean parseUserCommand( String channel, String sender, String prefix, String message ) {
		return false;
	}

	/**
	 * Parses admin-level commands passed from the main class. Returns true if the message was handled, false if it was
	 * not.
	 *
	 * @param channel What channel this came from
	 * @param sender  Who sent the command
	 * @param message What was the actual content of the message.
	 *
	 * @return True if message was handled, false otherwise
	 */
	public boolean parseAdminCommand( MessageEvent event ) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		String command;
		String argsString;
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			argsString = message.substring(space + 1);
			args = argsString.split(" ", 0);
		} else {
			command = message.toLowerCase().substring(1);
		}

		if (command.equals("badword")) {
			if (args != null && args.length == 1) {
				addBadWord(args[0], event.getChannel());
				return true;
			}
		}

		return false;
	}

	protected void adminHelpSection( MessageEvent event ) {
		String[] strs = {
			"Markhov Chain Commands:",
			"    $badword <word> - Add <word> to the 'bad word' list, and purge from the chain data.",};

		for (int i = 0; i < strs.length; ++i) {
			Tim.bot.sendNotice(event.getUser(), strs[i]);
		}
	}

	public void refreshDbLists() {
		getBadwords();
	}

	public void randomActionWrapper( MessageEvent event ) {
		randomAction(event.getChannel(), "say");
	}

	public void randomActionWrapper( ActionEvent event ) {
		randomAction(event.getChannel(), "emote");
	}

	public void randomActionWrapper( ServerPingEvent event, String channel ) {
		randomAction(event.getBot().getChannel(channel), "say");
	}

	protected void randomAction( Channel channel, String type ) {
		String[] actions = {
			"markhov"
		};

		String action = actions[Tim.rand.nextInt(actions.length)];

		if ("markhov".equals(action)) {
			try {
				Thread.sleep(Tim.rand.nextInt(1000) + 500);
				if ("say".equals(type)) {
					Tim.bot.sendMessage(channel, generate_markhov(type));
				} else {
					Tim.bot.sendAction(channel, generate_markhov(type));
				}
			} catch (InterruptedException ex) {
				Logger.getLogger(MarkhovChains.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public void addBadWord( String word, Channel channel ) {
		Connection con;
		if ("".equals(word)) {
			Tim.bot.sendMessage(channel, "I can't add nothing. Please provide the bad word.");
		} else {
			try {
				con = db.pool.getConnection(timeout);

				PreparedStatement s = con.prepareStatement("REPLACE INTO bad_words SET word = ?");
				s.setString(1, word);
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE FROM markhov_say_data WHERE first COLLATE utf8_general_ci REGEXP ? OR second COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*"+ word +"[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*"+ word +"[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE FROM markhov_emote_data WHERE first COLLATE utf8_general_ci REGEXP ? OR second COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*"+ word +"[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*"+ word +"[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				if (badwordPatterns.get(word) == null) {
					badwordPatterns.put(word, Pattern.compile("(?ui)(?:\\W|\\b)" + Pattern.quote(word) + "(?:\\W|\\b)"));
				}

				Tim.bot.sendAction(channel, "quickly goes through his records, and purges all knowledge of that horrible word.");

				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(MarkhovChains.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public String generate_markhov( String type ) {
		Connection con = null;
		String sentence = "";
		try {
			con = db.pool.getConnection(timeout);
			PreparedStatement nextList, getTotal;

			if ("emote".equals(type)) {
				getTotal = con.prepareStatement("SELECT SUM(count) AS total FROM markhov_emote_data WHERE first = ?");
				nextList = con.prepareStatement("SELECT first, second, count FROM markhov_emote_data WHERE first = ? ORDER BY count ASC");
			} else {
				getTotal = con.prepareStatement("SELECT SUM(count) AS total FROM markhov_say_data WHERE first = ?");
				nextList = con.prepareStatement("SELECT first, second, count FROM markhov_say_data WHERE first = ? ORDER BY count ASC");
			}

			getTotal.setString(1, "");
			nextList.setString(1, "");

			ResultSet totalRes = getTotal.executeQuery();
			totalRes.next();
			int total = totalRes.getInt("total");
			int pick = Tim.rand.nextInt(total);

			String lastWord = "";
			int check = 0;

			ResultSet nextRes = nextList.executeQuery();
			while (nextRes.next()) {
				check += nextRes.getInt("count");
				if (check > pick) {
					break;
				}

				sentence = nextRes.getString("second");
				lastWord = nextRes.getString("second");
			}

			int maxLength = Tim.rand.nextInt(25) + 10;
			int curWords = 1;
			boolean keepGoing = true;

			while (keepGoing || curWords < maxLength) {
				keepGoing = true;

				getTotal.setString(1, lastWord);
				nextList.setString(1, lastWord);

				totalRes = getTotal.executeQuery();
				if (totalRes.next()) {
					total = totalRes.getInt("total");
				} else {
					break;
				}

				if (total == 0) {
					break;
				}

				pick = Tim.rand.nextInt(total);

				check = 0;

				nextRes = nextList.executeQuery();
				while (nextRes.next()) {
					check += nextRes.getInt("count");
					if (check > pick) {
						lastWord = nextRes.getString("second");

						if ("".equals(lastWord)) {
							break;
						}

						sentence += " " + nextRes.getString("second");
						break;
					}
				}

				if ("".equals(lastWord)) {
					keepGoing = false;
				}

				curWords++;
			}

			nextRes.close();
			totalRes.close();
			nextList.close();
			getTotal.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		return sentence;
	}

	public void getBadwords() {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `word` FROM `bad_words`");
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			String word;
			
			badwordPatterns.clear();
			while (rs.next()) {
				word = rs.getString("word");

				if (badwordPatterns.get(word) == null) {
					badwordPatterns.put(word, Pattern.compile("(?ui)(?:\\W|\\b)" + Pattern.quote(word) + "(?:\\W|\\b)"));
				}
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Process a message to populate the Markhov data tables
	 *
	 * Takes a message and a type and builds Markhov chain data out of the message, skipping bad words and other things
	 * we don't want to track such as email addresses and URLs.
	 *
	 * @param message What is the message to parse
	 * @param type    What type of message was it (say or emote)
	 *
	 */
	public void process_markhov( String message, String type ) {
		Connection con = null;
		String first;
		String second = "";

		String[] words = message.split(" ");
		try {
			con = db.pool.getConnection(timeout);
			PreparedStatement addPair;
			if ("emote".equals(type)) {
				addPair = con.prepareStatement("INSERT INTO markhov_emote_data (first, second, count) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE count = count + 1");
			} else {
				addPair = con.prepareStatement("INSERT INTO markhov_say_data (first, second, count) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE count = count + 1");
			}

			for (int i = 0; i < ( words.length - 1 ); i++) {
				if (skipMarkhovWord(words[i])) {
					continue;
				}

				if (i == 0) {
					first = "";
					second = words[i];

					addPair.setString(1, first);
					addPair.setString(2, second);

					addPair.executeUpdate();
				}

				if (skipMarkhovWord(words[i + 1])) {
					continue;
				}

				first = words[i];
				second = words[i + 1];

				addPair.setString(1, first);
				addPair.setString(2, second);

				addPair.executeUpdate();
			}

			if (!second.isEmpty()) {
				addPair.setString(1, second);
				addPair.setString(2, "");

				addPair.executeUpdate();
			}

			addPair.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private boolean skipMarkhovWord( String word ) {
		for (Pattern pattern : badwordPatterns.values()) {
			if (pattern.matcher(word).find()) {
				return true;
			}
		}
		
		// If email, skip
		if (Pattern.matches("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$", word)) {
			return true;
		}

		// Checks for URL
		try {
			new URL(word);
			return true;
		} catch (MalformedURLException ex) {
			// NOOP
		}

		// Phone number
		if (Pattern.matches("^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", word)) {
			return true;
		}

		return false;
	}
}
