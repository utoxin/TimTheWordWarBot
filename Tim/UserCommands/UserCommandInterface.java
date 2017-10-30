package Tim.UserCommands;

import org.pircbotx.hooks.events.MessageEvent;

public interface UserCommandInterface {
	boolean parseCommand(String command, String[] args, MessageEvent event);
}
