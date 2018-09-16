package Tim;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Colors;
import org.pircbotx.hooks.events.MessageEvent;

class MarkovChains {
	private final long timeout = 3000;
	private final String[] sentenceEndings = {".", ".", ".", ".", "!", "!", "?", "?", "!", "!", "?", "?", "...", "?!", "...!", "...?"};

	HashMap<String, Pattern> badwordPatterns = new HashMap<>();

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
					Tim.markovProcessor.addBadWord(args[0], event.getChannel());
					return true;
				}
				break;
			case "badpair":
				if (args != null && args.length == 2) {
					Tim.markovProcessor.addBadPair(args[0], args[1], event.getChannel());
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
			if (event.getUser() != null) {
				event.getUser().send().message(str);
			}
		}
	}

	void randomAction(String channel, String type, String message) {
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
			Tim.printStackTrace(ex);
		}
	}

	String generate_markov() {
		return generate_markov("say", Tim.rand.nextInt(25) + 10, 0);
	}

	String generate_markov(String type, String message) {
		int seedWord = 0;

		if (!message.equals("")) {
			seedWord = getSeedWord(message, type, 0);
		}

		return generate_markov(type, Tim.rand.nextInt(25) + 10, seedWord);
	}

	String generate_markov(String type, int maxLength, int seedWord) {
		String sentence = "";
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
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
					nextSentence = Tim.markovProcessor.getMarkovWordById(seedWord) + " " + nextSentence;
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
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}

		return sentence;
	}

	private int getSeedWord(String message, String type, int lastSeed) {
		String[] words = message.split(" ");
		HashSet<Integer> wordIds = new HashSet<>();

		for (String word : words) {
			wordIds.add(Tim.markovProcessor.getMarkovWordId(word));
		}
		
		wordIds.remove(lastSeed);
		if (wordIds.isEmpty()) {
			return 0;
		}
		
		String ids = StringUtils.join(wordIds, ",");
		
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
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
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
		
		return 0;
	}
}