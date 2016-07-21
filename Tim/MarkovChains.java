package Tim;

/*
 * Copyright (C) 2015 mwalker
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author mwalker
 */
class MarkovChains {

	private final DBAccess db = DBAccess.getInstance();
	private HashMap<String, Pattern[]> badpairPatterns = new HashMap<>();
	private ArrayList<String> alternateWords = new ArrayList<>(64);
	private ArrayList<String[]> alternatePairs = new ArrayList<>(64);
	private final long timeout = 3000;
	private final UrlValidator urlValidator = new UrlValidator();
	private final EmailValidator emailValidator;
	private final String[] sentenceEndings = {".", ".", ".", ".", "!", "!", "?", "?", "!", "!", "?", "?", "...", "?!", "...!", "...?"};

	HashMap<String, Pattern> badwordPatterns = new HashMap<>();

	MarkovChains() {
		this.emailValidator = EmailValidator.getInstance();
	}

	/**
	 * Parses admin-level commands passed from the main class. Returns true if the message was handled, false if it was
	 * not.
	 *
	 * @param event Event to process
	 *
	 * @return True if message was handled, false otherwise
	 */
	boolean parseAdminCommand(MessageEvent event) {
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
				}
				break;
			case "badpair":
				if (args != null && args.length == 2) {
					addBadPair(args[0], args[1], event.getChannel());
					return true;
				}
				break;
		}

		return false;
	}

	void adminHelpSection(MessageEvent event) {
		String[] strs = {
			"Markhov Chain Commands:",
			"    $badword <word> - Add <word> to the 'bad word' list, and purge from the chain data.",
			"    $badpair <word> <word> - Add pair to the 'bad pair' list, and purge from the chain data.",};

		for (String str : strs) {
			event.getUser().send().notice(str);
		}
	}

	void refreshDbLists() {
		getAlternatewords();
		getAlternatepairs();
		getBadwords();
		getBadpairs();
	}

	void randomAction(String channel, String type, String message) {
		String[] actions = {
			"markhov"
		};

		String action = actions[Tim.rand.nextInt(actions.length)];

		if ("markhov".equals(action)) {
			try {
				Thread.sleep(Tim.rand.nextInt(1000) + 500);
				switch (type) {
					case "say":
						Tim.bot.sendIRC().message(channel, generate_markov(type, message));
						break;
					case "mutter":
						Tim.bot.sendIRC().action(channel, "mutters under his breath, \"" + generate_markov("say", message) + "\"");
						break;
					default:
						Tim.bot.sendIRC().action(channel, generate_markov(type, message));
						break;
				}
			} catch (InterruptedException ex) {
				Logger.getLogger(MarkovChains.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private void addBadPair(String word_one, String word_two, Channel channel) {
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
				s.setString(1, "^[[:punct:]]*" + word_one + "[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*" + word_two + "[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE msd.* FROM markov3_say_data msd INNER JOIN markov_words mw1 ON (msd.second_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.third_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*" + word_one + "[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*" + word_two + "[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.first_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.second_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*" + word_one + "[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*" + word_two + "[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				s = con.prepareStatement("DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.second_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.third_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*" + word_one + "[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*" + word_two + "[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				badpairPatterns.putIfAbsent(word_one + ":" + word_two, new Pattern[]{
					Pattern.compile("(?ui)(\\W|\\b)(" + Pattern.quote(word_one) + ")(\\W|\\b)"),
					Pattern.compile("(?ui)(\\W|\\b)(" + Pattern.quote(word_two) + ")(\\W|\\b)"),});

				channel.send().action("quickly goes through his records, and purges all knowledge of that horrible phrase.");

				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(MarkovChains.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private void addBadWord(String word, Channel channel) {
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
				s.setString(1, "^[[:punct:]]*" + word + "[[:punct:]]*$");
				s.executeUpdate();
				s.close();

				badwordPatterns.putIfAbsent(word, Pattern.compile("(?ui)(\\W|\\b)(" + Pattern.quote(word) + ")(\\W|\\b)"));

				channel.send().action("quickly goes through his records, and purges all knowledge of that horrible word.");

				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(MarkovChains.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	String generate_markov(String type) {
		return generate_markov(type, Tim.rand.nextInt(25) + 10, 0);
	}

	String generate_markov(String type, String message) {
		int seedWord = 0;

		if (!message.equals("")) {
			seedWord = getSeedWord(message, type, 0);
		}

		return generate_markov(type, Tim.rand.nextInt(25) + 10, seedWord);
	}

	String generate_markov(String type, int maxLength, int seedWord) {
		Connection con = null;
		String sentence = "";
		try {
			con = db.pool.getConnection(timeout);
			CallableStatement nextSentenceStmt;

			if ("emote".equals(type)) {
				nextSentenceStmt = con.prepareCall("CALL generateMarkovEmote(?, ?)");
			} else {
				nextSentenceStmt = con.prepareCall("CALL generateMarkovSay(?, ?)");
			}

			nextSentenceStmt.registerOutParameter(2, java.sql.Types.LONGVARCHAR);

			int curWords = 0;

			while (curWords < maxLength) {
				nextSentenceStmt.setInt(1, seedWord);
				nextSentenceStmt.executeUpdate();
				String nextSentence = nextSentenceStmt.getString(2);
				
				if (seedWord > 0) {
					nextSentence = getMarkovWordById(seedWord) + " " + nextSentence;
				}
				
				if (nextSentence.split(" ").length >= 5) {
					seedWord = getSeedWord(nextSentence, type, seedWord);
				} else {
					seedWord = 0;
				}

				if ("emote".equals(type)) {
					if (curWords > 0) {
						if (Tim.rand.nextInt(100) > 75) {
							nextSentence = Tim.bot.getNick() + " " + nextSentence;
						} else if (Tim.rand.nextInt(100) > 50) {
							nextSentence = "He " + nextSentence;
						} else {
							nextSentence = "It " + nextSentence;
						}
					}
				} else {
					nextSentence = StringUtils.capitalize(nextSentence);
				}
				
				if (!"".equals(sentence)) {
					nextSentence = " " + nextSentence;
				}

				if (!nextSentence.matches("[.?!\"']+$")) {
					String ending = ".";
					if (Tim.rand.nextInt(100) > 65) {
						ending = sentenceEndings[Tim.rand.nextInt(sentenceEndings.length)];
					}

					nextSentence = nextSentence.replaceFirst("[.?!:;/\"'-]*$", ending);
				}

				curWords += nextSentence.trim().split("\\s+").length;
				sentence += nextSentence;

				// Odds of ending early = Percentage of Max divided by 4
				if (Tim.rand.nextInt(100) < ( (1-((maxLength - curWords) / maxLength)) * 25) ) {
					break;
				}
			}

			nextSentenceStmt.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				if (con != null) {
					con.close();
				}
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		return sentence;
	}

	private int getSeedWord(String message, String type, int lastSeed) {
		String[] words = message.split(" ");
		HashSet<Integer> wordIds = new HashSet<>();
		Connection con = null;

		for (String word : words) {
			wordIds.add(getMarkovWordId(word));
		}
		
		wordIds.remove(lastSeed);
		if (wordIds.isEmpty()) {
			return 0;
		}
		
		String ids = StringUtils.join(wordIds, ",");

		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s;
			if (type.equals("say")) {
				s = con.prepareStatement("SELECT * FROM (SELECT second_id FROM markov3_say_data msd WHERE msd.first_id = 1 AND msd.third_id != 1 AND msd.second_id IN ("+ids+") "
					+ "GROUP BY msd.second_id ORDER BY sum(msd.count) ASC LIMIT ?) derived ORDER BY RAND() LIMIT 1");
			} else {
				s = con.prepareStatement("SELECT * FROM (SELECT second_id FROM markov3_emote_data msd WHERE msd.first_id = 1 AND msd.third_id != 1 AND msd.second_id IN ("+ids+") "
					+ "GROUP BY msd.second_id ORDER BY sum(msd.count) ASC LIMIT ?) derived ORDER BY RAND() LIMIT 1");
			}

			int innerLimit = 2;
			if ((words.length / 4) > 2) {
				innerLimit = words.length / 4;
			}
			
			s.setInt(1, innerLimit);
			s.executeQuery();

			try (ResultSet rs = s.getResultSet()) {
				if (rs.next()) {
					int id = rs.getInt(1);
					rs.close();
					s.close();
					con.close();
					return id;
				}
			}

			s.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				if (con != null) {
					con.close();
				}
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		
		return 0;
	}
	
	private void getAlternatewords() {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `word` FROM `alternate_words`");
			s.executeQuery();

			ResultSet rs = s.getResultSet();

			alternateWords.clear();
			while (rs.next()) {
				alternateWords.add(rs.getString("word"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getAlternatepairs() {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `word_one`, `word_two` FROM `bad_pairs`");
			s.executeQuery();

			ResultSet rs = s.getResultSet();

			alternatePairs.clear();
			while (rs.next()) {
				alternatePairs.add(new String[]{
					rs.getString("word_one"),
					rs.getString("word_two")
				});
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getBadwords() {
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

				badwordPatterns.putIfAbsent(word, Pattern.compile("(?ui)(\\W|\\b)(" + Pattern.quote(word) + ")(\\W|\\b)"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(DBAccess.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getBadpairs() {
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

				badpairPatterns.putIfAbsent(word_one + ":" + word_two, new Pattern[]{
					Pattern.compile("(?ui)(\\W|\\b)(" + Pattern.quote(word_one) + ")(\\W|\\b)"),
					Pattern.compile("(?ui)(\\W|\\b)(" + Pattern.quote(word_two) + ")(\\W|\\b)"),});
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
	 * @param message  What is the message to parse
	 * @param type     What type of message was it (say or emote)
	 * @param username The username it came from
	 *
	 */
	void process_markov(String message, String type, String username) {
		String[] words = message.split(" ");

		for (int i = -1; i <= words.length; i++) {
			String word1, word2, word3;

			int offset1 = i - 1;
			int offset3 = i + 1;

			if (offset1 < 0) {
				word1 = "";
			} else if (offset1 >= words.length) {
				word1 = "";
			} else {
				word1 = words[offset1];
			}

			if (i < 0) {
				word2 = "";
			} else if (i >= words.length) {
				word2 = "";
			} else {
				word2 = words[i];
			}

			if (offset3 < 0) {
				word3 = "";
			} else if (offset3 >= words.length) {
				word3 = "";
			} else {
				word3 = words[offset3];
			}

			word1 = replaceBadWord(word1);
			word2 = replaceBadWord(word2);
			word3 = replaceBadWord(word3);

			String[] result;
			result = replaceBadPair(word1, word2);
			word1 = result[0];
			word2 = result[1];

			result = replaceBadPair(word2, word3);
			word2 = result[0];
			word3 = result[1];

			word1 = word1.replaceAll(Tim.bot.getNick(), StringUtils.capitalize(username));
			word2 = word2.replaceAll(Tim.bot.getNick(), StringUtils.capitalize(username));
			word3 = word3.replaceAll(Tim.bot.getNick(), StringUtils.capitalize(username));

			word1 = word1.substring(0, Math.min(word1.length(), 49));
			word2 = word2.substring(0, Math.min(word2.length(), 49));
			word3 = word3.substring(0, Math.min(word3.length(), 49));

			if (offset1 >= 0 && offset1 < words.length) {
				words[offset1] = word1;
			}

			if (i >= 0 && i < words.length) {
				words[i] = word2;
			}

			if (offset3 >= 0 && offset3 < words.length) {
				words[offset3] = word3;
			}

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
				if (con != null) {
					con.close();
				}
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private int getMarkovWordId(String word) {
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
				if (con != null) {
					con.close();
				}
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		return 0;
	}

	private String getMarkovWordById(int wordID) {
		Connection con = null;

		try {
			con = db.pool.getConnection(timeout);
			PreparedStatement checkword;

			checkword = con.prepareStatement("SELECT word FROM markov_words WHERE id = ?");

			checkword.setInt(1, wordID);
			ResultSet checkRes = checkword.executeQuery();
			if (checkRes.next()) {
				return checkRes.getString("word");
			}
		} catch (SQLException ex) {
			Logger.getLogger(MarkovChains.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				if (con != null) {
					con.close();
				}
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		return "";
	}

	private String[] replaceBadPair(String word_one, String word_two) {
		String[] result = {word_one, word_two};
		String[] alternate_pair = alternatePairs.get(Tim.rand.nextInt(alternatePairs.size()));
		String working1, working2;
		working1 = word_one.replaceAll("[^a-zA-Z]", "");
		working2 = word_two.replaceAll("[^a-zA-Z]", "");

		if (working1.matches("^[A-Z]+$")) {
			alternate_pair[0] = alternate_pair[0].toUpperCase();
		} else if (working1.matches("^[A-Z]+[a-z]+")) {
			alternate_pair[0] = StringUtils.capitalize(alternate_pair[0]);
		}

		if (working2.matches("^[A-Z]+$")) {
			alternate_pair[1] = alternate_pair[1].toUpperCase();
		} else if (working2.matches("^[A-Z]+[a-z]+")) {
			alternate_pair[1] = StringUtils.capitalize(alternate_pair[1]);
		}

		for (Pattern[] patterns : badpairPatterns.values()) {
			if (patterns[0].matcher(word_one).find() && patterns[1].matcher(word_two).find()) {
				result[0] = patterns[0].matcher(word_one).replaceAll("$1" + alternate_pair[0] + "$3");
				result[1] = patterns[1].matcher(word_two).replaceAll("$1" + alternate_pair[1] + "$3");

				Logger.getLogger(DBAccess.class.getName()).log(Level.INFO, "Replaced pair ''{0} {1}'' with ''{2} {3}''", new Object[]{word_one, word_two, result[0], result[1]});

				return result;
			}
		}

		return result;
	}

	private String replaceBadWord(String word) {
		String old_word = word, working, replacement = alternateWords.get(Tim.rand.nextInt(alternateWords.size()));
		working = word.replaceAll("[^a-zA-Z]", "");

		if (working.matches("^[A-Z]+$")) {
			replacement = replacement.toUpperCase();
		} else if (working.matches("^[A-Z]+[a-z]+")) {
			replacement = StringUtils.capitalize(replacement);
		}

		for (Pattern pattern : badwordPatterns.values()) {
			word = pattern.matcher(word).replaceAll("$1" + replacement + "$3");
			if (!old_word.equals(word)) {
				Logger.getLogger(DBAccess.class.getName()).log(Level.INFO, "Replaced string ''{0}'' with ''{1}''", new Object[]{old_word, word});
				break;
			}
		}

		if (urlValidator.isValid(word.replace("^(?ui)[^a-z0-9]*(.*?)[^a-z0-9]*$", "$1"))
			|| emailValidator.isValid(word.replace("^(?ui)[^a-z0-9]*(.*?)[^a-z0-9]*$", "$1"))) {
			word = "http://bit.ly/19VurZW";
			if (!old_word.equals(word)) {
				Logger.getLogger(DBAccess.class.getName()).log(Level.INFO, "Replaced string ''{0}'' with ''{1}''", new Object[]{old_word, word});
			}
		}

		if (Pattern.matches("^\\(?(\\d{3})\\)?[- ]?(\\d{2,3})[- ]?(\\d{4})$", word)) {
			word = "867-5309";
			if (!old_word.equals(word)) {
				Logger.getLogger(DBAccess.class.getName()).log(Level.INFO, "Replaced string ''{0}'' with ''{1}''", new Object[]{old_word, word});
			}
		}

		return word;
	}
}