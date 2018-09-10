package Tim.Commands.Amusement;

import Tim.Commands.ICommandHandler;
import Tim.Data.CommandData;
import Tim.Data.UserData;
import Tim.Tim;

import java.util.HashSet;

public class Raptor implements ICommandHandler {
	private HashSet<String> handledCommands = new HashSet<>();
	
	public Raptor() {
		handledCommands.add("raptor");
	}
	
	@Override
	public boolean handleCommand(CommandData commandData) {
		if (handledCommands.contains(commandData.command)) {
			UserData userData = Tim.userDirectory.findUserData(commandData.issuer);

			if (userData == null) {
				commandData.event.respond("I'm sorry, you must be registered before you can work with raptors... (Please ensure you are registered and identified with NickServ.)");
			} else if (commandData.args.length < 1) {
				commandData.event.respond("Raptors have recently become fascinated by writing, so we have created a raptor adoption program. They will monitor your writing sprints, and share those details with you when asked. Don't worry, they probably won't eat your cat.");
				commandData.event.respond("Valid subcommands: adopt, release, details");
			} else {
				// TODO: Add renaming support.

				String subcommand = commandData.args[0];
				switch (subcommand) {
					case "adopt":
						if (userData.raptorAdopted) {
							commandData.event.respond("Due to safety regulations, you may only adopt a single raptor.");
						} else if (commandData.args.length < 2) {
							commandData.event.respond("You need to provide a name to adopt your new raptor.");
						} else {
							userData.raptorAdopted = true;
							userData.raptorName = commandData.args[1];
							userData.raptorFavoriteColor = Tim.db.dynamic_lists.get("color").get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("color").size()));
							userData.save();

							commandData.event.respond("Excellent! I've noted your raptor's name. Just so you know, their favorite color is " + userData.raptorFavoriteColor + ".");
						}
						break;

					case "release":
						if (!userData.raptorAdopted) {
							commandData.event.respond("I don't have any record of you having a raptor. Are you sure that isn't your cat?");
						} else {
							commandData.event.respond("I'll release " + userData.raptorName + " back into the wild. I'm sure they'll adjust well...");

							userData.raptorAdopted = false;
							userData.raptorName = null;
							userData.raptorFavoriteColor = null;
							userData.raptorBunniesStolen = 0;
							userData.lastBunnyRaid = null;
							userData.save();
						}
						break;

					case "details":
						commandData.event.respond("TODO");
						break;

					default:
						commandData.event.respond("Valid subcommands: adopt, release, details");
				}
			}
			
			return true;
		}
		
		return false;
	}
}
