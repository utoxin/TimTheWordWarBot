package Tim.Utility;

import Tim.Data.CommandData;
import org.pircbotx.Colors;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.hooks.types.GenericUserEvent;

import java.util.regex.Pattern;

public class CommandParser {
	public static <T extends GenericChannelEvent & GenericUserEvent & GenericMessageEvent> CommandData parseCommand(T event) {
		CommandData<T> commandData = new CommandData<>();
		commandData.setEvent(event);
		commandData.issuer = event.getUser().getNick();
		event.getUser();

		String message = Colors.removeFormattingAndColors(event.getMessage());

		if (message.charAt(0) == '!') {
			if (message.startsWith("!skynet")) {
				commandData.type = CommandData.CommandType.SKYNET_USER;
			} else if (message.length() == 1) {
				return new CommandData();
			} else {
				commandData.type = CommandData.CommandType.TIMMY_USER;
			}
		} else if (message.charAt(0) == '$') {
			if (message.startsWith("$skynet")) {
				commandData.type = CommandData.CommandType.SKYNET_ADMIN;
			} else if (message.length() == 1) {
				return new CommandData();
			} else if (Pattern.matches("\\$(-?)\\d+.*", message)) {
				return new CommandData();
			} else {
				commandData.type = CommandData.CommandType.TIMMY_ADMIN;
			}
		}

		// Where's the first space of the string
		int space = message.indexOf(" ");

		if (space > 0) {
			commandData.command = message.substring(1, space).toLowerCase();
			commandData.args = message.substring(space + 1).split(" ", 0);
			commandData.argString = message.substring(space + 1);
		} else {
			commandData.command = message.substring(1).toLowerCase();
			commandData.argString = "";
		}

		return commandData;
	}
}
