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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pircbotx.Colors;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author mwalker
 */
public class ChainStory {
	private long timeout = 3000;

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
	public boolean parseUserCommand( MessageEvent event ) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		String command;
		String argsString = "";

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			argsString = message.substring(space + 1);
		} else {
			command = message.substring(1).toLowerCase();
		}

		Calendar cal = Calendar.getInstance();
		boolean isNovember = (10 == cal.get(Calendar.MONTH));
		
		if (command.equals("chainlast")) {
			showLast(event);
			return true;
		} else if (command.equals("chainnew")) {
			if (!isNovember) {
				event.respond("Sorry, that command won't work outside November!");
				return true;
			}
			addNew(event, argsString);
			return true;
		} else if (command.equals("chaininfo")) {
			info(event);
			return true;
		} else if (command.equals("chaincount")) {
			count(event);
			return true;
		}

		return false;
	}

	protected void helpSection( MessageEvent event ) {
		String[] strs = {
			"Chain Story Commands:",
			"    !chaininfo - General info about the current status of my novel.",
			"    !chainlast - The last paragraph of my novel, so you have something to base the next one one.",
			"    !chainnew <paragraph> - Provide the next paragraph of my great cyberspace novel!",
		    "    !chaincount - Just the word count and author stats.",};

		for (int i = 0; i < strs.length; ++i) {
			Tim.bot.sendNotice(event.getUser(), strs[i]);
		}
	}

	public void refreshDbLists() {
	}

	public void info( MessageEvent event ) {
		DecimalFormat formatter = new DecimalFormat("#,###");
		Connection con;
		String last_line = "";
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

			event.respond("My novel is currently " + formatter.format(word_count) + " words long, with paragraphs written by " + formatter.format(author_count) + " different people, and the last paragraphs are:");
			
			s = con.prepareStatement("SELECT * FROM `story` ORDER BY id DESC LIMIT 3");
			s.executeQuery();

			rs = s.getResultSet();
			rs.setFetchDirection(ResultSet.FETCH_REVERSE);
			rs.last();
			while (true) {
				event.respond(rs.getString("string"));
				if (!rs.previous()) {
					break;
				}
			}

			event.respond("You can read an excerpt in my profile here: http://www.nanowrimo.org/en/participants/timmybot");

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void showLast( MessageEvent event ) {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT * FROM `story` ORDER BY id DESC LIMIT 3");
			s.executeQuery();

			event.respond("Let's see... the last paragraphs of my novel are...");

			ResultSet rs = s.getResultSet();
			rs.setFetchDirection(ResultSet.FETCH_REVERSE);
			rs.last();
			while (true) {
				event.respond(rs.getString("string"));
				if (!rs.previous()) {
					break;
				}
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void count( MessageEvent event ) {
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
			
			event.respond("My novel is currently " + formatter.format(word_count) + " words long, with paragraphs written by " + formatter.format(author_count) + " different people.");

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void addNew( MessageEvent event, String message ) {
		DecimalFormat formatter = new DecimalFormat("#,###");
		Connection con;
		if ("".equals(message)) {
			Tim.bot.sendAction(event.getChannel(), "blinks, and looks confused. \"But there's nothing there. That won't help my wordcount!\"");
		} else {
			try {
				con = Tim.db.pool.getConnection(timeout);

				storeLine(message, event.getUser().getNick());
				Tim.bot.sendAction(event.getChannel(), "quickly copies down what " + event.getUser().getNick() + " said. \"Thanks!\"");

				PreparedStatement s = con.prepareStatement("SELECT SUM( LENGTH( STRING ) - LENGTH( REPLACE( STRING ,  ' ',  '' ) ) +1 ) AS word_count FROM story");
				s.executeQuery();
				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					event.respond("My novel is now " + formatter.format(Integer.parseInt(rs.getString("word_count"))) + " words long!");
				}

				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
	
	public void storeLine(String line, String author) {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO story SET string = ?, author = ?, created = NOW()");
			s.setString(1, line);
			s.setString(2, author);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
