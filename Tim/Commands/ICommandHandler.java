package Tim.Commands;

import Tim.Data.CommandData;

public interface ICommandHandler {
	boolean handleCommand(CommandData commandData);
}
