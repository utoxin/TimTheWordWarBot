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

import com.mysql.jdbc.Driver;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;
import org.pircbotx.Channel;
import org.pircbotx.User;
import snaq.db.ConnectionPool;
import snaq.db.Select1Validator;

/**
 *
 * @author Matthew Walker
 */
class DBAccess {
	private static final DBAccess instance;
	private final long timeout = 3000;

	protected HashMap<String, ChannelInfo> channel_data = new HashMap<>(62);

	Set<String> admin_list = new HashSet<>(16);
	List<String> extra_greetings = new ArrayList<>();
	List<String> cat_herds = new ArrayList<>();
	List<String> greetings = new ArrayList<>();
	List<String> pokemon = new ArrayList<>();
	Set<String> ignore_list = new HashSet<>(16);
	Set<String> soft_ignore_list = new HashSet<>(16);
	ConnectionPool pool;

	static {
		instance = new DBAccess();
	}

	private DBAccess() {
		Class c;
		Driver driver;

		/*
		 * Make sure the JDBC driver is initialized. Used by the connection pool.
		 */
		try {
			c = Class.forName("com.mysql.jdbc.Driver");
			driver = (Driver) c.newInstance();
			DriverManager.registerDriver(driver);
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		// Initialize the connection pool, to prevent SQL timeout issues
		String url = "jdbc:mysql://" + Tim.config.getString("sql_server") + ":3306/" + Tim.config.getString("sql_database") + "?useUnicode=true&characterEncoding=UTF-8";
		pool = new ConnectionPool("local", 5, 25, 150, 180000, url, Tim.config.getString("sql_user"), Tim.config.getString("sql_password"));
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

	void deleteChannel(Channel channel) {
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

	void deleteWar(int id) {
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

	void deleteIgnore(String username) {
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

	private void getAdminList() {
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

	private void getChannelList() {
		Connection con;
		ChannelInfo ci;

		this.channel_data.clear();

		try {
			con = pool.getConnection(timeout);

			ResultSet rs;
			try (Statement s = con.createStatement()) {
				rs = s.executeQuery("SELECT * FROM `channels`");
				PreparedStatement s2;
				ResultSet rs2;
				while (rs.next()) {
					ci = new ChannelInfo(rs.getString("channel"));
					ci.setDefaultOptions();

					ci.setReactiveChatter(
						rs.getInt("reactive_chatter_level"), 
						rs.getInt("chatter_name_multiplier"));

					ci.setRandomChatter(
						rs.getInt("random_chatter_level"));

					ci.setTwitterTimers(
						rs.getFloat("tweet_bucket_max"),
						rs.getFloat("tweet_bucket_charge_rate"));

					ci.setWarAutoMuzzle(
						rs.getBoolean("auto_muzzle_wars"));
					
					ci.velociraptorSightings = rs.getInt("velociraptor_sightings");
					ci.activeVelociraptors = rs.getInt("active_velociraptors");
					ci.deadVelociraptors = rs.getInt("dead_velociraptors");
					ci.killedVelociraptors = rs.getInt("killed_velociraptors");
					
					if (rs.getDate("last_sighting_date") != null) {
						long time = rs.getDate("last_sighting_date").getTime();
						ci.lastSighting = new java.util.Date(time);
					}

					s2 = con.prepareStatement("SELECT `setting`, `value` FROM `channel_chatter_settings` WHERE `channel` = ?");
					s2.setString(1, rs.getString("channel"));
					s2.executeQuery();

					rs2 = s2.getResultSet();
					while (rs2.next()) {
						ci.addChatterSetting(rs2.getString("setting"), rs2.getBoolean("value"));
					}

					s2.close();
					rs2.close();

					s2 = con.prepareStatement("SELECT `setting`, `value` FROM `channel_command_settings` WHERE `channel` = ?");
					s2.setString(1, rs.getString("channel"));
					s2.executeQuery();

					rs2 = s2.getResultSet();
					while (rs2.next()) {
						ci.addCommandSetting(rs2.getString("setting"), rs2.getBoolean("value"));
					}

					s2.close();
					rs2.close();

					s2 = con.prepareStatement("SELECT `account` FROM `channel_twitter_feeds` WHERE `channel` = ?");
					s2.setString(1, rs.getString("channel"));
					s2.executeQuery();

					rs2 = s2.getResultSet();
					while (rs2.next()) {
						ci.addTwitterAccount(rs2.getString("account"));
					}

					s2.close();
					rs2.close();

					this.channel_data.put(ci.channel, ci);

					this.saveChannelSettings(ci);
				}
			}
			rs.close();
			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getGreetingList() {
		Connection con;
		try {
			con = pool.getConnection(timeout);
			try (Statement s = con.createStatement()) {
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
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getCatHerds() {
		Connection con;
		try {
			con = pool.getConnection(timeout);
			try (Statement s = con.createStatement()) {
				ResultSet rs;

				s.executeQuery("SELECT `string` FROM `cat_herds`");
				rs = s.getResultSet();
				this.cat_herds.clear();
				while (rs.next()) {
					this.cat_herds.add(rs.getString("string"));
				}
				rs.close();
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getPokemon() {
		Connection con;
		try {
			con = pool.getConnection(timeout);
			try (Statement s = con.createStatement()) {
				ResultSet rs;

				s.executeQuery("SELECT `name` FROM `pokemon`");
				rs = s.getResultSet();
				this.pokemon.clear();
				while (rs.next()) {
					this.pokemon.add(rs.getString("name"));
				}
				rs.close();
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getIgnoreList() {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `name`, `type` FROM `ignores`");

			ResultSet rs = s.getResultSet();
			this.ignore_list.clear();
			this.soft_ignore_list.clear();
			while (rs.next()) {
				if (rs.getString("type").equals("hard")) {
					this.ignore_list.add(rs.getString("name").toLowerCase());
				} else {
					this.soft_ignore_list.add(rs.getString("name").toLowerCase());
				}
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	String getSetting(String key) {
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
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		return value;
	}

	void joinChannel(Channel channel) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `channels` (`channel`) VALUES (?)");
			s.setString(1, channel.getName().toLowerCase());
			s.executeUpdate();

			if (!this.channel_data.containsKey(channel.getName().toLowerCase())) {
				ChannelInfo new_channel = new ChannelInfo(channel.getName().toLowerCase());
				new_channel.setDefaultOptions();

				this.channel_data.put(channel.getName().toLowerCase(), new_channel);
			}

			con.close();

			this.saveChannelSettings(this.channel_data.get(channel.getName().toLowerCase()));
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	String getRandomChannelWithVelociraptors(String exclude) {
		Connection con;
		String value = "";

		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `channel` FROM `channels` WHERE `channel` != ? AND `active_velociraptors` > 0 ORDER BY (RAND() * active_velociraptors) DESC LIMIT 1;");
			s.setString(1, exclude);
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				value = rs.getString("channel");
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		return value;
	}

	void saveChannelSettings(ChannelInfo channel) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET reactive_chatter_level = ?, chatter_name_multiplier = ?, random_chatter_level = ?, tweet_bucket_max = ?, "
				+ "tweet_bucket_charge_rate = ?, auto_muzzle_wars = ?, velociraptor_sightings = ?, active_velociraptors = ?, dead_velociraptors = ?, killed_velociraptors = ?, last_sighting_date = ? WHERE channel = ?;");
			s.setInt(1, channel.reactiveChatterLevel);
			s.setInt(2, channel.chatterNameMultiplier);
			s.setInt(3, channel.randomChatterLevel);
			s.setFloat(4, channel.tweetBucketMax);
			s.setFloat(5, channel.tweetBucketChargeRate);
			s.setBoolean(6, channel.auto_muzzle_wars);
			s.setInt(7, channel.velociraptorSightings);
			s.setInt(8, channel.activeVelociraptors);
			s.setInt(9, channel.deadVelociraptors);
			s.setInt(10, channel.killedVelociraptors);
			if (channel.lastSighting != null) {
				s.setString(11, sdf.format(channel.lastSighting));
			} else {
				s.setString(11, "1999-01-01");
			}
			s.setString(12, channel.channel);
			s.executeUpdate();

			s = con.prepareStatement("DELETE FROM `channel_chatter_settings` WHERE `channel` = ?;");
			s.setString(1, channel.channel);
			s.executeUpdate();

			s = con.prepareStatement("DELETE FROM `channel_command_settings` WHERE `channel` = ?;");
			s.setString(1, channel.channel);
			s.executeUpdate();

			s = con.prepareStatement("DELETE FROM `channel_twitter_feeds` WHERE `channel` = ?;");
			s.setString(1, channel.channel);
			s.executeUpdate();

			s = con.prepareStatement("INSERT INTO `channel_chatter_settings` SET `channel` = ?, `setting` = ?, `value` = ?;");
			s.setString(1, channel.channel);

			for (Map.Entry<String, Boolean> setting : channel.chatter_enabled.entrySet()) {
				s.setString(2, setting.getKey());
				s.setBoolean(3, setting.getValue());
				s.executeUpdate();
			}

			s = con.prepareStatement("INSERT INTO `channel_command_settings` SET `channel` = ?, `setting` = ?, `value` = ?;");
			s.setString(1, channel.channel);

			for (Map.Entry<String, Boolean> setting : channel.commands_enabled.entrySet()) {
				s.setString(2, setting.getKey());
				s.setBoolean(3, setting.getValue());
				s.executeUpdate();
			}

			s = con.prepareStatement("INSERT INTO `channel_twitter_feeds` SET `channel` = ?, `account` = ?;");
			s.setString(1, channel.channel);

			for (String account : channel.twitter_accounts) {
				s.setString(2, account);
				s.executeUpdate();
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void saveIgnore(String username, String type) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `ignores` (`name`, `type`) VALUES (?, ?);");
			s.setString(1, username.toLowerCase());
			s.setString(2, type.toLowerCase());
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void refreshDbLists() {
		this.getAdminList();
		this.getChannelList();
		this.getIgnoreList();
		this.getGreetingList();
		this.getCatHerds();
		this.getPokemon();

		Tim.amusement.refreshDbLists();
		Tim.story.refreshDbLists();
		Tim.challenge.refreshDbLists();
		Tim.markov.refreshDbLists();
	}

	int create_war(Channel channel, User starter, String name, long base_duration, long duration, long remaining, long time_to_start, int total_chains, int current_chain, int break_duration, boolean do_randomness) {
		int id = 0;
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `wars` (`channel`, `starter`, `name`, `base_duration`, `randomness`, `delay`, `duration`, `remaining`, `time_to_start`, `total_chains`, `current_chain`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			s.setString(1, channel.getName().toLowerCase());
			s.setString(2, starter.getNick());
			s.setString(3, name);
			s.setLong(4, base_duration);
			s.setBoolean(5, do_randomness);
			s.setLong(6, break_duration);
			s.setLong(7, duration);
			s.setLong(8, remaining);
			s.setLong(9, time_to_start);
			s.setInt(10, total_chains);
			s.setInt(11, current_chain);
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

	void update_war(int db_id, long duration, long remaining, long time_to_start, int current_chain) {
		Connection con;
		try {
			con = pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `wars` SET `duration` = ? ,`remaining` = ?, `time_to_start` = ?, `current_chain` = ? WHERE id = ?");
			s.setLong(1, duration);
			s.setLong(2, remaining);
			s.setLong(3, time_to_start);
			s.setInt(4, current_chain);
			s.setInt(5, db_id);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	ConcurrentHashMap<String, WordWar> loadWars() {
		Connection con;
		Channel channel;
		User user;
		ConcurrentHashMap<String, WordWar> wars = new ConcurrentHashMap<>(32);

		try {
			con = pool.getConnection(timeout);

			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM `wars`");

			while (rs.next()) {
				channel = Tim.bot.getUserChannelDao().getChannel(rs.getString("channel"));
				user = Tim.bot.getUserChannelDao().getUser(rs.getString("starter"));

				WordWar war = new WordWar(
					rs.getLong("base_duration"),
					rs.getLong("duration"),
					rs.getLong("remaining"),
					rs.getLong("time_to_start"),
					rs.getInt("total_chains"),
					rs.getInt("current_chain"),
					rs.getInt("delay"),
					rs.getBoolean("randomness"),
					rs.getString("name"),
					user,
					channel,
					rs.getInt("id")
				);

				wars.put(war.getName(false).toLowerCase(), war);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}

		return wars;
	}

	public void storeChatInput(String input) {

	}
}
