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
import java.sql.*;
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
	protected HashMap<String, Pattern[]> badpairPatterns = new HashMap<String, Pattern[]>();
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
        } else if (command.equals("badpair")) {
			if (args != null && args.length == 2) {
				addBadPair(args[0], args[1], event.getChannel());
				return true;
			}
		}

		return false;
	}

	protected void adminHelpSection( MessageEvent event ) {
		String[] strs = {
			"Markhov Chain Commands:",
			"    $badword <word> - Add <word> to the 'bad word' list, and purge from the chain data.",
			"    $badpair <word> <word> - Add pair to the 'bad pair' list, and purge from the chain data.",};

		for (int i = 0; i < strs.length; ++i) {
			Tim.bot.sendNotice(event.getUser(), strs[i]);
		}
	}

	public void refreshDbLists() {
		getBadwords();
		getBadpairs();
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

    public void addBadPair( String word_one, String word_two, Channel channel ) {
		Connection con;
		if ("".equals(word_one)) {
			Tim.bot.sendMessage(channel, "I can't add nothing. Please provide the bad word.");
		} else {
			try {
				con = db.pool.getConnection(timeout);

				PreparedStatement s = con.prepareStatement("REPLACE INTO bad_pairs SET word_one = ?, word_two = ?");
				s.setString(1, word_one);
				s.setString(2, word_two);
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE msd.* FROM markov_say_data msd INNER JOIN markov_words mw1 ON (msd.first_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.second_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*"+ word_one +"[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*"+ word_two +"[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE msd.* FROM markov_emote_data msd INNER JOIN markov_words mw1 ON (msd.first_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.second_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*"+ word_one +"[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*"+ word_two +"[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				if (badpairPatterns.get(word_one + ":" + word_two) == null) {
					badpairPatterns.put(word_one + ":" + word_two, new Pattern[] {
                            Pattern.compile("(?ui)(?:\\W|\\b)" + Pattern.quote(word_one) + "(?:\\W|\\b)"),
                            Pattern.compile("(?ui)(?:\\W|\\b)" + Pattern.quote(word_two) + "(?:\\W|\\b)"),
                                    });
				}

				Tim.bot.sendAction(channel, "quickly goes through his records, and purges all knowledge of that horrible phrase.");

				con.close();
			} catch (SQLException ex) {
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

				s = con.prepareStatement("DELETE FROM markov_words WHERE word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*"+ word +"[[:punct:]]*$");
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
				getTotal = con.prepareStatement("SELECT SUM(count) AS total FROM markov_emote_data med INNER JOIN markov_words mw ON (med.first_id = mw.id) WHERE word = ?");
				nextList = con.prepareStatement("SELECT mw1.word first, mw2.word second, count FROM markov_emote_data med INNER JOIN markov_words mw1 ON (med.first_id = mw1.id) INNER JOIN markov_words mw2 ON (med.second_id = mw2.id) WHERE mw1.word = ? ORDER BY count ASC");
			} else {
				getTotal = con.prepareStatement("SELECT SUM(count) AS total FROM markov_say_data med INNER JOIN markov_words mw ON (med.first_id = mw.id) WHERE word = ?");
				nextList = con.prepareStatement("SELECT mw1.word first, mw2.word second, count FROM markov_say_data med INNER JOIN markov_words mw1 ON (med.first_id = mw1.id) INNER JOIN markov_words mw2 ON (med.second_id = mw2.id) WHERE mw1.word = ? ORDER BY count ASC");
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

	public void getBadpairs() {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `word_one`, `word_two` FROM `bad_pairs`");
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			String word_one, word_two;
			
			badwordPatterns.clear();
			while (rs.next()) {
				word_one = rs.getString("word_one");
				word_two = rs.getString("word_two");

				if (badpairPatterns.get(word_one + ":" + word_two) == null) {
					badpairPatterns.put(word_one + ":" + word_two, new Pattern[] {
                            Pattern.compile("(?ui)(?:\\W|\\b)" + Pattern.quote(word_one) + "(?:\\W|\\b)"),
                            Pattern.compile("(?ui)(?:\\W|\\b)" + Pattern.quote(word_two) + "(?:\\W|\\b)"),
                                    });
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
		int first;
		int second = getMarkovWordId("");

		String[] words = message.split(" ");
		try {
			con = db.pool.getConnection(timeout);
			PreparedStatement addPair;

			if ("emote".equals(type)) {
				addPair = con.prepareStatement("INSERT INTO markov_emote_data (first_id, second_id, count) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE count = count + 1");
			} else {
				addPair = con.prepareStatement("INSERT INTO markov_say_data (first_id, second_id, count) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE count = count + 1");
			}

			for (int i = 0; i < ( words.length - 1 ); i++) {
				if (skipMarkhovWord(words[i])) {
					continue;
				}

				if (i == 0) {
					first = getMarkovWordId("");
					second = getMarkovWordId(words[i]);

					addPair.setInt(1, first);
					addPair.setInt(2, second);

					addPair.executeUpdate();
				}

				if (skipMarkhovWord(words[i + 1])) {
					continue;
				}

                if (skipMarkovPair(words[i], words[i + 1])) {
                    continue;
                }
                    
				first = getMarkovWordId(words[i]);
				second = getMarkovWordId(words[i + 1]);

				addPair.setInt(1, first);
				addPair.setInt(2, second);

				addPair.executeUpdate();
			}

			if (second != getMarkovWordId("")) {
				addPair.setInt(1, second);
				addPair.setInt(2, getMarkovWordId(""));

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
	
	private int getMarkovWordId( String word ) {
		Connection con = null;

		try {
			con = db.pool.getConnection(timeout);
			PreparedStatement checkword, addword;

			checkword = con.prepareStatement("SELECT id FROM markov_words WHERE word = ?");
			addword = con.prepareStatement("INSERT INTO markov_words SET word = ?", Statement.RETURN_GENERATED_KEYS);

			checkword.setString(1, word);
			ResultSet checkRes = checkword.executeQuery();
			if (checkRes.next()) {
				return checkRes.getInt("id");
			} else {
				addword.setString(1, word);
				addword.executeUpdate();
				ResultSet rs = addword.getGeneratedKeys();

				if (rs.next()) {
					return rs.getInt(1);
				}
			}
		} catch (SQLException ex) {
			Logger.getLogger(MarkhovChains.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		return 0;
	}

    private boolean skipMarkovPair( String word_one, String word_two ) {
        for (Pattern[] patterns : badpairPatterns.values()) {
            if (patterns[0].matcher(word_one).find() && patterns[1].matcher(word_two).find()) {
                return true;
            }
        }
        
        return false;
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
