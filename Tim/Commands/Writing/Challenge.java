package Tim.Commands.Writing;

import Tim.ChannelInfo;
import Tim.Commands.CommandHandler;
import Tim.Commands.Utility.InteractionControls;
import Tim.Data.CommandData;
import Tim.Tim;
import Tim.Utility.TagReplacer;
import org.pircbotx.Channel;
import org.pircbotx.hooks.events.MessageEvent;

public class Challenge implements CommandHandler {
	@Override
	public boolean handleCommand(CommandData commandData) {
		ChannelInfo cdata = Tim.db.channel_data.get(commandData.getChannelEvent().getChannel().getName().toLowerCase());

		switch (commandData.command) {
			case "challenge":
				if (cdata.commands_enabled.get("challenge")) {
					issueChallenge(commandData.getChannelEvent().getChannel(), commandData.getUserEvent().getUser().getNick());
				} else {
					commandData.getMessageEvent().respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "challengefor":
				String[] args = commandData.args;
				if (cdata.commands_enabled.get("challenge")) {
					if (args != null && args.length >= 1) {
						if (InteractionControls.interactWithUser(args[0], "challenge")) {
							issueChallenge(commandData.getChannelEvent().getChannel(), args[0]);
						} else {
							commandData.getMessageEvent().respond("I'm sorry, it's been requested that I not do that.");
						}
					} else {
						commandData.getMessageEvent().respond("Usage: !challengefor <person>");
					}
				} else {
					commandData.getMessageEvent().respond("I'm sorry. I don't do that here.");
				}
				return true;
		}

		return false;
	}

	public void helpSection(MessageEvent event) {
		if (event.getUser() == null) {
			return;
		}

		String[] strs = {
			"Challenge Commands:",
			"    !challenge - Request a challenge",
			"    !challengefor <name> - Challenge someone else",
		};

		for (String str : strs) {
			event.getUser().send().message(str);
		}
	}

	private void issueChallenge(Channel channel, String target) {
		TagReplacer tagReplacer = new TagReplacer();

		channel.send().action(String.format("challenges %s: %s", target, tagReplacer.doTagReplacment(Tim.db.dynamic_lists.get("challenge_templates").get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("challenge_templates").size())))));
	}
}
