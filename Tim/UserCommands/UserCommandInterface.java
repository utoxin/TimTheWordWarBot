package Tim.UserCommands;

import org.pircbotx.hooks.events.MessageEvent;

public interface UserCommandInterface {
	void parseCommand(String command, String[] args, MessageEvent event);
}
