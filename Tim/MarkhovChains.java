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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author mwalker
 */
public class MarkhovChains {
	Tim ircclient;
	private long timeout = 3000;

	public MarkhovChains( Tim ircclient ) {
		this.ircclient = ircclient;
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
	public boolean parseAdminCommand( String channel, String sender, String message ) {
		return false;
	}

	public void refreshDbLists() {
	}

	protected void randomAction( String sender, String channel, String message, String type ) {
		String[] actions = {
			"markhov"
		};

		String action = actions[ircclient.rand.nextInt(actions.length)];
		
		if ("markhov".equals(action)) {
			ircclient.sendDelayedMessage(channel, generate_markhov(type), ircclient.rand.nextInt(1500));
		}
	}

	public String generate_markhov( String type ) {
		Connection con = null;
		String sentence = "";
		try {
			con = ircclient.pool.getConnection(timeout);
			PreparedStatement nextList, getTotal;

			if ("emote".equals(type)) {
				getTotal = con.prepareStatement("SELECT SUM(count) AS total FROM markhov_emote_data WHERE first = ? GROUP BY first");
				nextList = con.prepareStatement("SELECT * FROM markhov_emote_data WHERE first = ? ORDER BY count ASC");
			} else {
				getTotal = con.prepareStatement("SELECT SUM(count) AS total FROM markhov_say_data WHERE first = ? GROUP BY first");
				nextList = con.prepareStatement("SELECT first, second, count FROM markhov_say_data WHERE first = ? ORDER BY count ASC");
			}

			getTotal.setString(1, "");
			nextList.setString(1, "");

			ResultSet totalRes = getTotal.executeQuery();
			totalRes.next();
			int total = totalRes.getInt("total");
			int pick = ircclient.rand.nextInt(total);

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

			int maxLength = ircclient.rand.nextInt(25) + 10;
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

				pick = ircclient.rand.nextInt(total);

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
			con = ircclient.pool.getConnection(timeout);
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
		Connection con = null;
		boolean foundBadWord = false;
		try {
			con = ircclient.pool.getConnection(timeout);
			PreparedStatement checkBad = con.prepareStatement("SELECT count(*) as matched FROM bad_words WHERE word LIKE ?");
			ResultSet checkBadRes;

			checkBad.setString(1, word);
			checkBadRes = checkBad.executeQuery();
			checkBadRes.next();

			if (checkBadRes.getInt("matched") > 0) {
				foundBadWord = true;
			}

			checkBadRes.close();
			checkBad.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}

			if (foundBadWord) {
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