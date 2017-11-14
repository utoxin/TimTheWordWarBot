package Tim.Commands;

import org.pircbotx.hooks.events.MessageEvent;

public interface AdminCommandInterface {
	boolean parseAdminCommand(String command, String[] args, MessageEvent event);
}
