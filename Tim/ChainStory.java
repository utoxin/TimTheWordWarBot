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
		}
		
		return false;
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
