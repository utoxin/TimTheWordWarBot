package Tim.Commands;

import Tim.Data.CommandData;

public interface CommandHandler {
	boolean handleCommand(CommandData commandData);
}
