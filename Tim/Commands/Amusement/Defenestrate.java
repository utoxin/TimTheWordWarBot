package Tim.Commands.Amusement;

import Tim.Data.ChannelInfo;
import Tim.Commands.ICommandHandler;
import Tim.Commands.Utility.InteractionControls;
import Tim.Data.CommandData;
import Tim.Tim;
import Tim.Utility.Response;
import Tim.Utility.TagReplacer;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Channel;
import org.pircbotx.User;

import java.util.HashSet;

public class Defenestrate implements ICommandHandler {
	private HashSet<String> handledCommands = new HashSet<>();

	public Defenestrate() {
		handledCommands.add("defenestrate");
	}

	@Override
	public boolean handleCommand(CommandData commandData) {
		if (handledCommands.contains(commandData.command)) {
			ChannelInfo cdata = Tim.db.channel_data.get(commandData.event.getChannel().getName().toLowerCase());

			if (cdata.commands_enabled.get("defenestrate")) {
				if (
					((commandData.args == null || commandData.args.length == 0) && InteractionControls.interactWithUser(commandData.issuer, "defenestrate")) ||
						(commandData.args != null && commandData.args.length > 0 && InteractionControls.interactWithUser(commandData.args[0], "defenestrate"))) {
					defenestrate(commandData.event.getChannel(), commandData.getUserEvent().getUser(), commandData.args, true);
				} else {
					commandData.event.respond("I'm sorry, it's been requested that I not do that.");
				}
			} else {
				commandData.event.respond("I'm sorry. I don't do that here.");
			}

			return true;
		} else {
			return false;
		}
	}

	public void defenestrate(Channel channel, User sender, String[] args, Boolean righto) {
		try {
			String target = sender.getNick();
			if (args != null && args.length > 0) {
				target = StringUtils.join(args, " ");

				for (User t : Tim.db.channel_data.get(channel.getName().toLowerCase()).userList.values()) {
					if (t.getNick().toLowerCase().equals(target.toLowerCase())) {
						target = t.getNick();
						break;
					}
				}
			}

			if (!InteractionControls.interactWithUser(target, "defenestrate")) {
				Response.sendResponse(channel.getName(), sender.getNick(), "I'm sorry, it's been requested that I not do that.");
				return;
			}

			if (righto) {
				channel.send().message("Righto...");
			}

			int time;
			time = Tim.rand.nextInt(1500) + 1500;
			Thread.sleep(time);

			TagReplacer tagReplacer = new TagReplacer();

			String act;
			act = Tim.db.dynamic_lists.get("defenestration_starter").get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("defenestration_starter").size()));
			channel.send().action(tagReplacer.doTagReplacment(act));

			int i = Tim.rand.nextInt(100);

			if (i > 33) {
				tagReplacer.setDynamicTag("target", target);
				act = Tim.db.dynamic_lists.get("defenestration_success").get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("defenestration_success").size()));
			} else {
				tagReplacer.setDynamicTag("target", sender.getNick());
				act = Tim.db.dynamic_lists.get("defenestration_failure").get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("defenestration_failure").size()));
			}

			act = tagReplacer.doTagReplacment(act);

			time = Tim.rand.nextInt(3000) + 2000;
			Thread.sleep(time);
			channel.send().action(act);
		} catch (InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}
}
