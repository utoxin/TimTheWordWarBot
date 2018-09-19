package Tim.Data;

import Tim.Tim;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class UserData {
	// Data for throttling lookups
	public boolean registrationDataRetrieved = false;
	public Date lastWhoisCheck;

	// Non-synced reference data
	public HashSet<String> nicks = new HashSet<>();
	public HashMap<UUID, Integer> recordedWars = new HashMap<>();

	// DB synced data
	public UUID id;
	public String authedUser;
	public int totalSprintWordcount;
	public int totalSprints;
	public int totalSprintDuration;
	public boolean raptorAdopted;
	public String raptorName;
	public String raptorFavoriteColor;
	public int raptorBunniesStolen;
	public Date lastBunnyRaid;

	public void save() {
		try (Connection con = Tim.db.pool.getConnection(3000)) {
			PreparedStatement s = con.prepareStatement("REPLACE INTO `users` (`id`, `authed_user`, `total_sprint_wordcount`, `total_sprints`, `total_sprint_duration`, `raptor_adopted`, `raptor_name`, `raptor_favorite_color`, `raptor_bunnies_stolen`, `last_bunny_raid`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");

			s.setString(1, id.toString());
			s.setString(2, authedUser);
			s.setInt(3, totalSprintWordcount);
			s.setInt(4, totalSprints);
			s.setInt(5, totalSprintDuration);
			s.setBoolean(6, raptorAdopted);
			s.setString(7, raptorName);
			s.setString(8, raptorFavoriteColor);
			s.setInt(9, raptorBunniesStolen);
			if (lastBunnyRaid != null) {
				s.setDate(10, new java.sql.Date(lastBunnyRaid.getTime()));
			} else {
				s.setNull(10, Types.DATE);
			}

			s.executeUpdate();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}
}
