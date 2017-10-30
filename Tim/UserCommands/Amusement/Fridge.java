package Tim.UserCommands.Amusement;

import Tim.ChannelInfo;
import Tim.Tim;
import Tim.UserCommands.UserCommandInterface;
import Tim.Utility.TagReplacer;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Fridge implements UserCommandInterface {
	public boolean parseCommand(String command, String[] args, MessageEvent event) {
		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());

		if (cdata.commands_enabled.get("fridge")) {
			throwFridge(event.getChannel(), event.getUser(), args, true);
		} else {
			event.respond("I'm sorry. I don't do that here.");
		}

		return true;
	}

	public void throwFridge(Channel channel, User sender, String[] args, Boolean righto) {
		try {
			String target = sender.getNick();
			if (args != null && args.length > 0) {
				target = StringUtils.join(args, " ");

				for (User t : channel.getUsers()) {
					if (t.getNick().toLowerCase().equals(target.toLowerCase())) {
						target = t.getNick();
						break;
					}
				}
			}

			if (righto) {
				channel.send().message("Righto...");
			}

			int time;

			time = Tim.rand.nextInt(1500) + 1500;
			Thread.sleep(time);
			channel.send().action(sneakyMessage());

			time = Tim.rand.nextInt(3000) + 2000;
			Thread.sleep(time);
			channel.send().action(throwMessage(target, sender.getNick()));
		} catch (InterruptedException ex) {
			Logger.getLogger(Fridge.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private String sneakyMessage() {
		String[] messages = {
			"looks back and forth, then slinks off...",
			"slips into the kitchen to grab something...",
			"places an order with the local appliance store..."
		};

		return messages[Tim.rand.nextInt(messages.length)];
	}

	private String throwMessage(String target, String thrower) {
		String[] messages = {
			"hurls a%(acolor) fridge at %(target).",
			"hurls a%(acolor) fridge at %(thrower) and runs away giggling.",
			"trips and drops a%(acolor) fridge on himself.",
			"rigs a complicated mechanism, and drops a%(acolor) fridge onto %(target)",
			"tries to build a complicated mechanism, but it breaks, and a%(acolor) fridge squishes him.",
			"picks the wrong target, and launches a%(acolor) fridge at %(thrower) with a trebuchet.",
			"grabs a%(acolor) fridge, but forgets to empty it first. What a mess!"
		};

		TagReplacer tagReplacer = new TagReplacer();

		tagReplacer.setDynamicTag("target", target);
		tagReplacer.setDynamicTag("thrower", thrower);

		return tagReplacer.doTagReplacment(messages[Tim.rand.nextInt(messages.length)]);
	}
}
