package Tim.Commands.Amusement;

import java.util.HashSet;

import Tim.Commands.ICommandHandler;
import Tim.Commands.Utility.InteractionControls;
import Tim.Data.ChannelInfo;
import Tim.Data.CommandData;
import Tim.Tim;
import Tim.Utility.TagReplacer;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Channel;

public class Summon implements ICommandHandler {
	private HashSet<String> handledCommands = new HashSet<>();

	public Summon() {
		handledCommands.add("summon");
		handledCommands.add("banish");
	}

	@Override
	public boolean handleCommand(CommandData commandData) {
		if (handledCommands.contains(commandData.command)) {
			ChannelInfo cdata = Tim.db.channel_data.get(commandData.event.getChannel()
																		 .getName()
																		 .toLowerCase());

			String nick = Tim.bot.getNick();

			if (commandData.issuer != null) {
				nick = commandData.issuer;
			}

			String target = String.join(" ", commandData.args);

			if (commandData.command.equalsIgnoreCase("banish") && cdata.commands_enabled.get("banish")) {
				if (InteractionControls.interactWithUser(target, "banish")) {
					banish(commandData.event.getChannel(), commandData.args, nick, true);
				} else {
					commandData.event.respond("I'm sorry, it's been requested that I not do that.");
				}
			} else if (commandData.command.equalsIgnoreCase("summon") && cdata.commands_enabled.get("summon")) {
				if (InteractionControls.interactWithUser(target, "summon")) {
					summon(commandData.event.getChannel(), commandData.args, nick, true);
				} else {
					commandData.event.respond("I'm sorry, it's been requested that I not do that.");
				}
			} else {
				commandData.event.respond("I'm sorry. I don't do that here.");
			}

			return true;
		}

		return false;
	}

	private void banish(Channel channel, String[] args, String banisher, Boolean righto) {
		try {
			String target;
			if (args == null || args.length == 0) {
				target = Tim.db.dynamic_lists.get("deity")
											 .get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("deity")
																					   .size()));
			} else {
				target = StringUtils.join(args, " ");
			}

			String target2 = Tim.db.dynamic_lists.get("deity")
												 .get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("deity")
																						   .size()));

			if (righto) {
				channel.send()
					   .message("Righto...");
			}

			Thread.sleep(Tim.rand.nextInt(1500) + 1500);
			channel.send()
				   .action(BanishStart(target, target2, banisher));

			Thread.sleep(Tim.rand.nextInt(3000) + 2000);
			channel.send()
				   .action(BanishEnd(target, target2, banisher));
		} catch (InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void summon(Channel channel, String[] args, String summoner, Boolean righto) {
		try {
			String target;
			if (args == null || args.length == 0) {
				target = Tim.db.dynamic_lists.get("deity")
											 .get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("deity")
																					   .size()));
			} else {
				target = StringUtils.join(args, " ");
			}

			String target2 = Tim.db.dynamic_lists.get("deity")
												 .get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("deity")
																						   .size()));

			if (righto) {
				channel.send()
					   .message("Righto...");
			}

			Thread.sleep(Tim.rand.nextInt(1500) + 1500);
			channel.send()
				   .action(SummonStart(target, target2, summoner));

			Thread.sleep(Tim.rand.nextInt(3000) + 2000);
			channel.send()
				   .action(SummonEnd(target, target2, summoner));
		} catch (InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private String BanishStart(String target, String alternate, String banisher) {
		String[] messages = {
			"gathers the supplies necessary to banish %(target) to the outer darkness...",
			"looks through an ancient scroll for the procedure to banish %(target)...",
			"writes a program called \"NoMore%(target).exe\" in an magical programming language...",
			"writes a program called \"Banish%(target).exe\" in an arcane programming language...",
			};

		TagReplacer tagReplacer = new TagReplacer();

		tagReplacer.setDynamicTag("target", target);
		tagReplacer.setDynamicTag("alternate", alternate);
		tagReplacer.setDynamicTag("banisher", banisher);

		return tagReplacer.doTagReplacment(messages[Tim.rand.nextInt(messages.length)]);
	}

	private String BanishEnd(String target, String alternate, String banisher) {
		String[] messages = {
			"completes the ritual successfully, banishing %(target) to the outer darkness, where they can't interfere with Timmy's affairs!",
			"completes the ritual to banish %(target), but they reappear after a short absence, looking a bit annoyed.",
			"attempts to banish %(target), but something goes horribly wrong. As the ritual is completed, %(alternate) appears to chastise %(banisher) for his"
			+ " temerity.", "fails completely in his attempt to banish %(target). They don't even seem to notice...",
			"succeeds in sending %(target) back to their home plane. Unfortunately, that plane is this one.",
			"must have messed up his attempt to banish %(target) pretty badly. %(banisher) disappears instead."
		};

		TagReplacer tagReplacer = new TagReplacer();

		tagReplacer.setDynamicTag("target", target);
		tagReplacer.setDynamicTag("alternate", alternate);
		tagReplacer.setDynamicTag("banisher", banisher);

		return tagReplacer.doTagReplacment(messages[Tim.rand.nextInt(messages.length)]);
	}

	private String SummonStart(String target, String alternate, String summoner) {
		String[] messages = {
			"prepares the summoning circle required to bring %(target) into the world...",
			"looks through an ancient manuscript for the procedure to summon %(target)...",
			"writes a program called \"Summon%(target).exe\" in an arcane programming language...",
			};

		TagReplacer tagReplacer = new TagReplacer();

		tagReplacer.setDynamicTag("target", target);
		tagReplacer.setDynamicTag("alternate", alternate);
		tagReplacer.setDynamicTag("summoner", summoner);

		return tagReplacer.doTagReplacment(messages[Tim.rand.nextInt(messages.length)]);
	}

	private String SummonEnd(String target, String alternate, String summoner) {
		String[] messages = {
			"completes the ritual successfully, drawing %(target) through, and binding them into the summoning circle!",
			"completes the ritual, drawing %(target) through, but something goes wrong and they fade away after just a few moments.",
			"attempts to summon %(target), but something goes horribly wrong. After the smoke clears, %(alternate) is left standing on the smoldering remains "
			+ "of the summoning circle.",
			"tries to figure out where he went wrong. He certainly didn't mean to summon %(alternate), and now they're pretty angry...",
			"succeeds at his attempt to summon %(target), but %(alternate) came along as well.",
			"fails to summon %(target). But a small note addressed to %(summoner) does appear. Unfortunately, it's written in a lost language..."
		};

		TagReplacer tagReplacer = new TagReplacer();

		tagReplacer.setDynamicTag("target", target);
		tagReplacer.setDynamicTag("alternate", alternate);
		tagReplacer.setDynamicTag("summoner", summoner);

		return tagReplacer.doTagReplacment(messages[Tim.rand.nextInt(messages.length)]);
	}

	public void summon(Channel channel) {
		summon(channel, null, Tim.bot.getNick(), false);
	}

	public void banish(Channel channel) {
		banish(channel, null, Tim.bot.getNick(), false);
	}
}
