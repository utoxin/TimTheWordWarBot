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
import org.pircbotx.User;
import snaq.db.ConnectionPool;
import snaq.db.Select1Validator;

/**
 *
 * @author Matthew Walker
 */
public class DBAccess {
	private static DBAccess instance;
	private long timeout = 3000;
	protected Set<String> admin_list = new HashSet<String>(16);
	protected HashMap<String, ChannelInfo> channel_data = new HashMap<String, ChannelInfo>(62);
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
		String url = "jdbc:mysql://" + Tim.config.getString("sql_server") + ":3306/" + Tim.config.getString("sql_database") + "?useUnicode=true&characterEncoding=UTF-8";
		pool = new ConnectionPool("local", 5, 25, 50, 180000, url, Tim.config.getString("sql_user"), Tim.config.getString("sql_password"));
		pool.setValidator(new Select1Validator());
		pool.setAsyncDestroy(true);
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static DBAccess getInstance() {
		return instance;
	}

	public void deleteChannel( Channel channel ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `channels` WHERE `channel` = ?");
			s.setString(1, channel.getName().toLowerCase());
			s.executeUpdate();

			// Will do nothing if the channel is not in the list.
			this.channel_data.remove(channel.getName().toLowerCase());

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void deleteWar( int id ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `wars` WHERE `id` = ?");
			s.setInt(1, id);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void deleteIgnore( String username ) {
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
		Channel channel;

		this.channel_data.clear();

		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM `channels`");

			while (rs.next()) {
				channel = Tim.bot.getChannel(rs.getString("channel"));
				ci = new ChannelInfo(
					channel, 
					rs.getBoolean("adult"), 
					rs.getBoolean("markhov"), 
					rs.getBoolean("random"), 
					rs.getBoolean("command"), 
					rs.getBoolean("relay_nanowrimo"),
					rs.getBoolean("relay_nanowordsprints"),
					rs.getBoolean("relay_bottimmy"),
					rs.getBoolean("relay_officeduckfrank")
				);

				ci.setChatterTimers(
					Integer.parseInt(getSetting("chatterMaxBaseOdds")),
					Integer.parseInt(getSetting("chatterNameMultiplier")),
					Integer.parseInt(getSetting("chatterTimeMultiplier")),
					Integer.parseInt(getSetting("chatterTimeDivisor")),
					rs.getInt("chatter_level"));

				this.channel_data.put(channel.getName().toLowerCase(), ci);
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

	public String getSetting( String key ) {
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

	public boolean isChannelAdult( Channel channel ) {
		boolean val = false;
		ChannelInfo cdata = this.channel_data.get(channel.getName().toLowerCase());
		if (cdata != null) {
			val = cdata.isAdult;
		}
		return val;
	}

	public void saveChannel( Channel channel ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `channels` (`channel`, `adult`, `markhov`, `random`, `command`) VALUES (?, 0, 1, 1, 1)");
			s.setString(1, channel.getName().toLowerCase());
			s.executeUpdate();

			if (!this.channel_data.containsKey(channel.getName())) {
				ChannelInfo new_channel = new ChannelInfo(channel);
				new_channel.setChatterTimers(
					Integer.parseInt(getSetting("chatterMaxBaseOdds")),
					Integer.parseInt(getSetting("chatterNameMultiplier")),
					Integer.parseInt(getSetting("chatterTimeMultiplier")),
					Integer.parseInt(getSetting("chatterTimeDivisor")),
					3);

				this.channel_data.put(channel.getName().toLowerCase(), new_channel);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void saveIgnore( String username ) {
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

	public void setChannelAdultFlag( Channel channel, boolean adult ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET adult = ? WHERE `channel` = ?");
			s.setBoolean(1, adult);
			s.setString(2, channel.getName());
			s.executeUpdate();

			if (adult) {
				this.channel_data.get(channel.getName()).isAdult = true;
			} else {
				this.channel_data.get(channel.getName()).isAdult = false;
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void setChannelChatterLevel( Channel channel, int level ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET chatter_level = ? WHERE `channel` = ?");
			s.setInt(1, level);
			s.setString(2, channel.getName());
			s.executeUpdate();

			this.channel_data.get(channel.getName()).chatterLevel = level;

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void setChannelMuzzledFlag( Channel channel, boolean muzzled ) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET `markhov` = ?, `random` = ?, `command` = ? WHERE `channel` = ?");
			s.setBoolean(1, muzzled);
			s.setBoolean(2, muzzled);
			s.setBoolean(3, muzzled);
			s.setString(4, channel.getName());
			s.executeUpdate();

			this.channel_data.get(channel.getName()).doMarkov = muzzled;
			this.channel_data.get(channel.getName()).doCommandActions = muzzled;
			this.channel_data.get(channel.getName()).doRandomActions = muzzled;

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void setRelayTwitterFlag( Channel channel, boolean relay) {
		setRelayTwitterFlag(channel, relay, "all");
	}

	public void setRelayTwitterFlag( Channel channel, boolean relay, String account ) {
		Connection con;		
		try {
			con = pool.getConnection(timeout);

			if (account.equals("all")) {
				PreparedStatement s = con.prepareStatement("UPDATE `channels` SET `relay_nanowrimo` = ?, `relay_nanowordsprints` = ?, `relay_bottimmy` = ?, `relay_officeduckfrank` = ? WHERE `channel` = ?");
				s.setBoolean(1, relay);
				s.setBoolean(2, relay);
				s.setBoolean(3, relay);
				s.setBoolean(4, relay);
				s.setString(5, channel.getName());
				s.executeUpdate();

				this.channel_data.get(channel.getName()).relayBotTimmy = relay;
				this.channel_data.get(channel.getName()).relayNaNoWordSprints = relay;
				this.channel_data.get(channel.getName()).relayNaNoWriMo = relay;
				this.channel_data.get(channel.getName()).relayofficeduckfrank = relay;
			} else {
				PreparedStatement s = con.prepareStatement("UPDATE `channels` SET `relay_" + account + "` = ? WHERE `channel` = ?");
				s.setBoolean(1, relay);
				s.setString(2, channel.getName());
				s.executeUpdate();
				
				if (account.equals("nanowrimo")) {
					this.channel_data.get(channel.getName()).relayNaNoWriMo = relay;
				} else if (account.equals("nanowordsprints")) {
					this.channel_data.get(channel.getName()).relayNaNoWordSprints = relay;
				} else if (account.equals("bottimmy")) {
					this.channel_data.get(channel.getName()).relayBotTimmy = relay;
				} else if (account.equals("officeduckfrank")) {
					this.channel_data.get(channel.getName()).relayofficeduckfrank = relay;
				}
			}

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

		Tim.amusement.refreshDbLists();
		Tim.story.refreshDbLists();
		Tim.challenge.refreshDbLists();
		Tim.markhov.refreshDbLists();
	}

	public int create_war(Channel channel, User starter, String name, long duration, long remaining, long time_to_start) {
		int id = 0;
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `wars` (`channel`, `starter`, `name`, `duration`, `remaining`, `time_to_start`) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			s.setString(1, channel.getName().toLowerCase());
			s.setString(2, starter.getNick());
			s.setString(3, name);
			s.setLong(4, duration);
			s.setLong(5, remaining);
			s.setLong(6, time_to_start);
			s.executeUpdate();

			ResultSet rs = s.getGeneratedKeys();
			
			if (rs.next()) {
				id = rs.getInt(1);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return id;
	}

	public void update_war(int db_id, long remaining, long time_to_start) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `wars` SET `remaining` = ?, `time_to_start` = ? WHERE id = ?");
			s.setLong(1, remaining);
			s.setLong(2, time_to_start);
			s.setInt(3, db_id);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public Map<String, WordWar> loadWars() {
		Connection con;
		Channel channel;
		User user;
		Map<String, WordWar> wars = Collections.synchronizedMap(new HashMap<String, WordWar>());

		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM `wars`");

			while (rs.next()) {
				channel = Tim.bot.getChannel(rs.getString("channel"));
				user = Tim.bot.getUser(rs.getString("starter"));

				WordWar war = new WordWar(
					rs.getLong("duration"),
					rs.getLong("remaining"),
					rs.getLong("time_to_start"),
					rs.getString("name"),
					user,
					channel,
					rs.getInt("id")
				);

				wars.put(war.getName().toLowerCase(), war);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		
		return wars;
	}
}
