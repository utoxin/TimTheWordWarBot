package Tim;

import org.pircbotx.Channel;
import snaq.db.ConnectionPool;
import snaq.db.Select1Validator;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Matthew Walker
 */
public class DBAccess {
	private static final DBAccess instance;

	static {
		instance = new DBAccess();
	}

	private final long timeout = 3000;
	public HashMap<String, ChannelInfo> channel_data = new HashMap<>(62);
	public Set<String> admin_list = new HashSet<>(16);
	List<String> cat_herds = new ArrayList<>();
	public List<String> items = new ArrayList<>();
	public List<String> flavours = new ArrayList<>();
	List<String> eightBalls = new ArrayList<>();
	Set<String> ignore_list = new HashSet<>(16);
	Set<String> soft_ignore_list = new HashSet<>(16);
	public HashMap<String, Set<ChannelInfo>> channel_groups = new HashMap<>();
	public HashMap<String, List<String>> dynamic_lists = new HashMap<>();
	public HashMap<String, HashMap<String, Boolean>> userInteractionSettings = new HashMap<>();
	public ConnectionPool pool;

	private DBAccess() {
		// Initialize the connection pool, to prevent SQL timeout issues
		String url = "jdbc:mysql://" + Tim.config.getString("sql_server") + ":3306/" + Tim.config.getString("sql_database") + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
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

	private void getDynamicLists() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			Statement s = con.createStatement();
			s.executeQuery("SELECT `name`, `item_name` FROM `lists` INNER JOIN `list_items` ON `lists`.`id` = `list_items`.`list_id`");

			ResultSet rs = s.getResultSet();
			this.dynamic_lists.clear();
			while (rs.next()) {
				if (!dynamic_lists.containsKey(rs.getString("name"))) {
					this.dynamic_lists.put(rs.getString("name"), new ArrayList<>());
				}

				this.dynamic_lists.get(rs.getString("name")).add(rs.getString("item_name"));
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getItemList() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			Statement s = con.createStatement();
			s.executeQuery("SELECT `item` FROM `items` WHERE `approved` = TRUE");

			ResultSet rs = s.getResultSet();
			this.items.clear();
			while (rs.next()) {
				this.items.add(rs.getString("item"));
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getFlavourList() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `flavours`");

			ResultSet rs = s.getResultSet();
			this.flavours.clear();
			while (rs.next()) {
				this.flavours.add(rs.getString("string"));
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getEightballList() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `eightballs`");

			ResultSet rs = s.getResultSet();
			this.eightBalls.clear();
			while (rs.next()) {
				this.eightBalls.add(rs.getString("string"));
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getChannelGroups() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			Statement s = con.createStatement();
			s.executeQuery("SELECT `name`, `channels`.`channel` FROM `channel_groups` INNER JOIN `channels` ON (`channel_groups`.`channel_id` = `channels`.`id`)");

			ResultSet rs = s.getResultSet();

			this.channel_groups.clear();
			while (rs.next()) {
				if (!this.channel_groups.containsKey(rs.getString("name").toLowerCase())) {
					this.channel_groups.put(rs.getString("name").toLowerCase(), new HashSet<>());
				}

				this.channel_groups.get(rs.getString("name").toLowerCase()).add(this.channel_data.get(rs.getString("channel")));
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	public void removeFromChannelGroup(String group, ChannelInfo channel) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("DELETE `channel_groups`.* FROM `channel_groups` INNER JOIN `channels` ON (`channel_groups`.`channel_id` = `channels`.`id`) WHERE `channels`.`channel` = ? AND `channel_groups`.`name` = ?");
			s.setString(1, channel.channel);
			s.setString(2, group);
			s.executeUpdate();
			s.close();

			this.channel_groups.get(group.toLowerCase()).remove(channel);
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	public void addToChannelGroup(String group, ChannelInfo channel) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("REPLACE INTO `channel_groups` SET `name` = ?, `channel_id` = (SELECT `id` FROM `channels` WHERE `channel` = ?)");
			s.setString(1, group.toLowerCase());
			s.setString(2, channel.channel.toLowerCase());
			s.executeUpdate();

			if (!this.channel_groups.containsKey(group.toLowerCase())) {
				this.channel_groups.put(group.toLowerCase(), new HashSet<>());
			}

			this.channel_groups.get(group.toLowerCase()).add(channel);
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void deleteChannel(Channel channel) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("DELETE FROM `channels` WHERE `channel` = ?");
			s.setString(1, channel.getName().toLowerCase());
			s.executeUpdate();

			this.channel_data.remove(channel.getName().toLowerCase());
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void deleteWar(int id) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("DELETE FROM `wars` WHERE `id` = ?");
			s.setInt(1, id);
			s.executeUpdate();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void deleteIgnore(String username) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("DELETE FROM `ignores` WHERE `name` = ?;");
			s.setString(1, username.toLowerCase());
			s.executeUpdate();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getAdminList() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			Statement s = con.createStatement();
			s.executeQuery("SELECT `name` FROM `admins`");

			ResultSet rs = s.getResultSet();

			this.admin_list.clear();
			while (rs.next()) {
				this.admin_list.add(rs.getString("name").toLowerCase());
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getChannelList() {
		ChannelInfo ci;
		this.channel_data.clear();
		
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			ResultSet rs;
			try (Statement s = con.createStatement()) {
				rs = s.executeQuery("SELECT * FROM `channels`");
				PreparedStatement s2;
				ResultSet rs2;
				while (rs.next()) {
					ci = new ChannelInfo(rs.getString("channel").toLowerCase());
					ci.setDefaultOptions();

					ci.setReactiveChatter(
						rs.getFloat("reactive_chatter_level"),
						rs.getFloat("chatter_name_multiplier"));

					ci.setRandomChatter(
						rs.getFloat("random_chatter_level"));

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
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getCatHerds() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `cat_herds`");

			ResultSet rs = s.getResultSet();
			this.cat_herds.clear();
			while (rs.next()) {
				this.cat_herds.add(rs.getString("string"));
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getIgnoreList() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
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
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	String getSetting(String key) {
		String value = "";

		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("SELECT `value` FROM `settings` WHERE `key` = ?");
			s.setString(1, key);
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				value = rs.getString("value");
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}

		return value;
	}

	void joinChannel(Channel channel) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("INSERT INTO `channels` (`channel`) VALUES (?)");
			s.setString(1, channel.getName().toLowerCase());
			s.executeUpdate();
			s.close();

			if (!this.channel_data.containsKey(channel.getName().toLowerCase())) {
				ChannelInfo new_channel = new ChannelInfo(channel.getName().toLowerCase());
				new_channel.setDefaultOptions();

				this.channel_data.put(channel.getName().toLowerCase(), new_channel);
			}

			this.saveChannelSettings(this.channel_data.get(channel.getName().toLowerCase()));
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void saveChannelSettings(ChannelInfo channel) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			PreparedStatement s = con.prepareStatement("UPDATE `channels` SET reactive_chatter_level = ?, chatter_name_multiplier = ?, random_chatter_level = ?, tweet_bucket_max = ?, "
				+ "tweet_bucket_charge_rate = ?, auto_muzzle_wars = ?, velociraptor_sightings = ?, active_velociraptors = ?, dead_velociraptors = ?, killed_velociraptors = ?, last_sighting_date = ? WHERE channel = ?;");
			s.setFloat(1, channel.reactiveChatterLevel);
			s.setFloat(2, channel.chatterNameMultiplier);
			s.setFloat(3, channel.randomChatterLevel);
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
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void saveIgnore(String username, String type) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("INSERT INTO `ignores` (`name`, `type`) VALUES (?, ?);");
			s.setString(1, username.toLowerCase());
			s.setString(2, type.toLowerCase());
			s.executeUpdate();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void refreshDbLists() {
		this.getAdminList();
		this.getChannelList();
		this.getIgnoreList();
		this.getCatHerds();
		this.getChannelGroups();
		this.getEightballList();
		this.getFlavourList();
		this.getItemList();
		this.getDynamicLists();
		this.loadInteractionSettings();

		Tim.amusement.refreshDbLists();
		Tim.markovProcessor.refreshDbLists();
	}

	int create_war(String channel, String starter, String name, long base_duration, long duration, long remaining, long time_to_start, int total_chains, int current_chain, int break_duration, boolean do_randomness) {
		int id = 0;

		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("INSERT INTO `wars` (`channel`, `starter`, `name`, `base_duration`, `randomness`, `delay`, `duration`, `remaining`, `time_to_start`, `total_chains`, `current_chain`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			s.setString(1, channel.toLowerCase());
			s.setString(2, starter);
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
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}

		return id;
	}

	void update_war(int db_id, long duration, long remaining, long time_to_start, int current_chain) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement s = con.prepareStatement("UPDATE `wars` SET `duration` = ? ,`remaining` = ?, `time_to_start` = ?, `current_chain` = ? WHERE id = ?");
			s.setLong(1, duration);
			s.setLong(2, remaining);
			s.setLong(3, time_to_start);
			s.setInt(4, current_chain);
			s.setInt(5, db_id);
			s.executeUpdate();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void updateWarMembers(int war_id, ConcurrentHashMap<String, Integer> warMembers) {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement purgeOld = con.prepareStatement("DELETE FROM war_members WHERE war_id = ?");
			purgeOld.setInt(1, war_id);
			purgeOld.execute();

			PreparedStatement addCurrent = con.prepareStatement("INSERT INTO war_members SET war_id = ?, nickname = ?, starting_count = ?");
			for(Map.Entry<String, Integer> entry : warMembers.entrySet()) {
				String nick = entry.getKey();
				Integer wordCount = entry.getValue();

				addCurrent.setInt(1, war_id);
				addCurrent.setString(2, nick);
				addCurrent.setInt(3, wordCount);
				addCurrent.executeUpdate();
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	ConcurrentHashMap<String, WordWar> loadWars() {
		ConcurrentHashMap<String, WordWar> wars = new ConcurrentHashMap<>(32);
		
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery("SELECT * FROM `wars`");

			while (rs.next()) {
				if (channel_data.get(rs.getString("channel")) != null) {
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
						rs.getString("starter"),
						rs.getString("channel"),
						rs.getInt("id")
					);

					wars.put(war.getInternalName(), war);
				}
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}

		return wars;
	}

	ConcurrentHashMap<String, Integer> loadWarMembers(Integer war_id) {
		ConcurrentHashMap<String, Integer> warMembers = new ConcurrentHashMap<>();
		
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			PreparedStatement fetchMembers = con.prepareStatement("SELECT * FROM `war_members` WHERE war_id = ?");
			fetchMembers.setInt(1, war_id);
			ResultSet rs = fetchMembers.executeQuery();

			while (rs.next()) {
				warMembers.put(rs.getString("nickname"), rs.getInt("starting_count"));
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}

		return warMembers;
	}

	private void loadInteractionSettings() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			Statement s = con.createStatement();
			s.executeQuery("SELECT `username`, `setting`, `value` FROM `user_interaction_settings`");

			ResultSet rs = s.getResultSet();
			this.userInteractionSettings.clear();

			while (rs.next()) {
				String username = rs.getString("username").toLowerCase();
				String setting = rs.getString("setting");
				Boolean value = rs.getBoolean("value");

				HashMap<String, Boolean> userMap;
				if (!userInteractionSettings.containsKey(username.toLowerCase())) {
					userMap = new HashMap<>();
					userInteractionSettings.put(username.toLowerCase(), userMap);
				} else {
					userMap = userInteractionSettings.get(username.toLowerCase());
				}

				userMap.put(setting, value);
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	public void saveInteractionSettings() {
		try (Connection con = Tim.db.pool.getConnection(timeout)) {
			con.createStatement().execute("TRUNCATE `user_interaction_settings`");
			PreparedStatement s = con.prepareStatement("INSERT INTO `user_interaction_settings` SET `username` = ?, `setting` = ?, `value` = ?");

			for (String user : userInteractionSettings.keySet()) {
				HashMap<String, Boolean> userMap = userInteractionSettings.get(user.toLowerCase());

				for (String setting : userMap.keySet()) {
					s.setString(1, user.toLowerCase());
					s.setString(2, setting);
					s.setBoolean(3, userMap.get(setting));

					s.execute();
				}
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}
}
