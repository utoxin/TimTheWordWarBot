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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mwalker
 */
public class ChainStory {
	Tim ircclient;
	private long timeout = 3000;
	private Connection con = null;

	public ChainStory( Tim ircclient ) {
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
		String command;
		String argsString = "";
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			argsString = message.substring(space + 1);
			args = argsString.split(" ", 0);
		} else {
			command = message.substring(1).toLowerCase();
		}

		if (prefix.equals("!")) {
			if (command.equals("chainlast")) {
				showLast(channel);
				return true;
			} else if (command.equals("chainnew")) {
					addNew(channel, sender, argsString);
					return true;
			} else if (command.equals("chaininfo")) {
					info(channel);
					return true;
			}
		}

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

	protected int helpSection( String target, String channel, int delayCnt, int msgdelay ) {
		String[] strs = {
			"Chain Story Commands:",
			"    !chaininfo - General info about the current status of my navel.",
			"    !chainlast - The last paragraph of my novel, so you have something to base the next one one.",
			"    !chainnew <paragraph> - Provide the next paragraph of my great cyberspace novel!",};

		for (int i = 0; i < strs.length; ++i, ++delayCnt) {
			ircclient.sendDelayedMessage(target, strs[i], msgdelay * delayCnt);
		}
		
		return delayCnt;
	}

	public void refreshDbLists() {
	}

	public void info( String channel ) {
		String word_count = "", last_line = "", author_count = "";
		ResultSet rs;
		PreparedStatement s;

		try {
			con = ircclient.pool.getConnection(timeout);

			s = con.prepareStatement("SELECT SUM( LENGTH( STRING ) - LENGTH( REPLACE( STRING ,  ' ',  '' ) ) +1 ) AS word_count FROM story");
			s.executeQuery();
			rs = s.getResultSet();
			while (rs.next()) {
				word_count = rs.getString("word_count");
			}

			s = con.prepareStatement("SELECT COUNT(DISTINCT author) AS author_count FROM story");
			s.executeQuery();
			rs = s.getResultSet();
			while (rs.next()) {
				author_count = rs.getString("author_count");
			}

			s = con.prepareStatement("SELECT * FROM `story` ORDER BY id DESC LIMIT 1");
			s.executeQuery();

			rs = s.getResultSet();
			while (rs.next()) {
				last_line = rs.getString("string");
			}

			this.ircclient.sendMessage(channel, "My novel is currently " + word_count + " words long, with paragraphs written by " + author_count + " different people, and the last paragraph is:");
			this.ircclient.sendMessage(channel, last_line);
			this.ircclient.sendMessage(channel, "You can read an excerpt in my profile here: http://www.nanowrimo.org/en/participants/timmybot");

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void showLast( String channel ) {
		try {
			con = ircclient.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT * FROM `story` ORDER BY id DESC LIMIT 1");
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				this.ircclient.sendMessage(channel, "Let's see... the last paragraph of my novel is...");
				this.ircclient.sendMessage(channel, rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void addNew( String channel, String sender, String message ) {
		if ("".equals(message)) {
			this.ircclient.sendAction(channel, "blinks, and looks confused. \"But there's nothing there. That won't help my wordcount!\"");
		} else {
			try {
				con = ircclient.pool.getConnection(timeout);

				PreparedStatement s = con.prepareStatement("INSERT INTO story SET string = ?, author = ?, created = NOW()");
				s.setString(1, message);
				s.setString(2, sender);
				s.executeUpdate();

				this.ircclient.sendAction(channel, "quickly copies down what " + sender + " said. \"Thanks!\"");

				s = con.prepareStatement("SELECT SUM( LENGTH( STRING ) - LENGTH( REPLACE( STRING ,  ' ',  '' ) ) +1 ) AS word_count FROM story");
				s.executeQuery();
				ResultSet rs = s.getResultSet();
				while (rs.next()) {
					this.ircclient.sendMessage(channel, "My novel is now " + rs.getString("word_count") + " words long!");
				}

				con.close();
			} catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
