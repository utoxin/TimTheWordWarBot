package Tim.Commands;

import org.pircbotx.hooks.events.MessageEvent;

public interface UserCommandInterface {
	boolean parseUserCommand(String command, String[] args, MessageEvent event);
}
