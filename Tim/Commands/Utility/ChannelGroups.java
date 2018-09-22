package Tim.Commands.Utility;

import java.util.Set;
import java.util.stream.Collectors;

import Tim.Commands.IAdminCommand;
import Tim.Data.ChannelInfo;
import Tim.Tim;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.events.MessageEvent;

public final class ChannelGroups implements IAdminCommand {
	@Override
	public boolean parseAdminCommand(String command, String[] args, MessageEvent event) {
		if (args != null && args.length == 1 && args[0].equalsIgnoreCase("list")) {
			if (Tim.db.channel_groups.size() > 0) {
				event.respond("Channel Groups: " + StringUtils.join(Tim.db.channel_groups.keySet(), ", "));
			} else {
				event.respond("No known channel groups.");
			}
		} else if (args != null && args.length == 2 && args[0].equalsIgnoreCase("list")) {
			if (Tim.db.channel_groups.containsKey(args[1].toLowerCase())) {
				Set<ChannelInfo> channels = Tim.db.channel_groups.get(args[1].toLowerCase());
				if (channels.size() > 0) {
					event.respond("Channels in group: " + StringUtils.join(channels.stream()
																				   .map(c -> c.channel)
																				   .collect(Collectors.toList()), ", "));
				} else {
					event.respond("No channels in group.");
				}
			} else {
				event.respond("Unknown channel group.");
			}
		} else if (args != null && args.length == 3 && args[0].equalsIgnoreCase("add")) {
			String group       = args[1].toLowerCase();
			String channelName = args[2].toLowerCase();

			if (Tim.db.channel_data.containsKey(channelName)) {
				Tim.db.addToChannelGroup(group, Tim.db.channel_data.get(channelName));
				event.respond("Channel added to group '" + args[1] + "'.");
			} else {
				event.respond("Channel not found.");
			}
		} else if (args != null && args.length == 3 && args[0].equalsIgnoreCase("remove")) {
			String group       = args[1].toLowerCase();
			String channelName = args[2].toLowerCase();

			if (Tim.db.channel_groups.containsKey(group)) {
				if (Tim.db.channel_data.containsKey(channelName)) {
					Tim.db.addToChannelGroup(group, Tim.db.channel_data.get(channelName));
					event.respond("Channel removed from group.");
				} else {
					event.respond("Channel not found.");
				}
			} else {
				event.respond("Group not found.");
			}
		} else if (args != null && args.length == 2 && args[0].equalsIgnoreCase("destroy")) {
			String group = args[1].toLowerCase();

			if (Tim.db.channel_groups.containsKey(group)) {
				Tim.db.channel_groups.get(group)
									 .forEach(channel -> Tim.db.removeFromChannelGroup(group, channel));
				Tim.db.channel_groups.remove(group);
				event.respond("Channel group destroyed.");
			} else {
				event.respond("Group not found.");
			}
		} else {
			event.respond("Usage: $channelgroup list [<group>]");
			event.respond("Usage: $channelgroup add <group> <#channel>");
			event.respond("Usage: $channelgroup remove <group> <#channel>");
			event.respond("Usage: $channelgroup destroy <group>");
		}

		return true;
	}
}
