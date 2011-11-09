/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Tim;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.*;

/**
 *
 * @author mwalker
 */
public class ChainStory {
	Tim ircclient;

	public ChainStory(Tim ircclient) {
		this.ircclient = ircclient;
	}

	/**
	 * Parses user-level commands passed from the main class. Returns true if 
	 * the message was handled, false if it was not.
	 * 
	 * @param channel
	 * @param sender
	 * @param prefix
	 * @param message
	 * @return 
	 */
	public boolean parseUserCommand(String channel, String sender, String prefix, String message) {
		String command;
		String argsString = "";
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			argsString = message.substring(space + 1);
			args = argsString.split(" ", 0);
		}
		else {
			command = message.substring(1).toLowerCase();
		}

		if (prefix.equals("!")) {
			if (command.equals("chainlast")) {
				showLast(channel);
				return true;
			}
			else if (command.equals("chainnew")) {
				addNew(channel, sender, argsString);
				return true;
			}
			else if (command.equals("chaininfo")) {
				info(channel);
				return true;
			}
			else if (command.equals("chainhelp")) {
				help(sender, channel);
				return true;
			}
		}
		
		return false;
	}

	private void help(String target, String channel) {
		int msgdelay = 9;
		String[] strs = { 
						  "!chaininfo - General info about the current status of my navel.",
						  "!chainlast - The last line of my novel, so you have something to base the next one one.",
						  "!chainnew <new line for novel> - Provide the next line of my great cyberspace novel!",
						  "!chainhelp - Get help on my chain story commands",
		};

		this.ircclient.sendAction(channel, "whispers in " + target + "'s ear. (Check for a new windor or tab with the help text.)");
		for (int i = 0; i < strs.length; ++i) {
			this.ircclient.sendDelayedMessage(target, strs[i], msgdelay * i);
		}
	}

	public void refreshDbLists() {

	}
		
	public void info(String channel) {
		long timeout = 3000;
		Connection con = null;
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

			this.ircclient.sendMessage(channel, "My novel is currently " + word_count + " words long, with sections written by " + author_count + " different people, and the last line is:");
			this.ircclient.sendMessage(channel, last_line);
			this.ircclient.sendMessage(channel, "You can read an excerpt in my profile here: http://www.nanowrimo.org/en/participants/timmybot");
			
			con.close();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public void showLast(String channel) {
		long timeout = 3000;
		Connection con = null;

		try {
			con = ircclient.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT * FROM `story` ORDER BY id DESC LIMIT 1");
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				this.ircclient.sendMessage(channel, "Let's see... the last line of my novel is...");
				this.ircclient.sendMessage(channel, rs.getString("string"));
			}
			
			con.close();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void addNew(String channel, String sender, String message) {
		long timeout = 3000;
		Connection con = null;

		if ("".equals(message)) {
			this.ircclient.sendAction(channel, "blinks, and looks confused. \"But there's nothing there. That won't help my wordcount!\"");
		}
		else {
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
			}
			catch (SQLException ex) {
				Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
