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

import com.mysql.jdbc.Driver;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pircbotx.Channel;
import snaq.db.ConnectionPool;

/**
 *
 * @author Matthew Walker
 */
public class DBAccess {
	private static DBAccess instance;
	private long timeout = 3000;
	protected Set<String> admin_list = new HashSet<String>(16);
	protected Hashtable<String, ChannelInfo> channel_data = new Hashtable<String, ChannelInfo>(62);
	protected List<String> extra_greetings = new ArrayList<String>();
	protected List<String> greetings = new ArrayList<String>();
	protected Set<String> ignore_list = new HashSet<String>(16);
	protected ConnectionPool pool;

	static {
		instance = new DBAccess();
	}

	private DBAccess() {
		Class c;
		Driver driver;

		/**
		 * Make sure the JDBC driver is initialized. Used by the connection pool.
		 */
		try {
			c = Class.forName("com.mysql.jdbc.Driver");
			driver = (Driver) c.newInstance();
			DriverManager.registerDriver(driver);
		} catch (Exception ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		// Initialize the connection pool, to prevent SQL timeout issues
		String url = "jdbc:mysql://" + Tim.config.getString("sql_server") + ":3306/" + Tim.config.getString("sql_database");
		pool = new ConnectionPool("local", 5, 25, 50, 180000, url, Tim.config.getString("sql_user"), Tim.config.getString("sql_password"));
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static DBAccess getInstance() {
		return instance;
	}

	public void deleteChannel(String channel) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `channels` WHERE `channel` = ?");
			s.setString(1, channel.toLowerCase());
			s.executeUpdate();

			// Will do nothing if the channel is not in the list.
			this.channel_data.remove(channel.toLowerCase());

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void deleteIgnore(String username) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `ignores` WHERE `name` = ?;");
			s.setString(1, username.toLowerCase());
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void getAdminList() {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `name` FROM `admins`");

			ResultSet rs = s.getResultSet();

			this.admin_list.clear();
			while (rs.next()) {
				this.admin_list.add(rs.getString("name").toLowerCase());
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void getChannelList() {
		Connection con;
		ChannelInfo ci;
		String channel;

		this.channel_data.clear();

		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM `channels`");

			while (rs.next()) {
				channel = rs.getString("channel").toLowerCase();
				ci = new ChannelInfo(channel, rs.getBoolean("adult"), rs.getBoolean("markhov"), rs.getBoolean("random"), rs.getBoolean("command"));
				ci.setChatterTimers(
					Integer.parseInt(getSetting("chatterMaxBaseOdds")),
					Integer.parseInt(getSetting("chatterNameMultiplier")),
					Integer.parseInt(getSetting("chatterTimeMultiplier")),
					Integer.parseInt(getSetting("chatterTimeDivisor")));

				this.channel_data.put(channel, ci);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void getGreetingList() {
		Connection con;
		try {
			con = pool.getConnection(timeout);
			Statement s = con.createStatement();
			ResultSet rs;

			s.executeQuery("SELECT `string` FROM `greetings`");
			rs = s.getResultSet();
			this.greetings.clear();
			while (rs.next()) {
				this.greetings.add(rs.getString("string"));
			}
			rs.close();

			s.executeQuery("SELECT `string` FROM `extra_greetings`");
			rs = s.getResultSet();
			this.extra_greetings.clear();
			while (rs.next()) {
				this.extra_greetings.add(rs.getString("string"));
			}
			rs.close();
			s.close();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void getIgnoreList() {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `name` FROM `ignores`");

			ResultSet rs = s.getResultSet();
			this.ignore_list.clear();
			while (rs.next()) {
				this.ignore_list.add(rs.getString("name").toLowerCase());
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public String getSetting(String key) {
		Connection con;
		String value = "";

		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `value` FROM `settings` WHERE `key` = ?");
			s.setString(1, key);
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				value = rs.getString("value");
			}

			con.close();
		} catch (Exception ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		return value;
	}

	public boolean isChannelAdult(Channel channel) {
		boolean val = false;
		ChannelInfo cdata = this.channel_data.get(channel.getName().toLowerCase());
		if (cdata != null) {
			val = cdata.isAdult;
		}
		return val;
	}

	public void saveChannel(String channel) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `channels` (`channel`, `adult`, `markhov`, `random`, `command`) VALUES (?, 0, 1, 1, 1)");
			s.setString(1, channel.toLowerCase());
			s.executeUpdate();

			if (!this.channel_data.containsKey(channel.toLowerCase())) {
				this.channel_data.put(channel.toLowerCase(), new ChannelInfo(channel.toLowerCase()));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void saveIgnore(String username) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `ignores` (`name`) VALUES (?);");
			s.setString(1, username.toLowerCase());
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void setChannelAdultFlag(String channel, boolean adult) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET adult = ? WHERE `channel` = ?");
			s.setBoolean(1, adult);
			s.setString(2, channel.toLowerCase());
			s.executeUpdate();

			if (adult) {
				this.channel_data.get(channel.toLowerCase()).isAdult = true;
			} else {
				this.channel_data.get(channel.toLowerCase()).isAdult = false;
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void setChannelMuzzledFlag(String channel, boolean muzzled) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET `markhov` = ?, `random` = ?, `command` = ? WHERE `channel` = ?");
			s.setBoolean(1, muzzled);
			s.setBoolean(2, muzzled);
			s.setBoolean(3, muzzled);
			s.setString(2, channel.toLowerCase());
			s.executeUpdate();

			this.channel_data.get(channel.toLowerCase()).doMarkhov = muzzled;
			this.channel_data.get(channel.toLowerCase()).doCommandActions = muzzled;
			this.channel_data.get(channel.toLowerCase()).doRandomActions = muzzled;

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void refreshDbLists() {
		this.getAdminList();
		this.getChannelList();
		this.getIgnoreList();
		this.getGreetingList();

//		this.amusement.refreshDbLists();
//		this.story.refreshDbLists();
//		this.challenge.refreshDbLists();
	}
}
