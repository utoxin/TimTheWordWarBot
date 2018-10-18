package Tim;

import Tim.Commands.ICommandHandler;
import Tim.Data.ChannelInfo;
import Tim.Data.CommandData;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.hooks.types.GenericUserEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

class ChainStory implements ICommandHandler {
	private final long timeout = 3000;

	public boolean handleCommand(CommandData commandData) {
		ChannelInfo cdata = Tim.db.channel_data.get(commandData.getChannelEvent()
			.getChannel()
			.getName()
			.toLowerCase());
		Calendar cal = Calendar.getInstance();

		boolean isNovember = (Calendar.NOVEMBER == cal.get(Calendar.MONTH));

		switch (commandData.command) {
			case "chainlast":
				if (cdata.commands_enabled.get("chainstory")) {
					showLast(commandData.getUserEvent());
				} else {
					commandData.event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "chainnew":
				if (cdata.commands_enabled.get("chainstory")) {
					if (!isNovember) {
						commandData.getMessageEvent()
							.respond("Sorry, that command won't work outside November!");
						return true;
					}
					addNew(commandData);
				} else {
					commandData.getMessageEvent()
						.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "chaininfo":
				if (cdata.commands_enabled.get("chainstory")) {
					info(commandData.getMessageEvent());
				} else {
					commandData.getMessageEvent()
						.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "chaincount":
				if (cdata.commands_enabled.get("chainstory")) {
					count(commandData.getMessageEvent());
				} else {
					commandData.getMessageEvent()
						.respond("I'm sorry. I don't do that here.");
				}

				return true;
		}

		return false;
	}

	private void showLast(GenericUserEvent event) {
		if (event.getUser() == null) {
			return;
		}

		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT * FROM `story` ORDER BY id DESC LIMIT 3");
			s.executeQuery();

			event.respond("I sent you the last three paragraphs in a private message... They're too awesome for everyone to see!");

			ResultSet rs = s.getResultSet();
			rs.setFetchDirection(ResultSet.FETCH_REVERSE);
			rs.last();
			do {
				event.getUser()
					.send()
					.message(rs.getString("string"));
			} while (rs.previous());

			con.close();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void addNew(CommandData commandData) {
		if (commandData.getUserEvent()
			.getUser() == null) {
			return;
		}

		DecimalFormat formatter = new DecimalFormat("#,###");
		Connection con;

		String message = commandData.argString.replaceAll("^<?(.*)>?$", "$1");

		if ("".equals(message)) {
			commandData.getChannelEvent()
				.getChannel()
				.send()
				.action("blinks, and looks confused. \"But there's nothing there. That won't help my wordcount!\"");
		} else {
			try {
				con = Tim.db.pool.getConnection(timeout);

				storeLine(message, commandData.getMessageEvent()
					.getUser()
					.getNick());
				commandData.getChannelEvent()
					.getChannel()
					.send()
					.action("quickly copies down what " + commandData.getUserEvent()
						.getUser()
						.getNick() + " said. \"Thanks!\"");

				PreparedStatement s = con.prepareStatement(
					"SELECT SUM( LENGTH( STRING ) - LENGTH( REPLACE( STRING ,  ' ',  '' ) ) +1 ) AS word_count FROM story");
				s.executeQuery();
				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					commandData.getMessageEvent()
						.respond("My novel is now " + formatter.format(Integer.parseInt(rs.getString("word_count"))) + " words long!");
				}

				con.close();
			} catch (SQLException ex) {
				Tim.printStackTrace(ex);
			}
		}
	}

	String getLastLines() {
		Connection con;
		ResultSet rs;
		PreparedStatement s;
		StringBuilder response = new StringBuilder();

		try {
			con = Tim.db.pool.getConnection(timeout);

			s = con.prepareStatement("SELECT * FROM `story` ORDER BY id DESC LIMIT 3");
			s.executeQuery();

			rs = s.getResultSet();
			rs.setFetchDirection(ResultSet.FETCH_REVERSE);
			rs.last();
			do {
				response.append(rs.getString("string"))
					.append(" ");
			} while (rs.previous());

			con.close();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}

		return response.toString();
	}

	private void info(GenericMessageEvent event) {
		if (event.getUser() == null) {
			return;
		}

		DecimalFormat formatter = new DecimalFormat("#,###");
		Connection con;
		int word_count = 0, author_count = 0;
		ResultSet rs;
		PreparedStatement s;

		try {
			con = Tim.db.pool.getConnection(timeout);

			s = con.prepareStatement("SELECT SUM( LENGTH( STRING ) - LENGTH( REPLACE( STRING ,  ' ',  '' ) ) +1 ) AS word_count FROM story");
			s.executeQuery();
			rs = s.getResultSet();
			while (rs.next()) {
				word_count = Integer.parseInt(rs.getString("word_count"));
			}

			s = con.prepareStatement("SELECT COUNT(DISTINCT author) AS author_count FROM story");
			s.executeQuery();
			rs = s.getResultSet();
			while (rs.next()) {
				author_count = Integer.parseInt(rs.getString("author_count"));
			}

			event.respond("My novel is currently " + formatter.format(word_count) + " words long, with paragraphs written by " + formatter.format(author_count)
				+ " different people, and I sent you the last three paragraphs are in a private message... They're too awesome for everyone to "
				+ "see!");

			s = con.prepareStatement("SELECT * FROM `story` ORDER BY id DESC LIMIT 3");
			s.executeQuery();

			rs = s.getResultSet();
			rs.setFetchDirection(ResultSet.FETCH_REVERSE);
			rs.last();
			do {
				event.getUser()
					.send()
					.message(rs.getString("string"));
			} while (rs.previous());

			event.respond("You can read an excerpt in my profile here: http://www.nanowrimo.org/en/participants/timmybot");

			con.close();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void count(GenericMessageEvent event) {
		DecimalFormat formatter = new DecimalFormat("#,###");
		Connection con;
		int word_count = 0, author_count = 0;
		ResultSet rs;
		PreparedStatement s;

		try {
			con = Tim.db.pool.getConnection(timeout);

			s = con.prepareStatement("SELECT SUM( LENGTH( STRING ) - LENGTH( REPLACE( STRING ,  ' ',  '' ) ) +1 ) AS word_count FROM story");
			s.executeQuery();
			rs = s.getResultSet();
			while (rs.next()) {
				word_count = Integer.parseInt(rs.getString("word_count"));
			}

			s = con.prepareStatement("SELECT COUNT(DISTINCT author) AS author_count FROM story");
			s.executeQuery();
			rs = s.getResultSet();
			while (rs.next()) {
				author_count = Integer.parseInt(rs.getString("author_count"));
			}

			event.respond("My novel is currently " + formatter.format(word_count) + " words long, with paragraphs written by " + formatter.format(author_count)
				+ " different people.");

			con.close();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void storeLine(String line, String author) {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO story SET string = ?, author = ?, created = NOW()");
			s.setString(1, line);
			s.setString(2, author);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void helpSection(GenericUserEvent event) {
		String[] strs = {
			"Timmy's chain story is a ridiculous attempt to create a horrific 50,000 word 'novel' out of",
			"contributions from people in chat and autogenerated text. It will be released in December",
			"as a book you can purchase on Amazon, and all contributors will be given credit in the book.",
			"Chain Story Commands:",
			"    !chaininfo - General info about the current status of my novel.",
			"    !chainlast - The last paragraph of my novel, so you have something to base the next one one.",
			"    !chainnew <paragraph> - Provide the next paragraph of my great cyberspace novel!", "    !chaincount - Just the word count and author stats.",
		};

		for (String str : strs) {
			event.getUser()
				.send()
				.message(str);
		}
	}

	int wordcount() {
		Connection con;
		ResultSet rs;
		PreparedStatement s;
		int word_count = 0;

		try {
			con = Tim.db.pool.getConnection(timeout);

			s = con.prepareStatement("SELECT SUM( LENGTH( STRING ) - LENGTH( REPLACE( STRING ,  ' ',  '' ) ) +1 ) AS word_count FROM story");
			s.executeQuery();
			rs = s.getResultSet();
			while (rs.next()) {
				String count = rs.getString("word_count");
				if (count != null && !count.equals("")) {
					word_count = Integer.parseInt(count);
				}
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName())
				.log(Level.SEVERE, null, ex);
		}

		return word_count;
	}
}
