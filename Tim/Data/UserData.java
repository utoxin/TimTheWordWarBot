package Tim.Data;

import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

public class UserData {
	// Data for throttling lookups
	public boolean registrationDataRetrieved = false;
	public Date lastWhoisCheck;

	// Non-synced reference data
	public HashSet<String> nicks = new HashSet<>();
	
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
}
