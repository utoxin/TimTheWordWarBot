package Tim.UserCommands.Amusement;

import Tim.ChannelInfo;
import Tim.Tim;
import Tim.UserCommands.UserCommandInterface;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Fridge implements UserCommandInterface {
	public void parseCommand(String command, String[] args, MessageEvent event) {
		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());

		if (cdata.commands_enabled.get("fridge")) {
			throwFridge(event.getChannel(), event.getUser(), args, true);
		} else {
			event.respond("I'm sorry. I don't do that here.");
		}
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

			String colour = Tim.db.colours.get(Tim.rand.nextInt(Tim.db.colours.size()));
			switch (colour.charAt(0)) {
				case 'a':
				case 'e':
				case 'i':
				case 'o':
				case 'u':
					colour = "n " + colour;
					break;
				default:
					colour = " " + colour;
			}

			time = Tim.rand.nextInt(3000) + 2000;
			Thread.sleep(time);

			channel.send().action(throwMessage(colour, target, sender.getNick()));
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

	private String throwMessage(String color, String target, String thrower) {
		String[] messages = {
			"hurls a%(color) fridge at %(target).",
			"hurls a%(color) fridge at %(thrower) and runs away giggling.",
			"trips and drops a%(color) fridge on himself.",
			"rigs a complicated mechanism, and drops a%(color) fridge onto %(target)",
			"tries to build a complicated mechanism, but it breaks, and a%(color) fridge squishes him.",
			"picks the wrong target, and launches a%(color) fridge at %(thrower) with a trebuchet.",
			"grabs a%(color) fridge, but forgets to empty it first. What a mess!"
		};

		Map<String, String> values = new HashMap<>();
		values.put("color", color);
		values.put("target", target);
		values.put("thrower", thrower);

		StrSubstitutor sub = new StrSubstitutor(values, "%(", ")");
		return sub.replace(messages[Tim.rand.nextInt(messages.length)]);
	}
}
