package Tim.Utility;

import Tim.Data.CommandData;
import Tim.Tim;

public class Permissions {
	public static boolean isAdmin(CommandData commandData) {
		return (commandData.getUserEvent().getUser() != null && Tim.db.admin_list.contains(commandData.issuer.toLowerCase()))
			|| Tim.db.admin_list.contains(commandData.event.getChannel().getName().toLowerCase())
			|| commandData.getUserEvent().getUser().getChannelsOpIn().contains(commandData.event.getChannel());
	}
}
