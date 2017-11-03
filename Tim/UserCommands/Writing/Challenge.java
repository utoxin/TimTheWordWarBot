package Tim.UserCommands.Writing;

import Tim.ChannelInfo;
import Tim.Tim;
import Tim.UserCommands.UserCommandInterface;
import Tim.Utility.TagReplacer;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.hooks.events.MessageEvent;

public class Challenge implements UserCommandInterface {
	@Override
	public boolean parseCommand(String command, String[] args, MessageEvent event) {
		if (event.getUser() == null) {
			return false;
		}

		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());
		String message = Colors.removeFormattingAndColors(event.getMessage());
		String argsString = "";

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			argsString = message.substring(space + 1);
		} else {
			command = message.substring(1).toLowerCase();
		}

		switch (command) {
			case "challenge":
				if (cdata.commands_enabled.get("challenge")) {
					issueChallenge(event.getChannel(), event.getUser().getNick());
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;

			case "challengefor":
				if (cdata.commands_enabled.get("challenge")) {
					String target;
					space = argsString.indexOf(" ");
					if (space > 0) {
						target = argsString.substring(0, space);
					} else {
						target = argsString;
					}

					issueChallenge(event.getChannel(), target);
				} else {
					event.respond("I'm sorry. I don't do that here.");
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
