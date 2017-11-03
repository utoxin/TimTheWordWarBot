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

public class Defenestrate implements UserCommandInterface {
	@Override
	public boolean parseCommand(String command, String[] args, MessageEvent event) {
		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());

		if (cdata.commands_enabled.get("defenestrate")) {
			defenestrate(event.getChannel(), event.getUser(), args, true);
		} else {
			event.respond("I'm sorry. I don't do that here.");
		}

		return true;
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
			Logger.getLogger(Defenestrate.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

}
