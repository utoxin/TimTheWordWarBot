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
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author mwalker
 */
public class MarkovChains {
	private final DBAccess db = DBAccess.getInstance();
	protected HashMap<String, Pattern> badwordPatterns = new HashMap<>();
	protected HashMap<String, Pattern[]> badpairPatterns = new HashMap<>();
	private final long timeout = 3000;

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
	 * @param event
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

		switch (command) {
			case "badword":
				if (args != null && args.length == 1) {
					addBadWord(args[0], event.getChannel());
					return true;
				}	break;
			case "badpair":
				if (args != null && args.length == 2) {
					addBadPair(args[0], args[1], event.getChannel());
					return true;
				}	break;
		}

		return false;
	}

	protected void adminHelpSection( MessageEvent event ) {
		String[] strs = {
			"Markhov Chain Commands:",
			"    $badword <word> - Add <word> to the 'bad word' list, and purge from the chain data.",
			"    $badpair <word> <word> - Add pair to the 'bad pair' list, and purge from the chain data.",};

		for (int i = 0; i < strs.length; ++i) {
			event.getUser().send().notice(strs[i]);
		}
	}

	public void refreshDbLists() {
		getBadwords();
		getBadpairs();
	}

	public void randomActionWrapper( MessageEvent event ) {
		randomAction(event.getChannel().getName(), Tim.rand.nextBoolean() ? "say" : "mutter");
	}

	public void randomActionWrapper( ActionEvent event ) {
		randomAction(event.getChannel().getName(), Tim.rand.nextBoolean() ? "mutter" : "emote");
	}

	protected void randomAction( String channel, String type ) {
		String[] actions = {
			"markhov"
		};

		String action = actions[Tim.rand.nextInt(actions.length)];

		if ("markhov".equals(action)) {
			try {
				Thread.sleep(Tim.rand.nextInt(1000) + 500);
				switch (type) {
					case "say":
						Tim.bot.sendIRC().message(channel, generate_markov(type));
						break;
					case "mutter":
						Tim.bot.sendIRC().action(channel, "mutters under his breath, \"" + generate_markov("say") + "\"");
						break;
					default:
						Tim.bot.sendIRC().action(channel, generate_markov(type));
						break;
				}
			} catch (InterruptedException ex) {
				Logger.getLogger(MarkovChains.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

    public void addBadPair( String word_one, String word_two, Channel channel ) {
		Connection con;
		if ("".equals(word_one)) {
			channel.send().message("I can't add nothing. Please provide the bad word.");
		} else {
			try {
				con = db.pool.getConnection(timeout);

				PreparedStatement s = con.prepareStatement("REPLACE INTO bad_pairs SET word_one = ?, word_two = ?");
				s.setString(1, word_one);
				s.setString(2, word_two);
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE msd.* FROM markov3_say_data msd INNER JOIN markov_words mw1 ON (msd.first_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.second_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*"+ word_one +"[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*"+ word_two +"[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE msd.* FROM markov3_say_data msd INNER JOIN markov_words mw1 ON (msd.second_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.third_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*"+ word_one +"[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*"+ word_two +"[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.first_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.second_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*"+ word_one +"[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*"+ word_two +"[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.second_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.third_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
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

				channel.send().action("quickly goes through his records, and purges all knowledge of that horrible phrase.");

				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(MarkovChains.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
    }
        
	public void addBadWord( String word, Channel channel ) {
		Connection con;
		if ("".equals(word)) {
			channel.send().message("I can't add nothing. Please provide the bad word.");
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

				channel.send().action("quickly goes through his records, and purges all knowledge of that horrible word.");

				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(MarkovChains.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public String generate_markov( String type ) {
		return generate_markov(type, Tim.rand.nextInt(25) + 10);
	}
	
	public String generate_markov( String type, int maxLength ) {
		Connection con = null;
		String sentence = "";
		try {
			con = db.pool.getConnection(timeout);
			PreparedStatement nextList, getTotal, nextWord;

			if ("emote".equals(type)) {
				getTotal = con.prepareStatement("SELECT SUM(count) AS total FROM markov3_emote_data WHERE first_id = ? AND second_id = ?");
				nextWord = con.prepareStatement("SELECT markov_words.word, rt FROM (SELECT first_id, second_id, third_id, (@runtot := @runtot + count) AS rt FROM (SELECT * FROM `markov3_emote_data` WHERE first_id = ? AND second_id = ?) derived, (SELECT @runtot := 0) r) derived2 INNER JOIN markov_words ON (derived2.third_id = markov_words.id) HAVING rt > ? LIMIT 1");
			} else {
				getTotal = con.prepareStatement("SELECT SUM(count) AS total FROM markov3_say_data WHERE first_id = ? AND second_id = ?");
				nextWord = con.prepareStatement("SELECT markov_words.word, rt FROM (SELECT first_id, second_id, third_id, (@runtot := @runtot + count) AS rt FROM (SELECT * FROM `markov3_say_data` WHERE first_id = ? AND second_id = ?) derived, (SELECT @runtot := 0) r) derived2 INNER JOIN markov_words ON (derived2.third_id = markov_words.id) HAVING rt > ? LIMIT 1");
			}

			int first = getMarkovWordId("");
			int second = first;
			
			getTotal.setInt(1, first);
			getTotal.setInt(2, second);

			ResultSet totalRes = getTotal.executeQuery();
			totalRes.next();
			int total = totalRes.getInt("total");
			int pick = Tim.rand.nextInt(total);

			String lastWord;

			nextWord.setInt(1, first);
			nextWord.setInt(2, second);
			nextWord.setInt(3, pick);

			ResultSet nextRes = nextWord.executeQuery();
			while (nextRes.next()) {
				sentence = nextRes.getString("word");
				second = getMarkovWordId(nextRes.getString("word"));
				break;
			}

			int curWords = 1;
			boolean keepGoing = true;

			while (keepGoing || curWords < maxLength) {
				keepGoing = true;

				getTotal.setInt(1, first);
				getTotal.setInt(2, second);

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

				nextWord.setInt(1, first);
				nextWord.setInt(2, second);
				nextWord.setInt(3, pick);

				nextRes = nextWord.executeQuery();
				while (nextRes.next()) {
					first = second;
					second = getMarkovWordId(nextRes.getString("word"));
					lastWord = nextRes.getString("word");

					if ("".equals(lastWord)) {
						break;
					}

					sentence += " " + nextRes.getString("word");
					break;
				}

				curWords++;

				if (Tim.rand.nextInt(100) < ((curWords - maxLength) * 10)) {
					break;
				}
			}

			nextRes.close();
			totalRes.close();
			nextWord.close();
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
			
			badpairPatterns.clear();
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
	 * @param username
	 *
	 */
	public void process_markov( String message, String type, String username ) {
		String[] words = message.split(" ");

		for (int i = -1; i <= words.length; i++) {
			String word1, word2, word3;
			
			int offset1 = i - 1;
			int offset2 = i;
			int offset3 = i + 1;

			if (offset1 < 0) {
				word1 = "";
			} else if (offset1 >= words.length) {
				word1 = "";
			} else {
				if (skipMarkovWord(words[offset1])) {
					continue;
				}
				word1 = words[offset1];
			}

			if (offset2 < 0) {
				word2 = "";
			} else if (offset2 >= words.length) {
				word2 = "";
			} else {
				if (skipMarkovWord(words[offset2])) {
					continue;
				}
				word2 = words[offset2];
			}

			if (offset3 < 0) {
				word3 = "";
			} else if (offset3 >= words.length) {
				word3 = "";
			} else {
				if (skipMarkovWord(words[offset3])) {
					continue;
				}
				word3 = words[offset3];
			}

			if (skipMarkovPair(word1, word2) || skipMarkovPair(word2, word3)) {
				continue;
			}

			word1 = word1.replaceAll(Tim.bot.getNick(), username);
			word2 = word2.replaceAll(Tim.bot.getNick(), username);
			word3 = word3.replaceAll(Tim.bot.getNick(), username);
			
			storeTriad(word1, word2, word3, type);
		}
	}
	
	private void storeTriad(String word1, String word2, String word3, String type) {
		Connection con = null;
		int first = getMarkovWordId(word1);
		int second = getMarkovWordId(word2);
		int third = getMarkovWordId(word3);

		try {
			con = db.pool.getConnection(timeout);
			PreparedStatement addTriad;

			if ("emote".equals(type)) {
				addTriad = con.prepareStatement("INSERT INTO markov3_emote_data (first_id, second_id, third_id, count) VALUES (?, ?, ?, 1) ON DUPLICATE KEY UPDATE count = count + 1");
			} else {
				addTriad = con.prepareStatement("INSERT INTO markov3_say_data (first_id, second_id, third_id, count) VALUES (?, ?, ?, 1) ON DUPLICATE KEY UPDATE count = count + 1");
			}

			addTriad.setInt(1, first);
			addTriad.setInt(2, second);
			addTriad.setInt(3, third);
			
			addTriad.executeUpdate();
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
			Logger.getLogger(MarkovChains.class.getName()).log(Level.SEVERE, null, ex);
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
    
	private boolean skipMarkovWord( String word ) {
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
			URL url = new URL(word);
			return true;
		} catch (MalformedURLException ex) {
			// NOOP
		}

		return Pattern.matches("^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", word);
	}
}
