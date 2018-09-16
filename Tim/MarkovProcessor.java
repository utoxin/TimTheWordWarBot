package Tim;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.pircbotx.Channel;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Pattern;

class MarkovProcessor implements Runnable {
	private HashMap<String, Pattern[]> badpairPatterns = new HashMap<>();
	private ArrayList<String> alternateWords = new ArrayList<>();
	private ArrayList<String[]> alternatePairs = new ArrayList<>();
	private final long timeout = 3000;
	private final UrlValidator urlValidator = new UrlValidator();
	private final EmailValidator emailValidator;

	private HashMap<String, Pattern> badwordPatterns = new HashMap<>();

	MarkovProcessor() {
		this.emailValidator = EmailValidator.getInstance();
	}

	@Override
	public void run() {
		while (true) {
			try {
				processing_loop();
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	void refreshDbLists() {
		getAlternatewords();
		getAlternatepairs();
		getBadwords();
		getBadpairs();
	}

	private void getAlternatewords() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("SELECT `word` FROM `alternate_words`");
			s.executeQuery();

			ResultSet rs = s.getResultSet();

			alternateWords.clear();
			while (rs.next()) {
				alternateWords.add(rs.getString("word"));
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getAlternatepairs() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
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
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getBadwords() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("SELECT `word` FROM `bad_words`");
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			String word;

			badwordPatterns.clear();
			while (rs.next()) {
				word = rs.getString("word");

				badwordPatterns.putIfAbsent(word, Pattern.compile("(?ui)(\\W|\\b)(" + Pattern.quote(word) + ")(\\W|\\b)"));
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getBadpairs() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
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
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void processing_loop() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("SELECT `id`, `type`, `text` FROM markov_processing_queue");
			PreparedStatement deleteQuery = con.prepareStatement("DELETE FROM markov_processing_queue WHERE `id` = ?");

			int id;
			String type, text;

			ResultSet rs = s.executeQuery();
			while(rs.next()) {
				id = rs.getInt("id");
				type = rs.getString("type");
				text = rs.getString("text");

				process_markov(text, type);

				deleteQuery.setInt(1, id);
				deleteQuery.execute();
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	/**
	 * Process a message to populate the Markov data tables
	 *
	 * Takes a message and a type and builds Markov chain data out of the message, skipping bad words and other things
	 * we don't want to track such as email addresses and URLs.
	 *
	 * @param message  What is the message to parse
	 * @param type     What type of message was it (say or emote)
	 *
	 */
	private void process_markov(String message, String type) {
		HashMap<String, String> knownReplacements = new HashMap<>();

		String[] words = message.split(" ");

		for (int i = -1; i <= words.length; i++) {
			String word1, word2, word3;
			String rep1, rep2, rep3;

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

			rep1 = replaceBadWord(word1);
			if (!Objects.equals(rep1, word1)) {
				if (knownReplacements.containsKey(word1)) {
					word1 = knownReplacements.get(word1);
				} else {
					knownReplacements.put(word1, rep1);
					word1 = rep1;
				}
			}

			rep2 = replaceBadWord(word2);
			if (!Objects.equals(rep2, word2)) {
				if (knownReplacements.containsKey(word2)) {
					word1 = knownReplacements.get(word2);
				} else {
					knownReplacements.put(word2, rep2);
					word2 = rep2;
				}
			}

			rep3 = replaceBadWord(word3);
			if (!Objects.equals(rep3, word3)) {
				if (knownReplacements.containsKey(word3)) {
					word3 = knownReplacements.get(word3);
				} else {
					knownReplacements.put(word3, rep3);
					word3 = rep3;
				}
			}

			String[] result;
			result = replaceBadPair(word1, word2);
			if (!Objects.equals(result[0] + " " + result[1], word1 + " " + word2)) {
				if (knownReplacements.containsKey(word1 + " " + word2)) {
					result = knownReplacements.get(word1 + " " + word2).split(" ");
				} else {
					knownReplacements.put(word1 + " " + word2, result[0] + " " + result[1]);
				}

				word1 = result[0];
				word2 = result[1];
			}

			result = replaceBadPair(word2, word3);
			if (!Objects.equals(result[0] + " " + result[1], word2 + " " + word3)) {
				if (knownReplacements.containsKey(word2 + " " + word3)) {
					result = knownReplacements.get(word2 + " " + word3).split(" ");
				} else {
					knownReplacements.put(word2 + " " + word3, result[0] + " " + result[1]);
				}

				word2 = result[0];
				word3 = result[1];
			}

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
		int first = getMarkovWordId(word1);
		int second = getMarkovWordId(word2);
		int third = getMarkovWordId(word3);
		
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
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
			Tim.printStackTrace(ex);
		}
	}

	void storeLine(String type, String text) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement insert = con.prepareStatement("INSERT INTO markov_processing_queue (`type`, `text`, `created`) VALUES (?, ?, NOW())");

			insert.setString(1, type);
			insert.setString(2, text);

			insert.executeUpdate();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	int getMarkovWordId(String word) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
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
			Tim.printStackTrace(ex);
		}

		return 0;
	}

	String getMarkovWordById(int wordID) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement checkword;

			checkword = con.prepareStatement("SELECT word FROM markov_words WHERE id = ?");

			checkword.setInt(1, wordID);
			ResultSet checkRes = checkword.executeQuery();
			if (checkRes.next()) {
				return checkRes.getString("word");
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}

		return "";
	}

	private String[] replaceBadPair(String word_one, String word_two) {
		String[] result = {word_one, word_two};
		
		if (badpairPatterns.size() == 0) return result;
		
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

				return result;
			}
		}

		return result;
	}

	private String replaceBadWord(String word) {
		if (badwordPatterns.size() == 0) return word;
		
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
				break;
			}
		}

