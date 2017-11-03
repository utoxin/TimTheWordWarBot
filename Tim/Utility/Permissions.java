package Tim.Utility;

import Tim.Tim;
import org.pircbotx.hooks.events.MessageEvent;

public class Permissions {
	public static boolean isAdmin(MessageEvent event) {
		return (event.getUser() != null && Tim.db.admin_list.contains(event.getUser().getNick().toLowerCase()))
			|| Tim.db.admin_list.contains(event.getChannel().getName().toLowerCase())
			|| event.getUser().getChannelsOpIn().contains(event.getChannel());
	}
}
