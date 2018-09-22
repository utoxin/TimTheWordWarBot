package Tim.Data;

import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.hooks.types.GenericUserEvent;

public class CommandData<T extends GenericChannelEvent & GenericUserEvent & GenericMessageEvent> {
	public CommandType type      = CommandType.UNKNOWN;
	public String      issuer    = "";
	public String      command   = "";
	public String[]    args      = {};
	public String      argString = "";
	public T           event;

	public void setEvent(T event) {
		this.event = event;
	}

	public GenericMessageEvent getMessageEvent() {
		return event;
	}

	public GenericUserEvent getUserEvent() {
		return event;
	}

	public GenericChannelEvent getChannelEvent() {
		return event;
	}

	public String toString() {
		return String.format("Command: %s, Issuer: %s, argString: %s.", command, issuer, argString);
	}

	public enum CommandType {
		TIMMY_USER, TIMMY_ADMIN, SKYNET_USER, SKYNET_ADMIN, UNKNOWN
	}
}