		if (urlValidator.isValid(word.replace("^(?ui)[^a-z0-9]*(.*?)[^a-z0-9]*$", "$1"))
			|| emailValidator.isValid(word.replace("^(?ui)[^a-z0-9]*(.*?)[^a-z0-9]*$", "$1"))) {
			word = "http://bit.ly/19VurZW";
		}

		if (Pattern.matches("^\\(?(\\d{3})\\)?[- ]?(\\d{2,3})[- ]?(\\d{4})$", word)) {
			word = "867-5309";
		}

		return word;
	}

	void addBadPair(String word_one, String word_two, Channel channel) {
		if ("".equals(word_one)) {
			channel.send().message("I can't add nothing. Please provide the bad word.");
		} else {
			try (Connection con = Tim.db.pool.getConnection(timeout)) {
				PreparedStatement s = con.prepareStatement("REPLACE INTO bad_pairs SET word_one = ?, word_two = ?");
				s.setString(1, word_one);
				s.setString(2, word_two);
				s.executeUpdate();

				s = con.prepareStatement("DELETE msd.* FROM markov3_say_data msd INNER JOIN markov_words mw1 ON (msd.first_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.second_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*" + word_one + "[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*" + word_two + "[[:punct:]]*$");
				s.executeUpdate();

				s = con.prepareStatement("DELETE msd.* FROM markov3_say_data msd INNER JOIN markov_words mw1 ON (msd.second_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.third_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*" + word_one + "[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*" + word_two + "[[:punct:]]*$");
				s.executeUpdate();

				s = con.prepareStatement("DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.first_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.second_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*" + word_one + "[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*" + word_two + "[[:punct:]]*$");
				s.executeUpdate();

				s = con.prepareStatement("DELETE msd.* FROM markov3_emote_data msd INNER JOIN markov_words mw1 ON (msd.second_id = mw1.id) INNER JOIN markov_words mw2 ON (msd.third_id = mw2.id) WHERE mw1.word COLLATE utf8_general_ci REGEXP ? AND mw2.word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*" + word_one + "[[:punct:]]*$");
				s.setString(2, "^[[:punct:]]*" + word_two + "[[:punct:]]*$");
				s.executeUpdate();

				badpairPatterns.putIfAbsent(word_one + ":" + word_two, new Pattern[]{
					Pattern.compile("(?ui)(\\W|\\b)(" + Pattern.quote(word_one) + ")(\\W|\\b)"),
					Pattern.compile("(?ui)(\\W|\\b)(" + Pattern.quote(word_two) + ")(\\W|\\b)"),});

				channel.send().action("quickly goes through his records, and purges all knowledge of that horrible phrase.");
			} catch (SQLException ex) {
				Tim.printStackTrace(ex);
			}
		}
	}

	void addBadWord(String word, Channel channel) {
		if ("".equals(word)) {
			channel.send().message("I can't add nothing. Please provide the bad word.");
		} else {
			try (Connection con = Tim.db.pool.getConnection(timeout)) {
				PreparedStatement s = con.prepareStatement("REPLACE INTO bad_words SET word = ?");
				s.setString(1, word);
				s.executeUpdate();

				s = con.prepareStatement("DELETE FROM markov_words WHERE word COLLATE utf8_general_ci REGEXP ?");
				s.setString(1, "^[[:punct:]]*" + word + "[[:punct:]]*$");
				s.executeUpdate();

				badwordPatterns.putIfAbsent(word, Pattern.compile("(?ui)(\\W|\\b)(" + Pattern.quote(word) + ")(\\W|\\b)"));

				channel.send().action("quickly goes through his records, and purges all knowledge of that horrible word.");
			} catch (SQLException ex) {
				Tim.printStackTrace(ex);
			}
		}
	}
}
