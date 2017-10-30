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
			event.getUser().send().notice(str);
		}
	}

	private void issueChallenge(Channel channel, String target) {
		String[] challenges = {
			"Your character finds the only remaining %(item) on the entire planet. What do they do with it?",
			"Your character loses their prized %(item). How do they react?",
			"Your character dreams about %(item) and %(item.2).",
			"Introduce a character who worships %(deity).",
			"A new character arrives, wearing a%(acolor) piece of clothing, and carrying %(item).",
			"Your character has a sudden craving for %(flavor) ice-cream.",
			"Did your character just see %(pokemon)? Or was it something else?",
			"Your character sees a news report that %(number) of %(item) have disappeared. What's going on?",
		};

		TagReplacer tagReplacer = new TagReplacer();

		channel.send().action(String.format("challenges %s: %s", target, tagReplacer.doTagReplacment(challenges[Tim.rand.nextInt(challenges.length)])));
	}
}
