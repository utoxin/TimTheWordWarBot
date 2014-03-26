/**
 * This file is part of Timmy, the Wordwar Bot.
 *
 * Timmy is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Timmy is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Timmy. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package Tim;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Colors;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

/**
 *
 * @author Matthew Walker
 */
public class AdminCommandListener extends ListenerAdapter {

	@Override
	public void onMessage(MessageEvent event) {
		String message = Colors.removeFormattingAndColors(event.getMessage());

		if (message.charAt(0) == '$') {
			if (message.startsWith("$skynet")) {
				return;
			}

			if (Pattern.matches("\\$\\d+.*", message)) {
				event.respond("Thank you for your donation to my pizza fund!");
			} else if (Pattern.matches("\\$-\\d+.*", message)) {
				event.respond("No stealing from the pizza fund, or I'll report you to Skynet!");
			} else if (Tim.db.admin_list.contains(event.getUser().getNick().toLowerCase()) || Tim.db.admin_list.contains(event.getChannel().getName().toLowerCase())) {
				String command;
				String[] args = null;

				int space = message.indexOf(" ");
				if (space > 0) {
					command = message.substring(1, space).toLowerCase();
					args = message.substring(space + 1).split(" ", 0);
				} else {
					command = message.substring(1).toLowerCase();
				}
				switch (command) {
					case "setmuzzleflag":
						if (args != null && args.length == 2) {
							String target = args[0].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								boolean flag = false;
								if (!"0".equals(args[1])) {
									flag = true;
								}

								ChannelInfo ci = Tim.db.channel_data.get(target);
								ci.setMuzzleFlag(flag, false);

								event.respond("Channel muzzle flag updated for " + target);
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $setmuzzleflag <#channel> <0/1>");
						}
						break;
					case "automuzzlewars":
						if (args != null && args.length == 2) {
							String target = args[0].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								boolean flag = false;
								if (!"0".equals(args[1])) {
									flag = true;
								}

								ChannelInfo ci = Tim.db.channel_data.get(target);
								ci.setWarAutoMuzzle(flag);
								Tim.db.saveChannelSettings(ci);

								event.respond("Channel auto muzzle flag updated for " + target);
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $setmuzzleflag <#channel> <0/1>");
						}
						break;
					case "chatterlevel":
						if (args != null && args.length == 2 && args[1].equalsIgnoreCase("list")) {
							String target = args[0].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);
								event.respond("Reactive Chatter Level: " + ci.reactiveChatterLevel + "%/Msg - Name Multiplier: " + ci.chatterNameMultiplier);
								event.respond("Random Chatter Level: " + ci.randomChatterLevel + "%/Min");
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 4 && args[1].equalsIgnoreCase("reactive")) {
							String target = args[0].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								int level = Integer.parseInt(args[2]);
								int multi = Integer.parseInt(args[3]);

								if (level < 0 || level > 100) {
									event.respond("Chatter level must be between 0 and 100 (inclusive)");
								} else if (multi < 0 || level > 100) {
									event.respond("Name multiplier must be between 0 and 100 (inclusive)");
								} else {
									ChannelInfo ci = Tim.db.channel_data.get(target);
									ci.setReactiveChatter(level, multi);
									Tim.db.saveChannelSettings(ci);

									event.respond("Reactive chatter level updated for " + target);
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 3 && args[1].equalsIgnoreCase("random")) {
							String target = args[0].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								int level = Integer.parseInt(args[2]);

								if (level < 0 || level > 100) {
									event.respond("Chatter level must be between 0 and 100 (inclusive)");
								} else {
									ChannelInfo ci = Tim.db.channel_data.get(target);
									ci.setRandomChatter(level);
									Tim.db.saveChannelSettings(ci);

									event.respond("Random chatter level updated for " + target);
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $chatterlevel <#channel> list");
							event.respond("Usage: $chatterlevel <#channel> reactive <%/Msg> <Name Multiplier>");
							event.respond("Usage: $chatterlevel <#channel> random <%/Min>");
						}
						break;
					case "chatterflag":
						if (args != null && args.length == 2 && args[0].equalsIgnoreCase("list")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								event.respond("Sending status of chatter settings for " + target + " via private message.");

								for (String setting : ci.chatter_enabled.keySet()) {
									event.getUser().send().message(setting + ": " + ci.chatter_enabled.get(setting).toString());
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 4 && args[0].equalsIgnoreCase("set")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								boolean flag = false;
								if (!"0".equals(args[3])) {
									flag = true;
								}

								if (args[2].equalsIgnoreCase("all")) {
									for (String key : ci.chatter_enabled.keySet()) {
										ci.chatter_enabled.put(key, flag);
									}
									Tim.db.saveChannelSettings(ci);

									event.respond("All chatter flags updated.");
								} else {
									if (ci.chatter_enabled.containsKey(args[2])) {
										ci.chatter_enabled.put(args[2], flag);
										Tim.db.saveChannelSettings(ci);

										event.respond("Chatter flag updated.");
									} else {
										event.respond("I'm sorry, but I don't have a setting for " + args[2]);
									}
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $chatterflag list <#channel> OR $chatterflag set <#channel> <type> <0/1>");
							event.respond("Valid Chatter Types: all, chainstory, challenge, creeper, dance, defenestrate, eightball, foof, fridge, get, greetings, helpful_reactions, markov, search, sing, silly_reactions, summon");
						}
						break;
					case "commandflag":
						if (args != null && args.length == 2 && args[0].equalsIgnoreCase("list")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								event.respond("Sending status of command settings for " + target + " via private message.");

								for (String setting : ci.commands_enabled.keySet()) {
									event.getUser().send().message(setting + ": " + ci.commands_enabled.get(setting).toString());
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 4 && args[0].equalsIgnoreCase("set")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								boolean flag = false;
								if (!"0".equals(args[3])) {
									flag = true;
								}
								
								if (args[2].equalsIgnoreCase("all")) {
									for (String key : ci.commands_enabled.keySet()) {
										ci.commands_enabled.put(key, flag);
									}
									Tim.db.saveChannelSettings(ci);

									event.respond("All command flags updated.");
								} else {
									if (ci.commands_enabled.containsKey(args[2])) {
										ci.commands_enabled.put(args[2], flag);
										Tim.db.saveChannelSettings(ci);

										event.respond("Command flag updated.");
									} else {
										event.respond("I'm sorry, but I don't have a setting for " + args[2]);
									}
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $commandflag list <#channel> OR $commandflag set <#channel> <command> <0/1>");
							event.respond("Valid Commands: all, attack, chainstory, challenge, commandment, creeper, dance, defenestrate, dice, eightball, expound, foof, fridge, get, lick, markov, ping, search, sing, summon, woot");
						}
						break;
					case "twitterrelay":
						if (args != null && args.length == 2 && args[0].equalsIgnoreCase("list")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								event.respond("Sending Twitter accounts relayed for " + target + " via private message.");

								for (String account : ci.twitter_accounts) {
									event.getUser().send().message(account);
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 3 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								if (args[0].equalsIgnoreCase("add")) {
									if (Tim.twitterstream.checkAccount(args[2]) > 0) {
										event.respond("Twitter account added to channel's twitter feed. There may be a short delay (up to 90 seconds) before it takes effect.");
										ci.addTwitterAccount(args[2], true);
									} else {
										event.respond("I'm sorry, but that isn't a valid twitter account.");
									}
								} else {
									event.respond("Twitter account removed from local feed. There may be a short delay (up to 90 seconds) before it takes effect.");
									ci.removeTwitterAccount(args[2]);
								}
								Tim.db.saveChannelSettings(ci);
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $twitterrelay list <#channel> OR $twitterrelay <add/remove> <#channel> <account>");
							event.respond("Suggested Accounts: NaNoWriMo, NaNoWordSprints, officeduckfrank, BotTimmy.");
							event.respond("Note, no '@' before account names for this command.");
						}
						break;
					case "twitterbucket":
						if (args != null && args.length == 2 && args[0].equalsIgnoreCase("list")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								event.respond(String.format("Current Twitter Bucket settings for %s - Current Bucket: %.2f  Max Bucket: %.1f  Charge Rate / Minute: %.2f", target, ci.tweetBucket, ci.tweetBucketMax, ci.tweetBucketChargeRate));

								for (String account : ci.twitter_accounts) {
									event.respond(account);
								}
							} else {
								event.respond("I don't know about " + target);
							}
						} else if (args != null && args.length == 4 && args[0].equalsIgnoreCase("set")) {
							String target = args[1].toLowerCase();
							if (Tim.db.channel_data.containsKey(target)) {
								ChannelInfo ci = Tim.db.channel_data.get(target);

								Float max = Float.parseFloat(args[2]);
								Float charge = Float.parseFloat(args[3]);

								if (max > 0 && charge > 0) {
									ci.setTwitterTimers(max, charge);
									event.respond(String.format("Current Twitter Bucket settings for %s - Current Bucket: %.2f  Max Bucket: %.1f  Charge Rate / Minute: %.2f", target, ci.tweetBucket, ci.tweetBucketMax, ci.tweetBucketChargeRate));
								} else {
									event.respond("Max bucket and charge rate must both be greater than 0.");
								}

								Tim.db.saveChannelSettings(ci);
							} else {
								event.respond("I don't know about " + target);
							}
						} else {
							event.respond("Usage: $twitterbucket list <#channel> OR $twitterbucket set <#channel> <max bucket> <charge rate>");
						}
						break;
					case "shutdown":
						if (event.getUser().getNick().equalsIgnoreCase("Utoxin")) {
							event.respond("Shutting down...");
							Tim.shutdown();
						} else {
							event.respond("You're probably looking for the command '/kick Timmy'");
						}
						break;
					case "reload":
						event.respond("Reading database tables ...");
						Tim.db.refreshDbLists();
						event.respond("Tables reloaded.");
						break;
					case "ignore":
						if (args != null && args.length > 0) {
							String users = "";
							for (int i = 0; i < args.length; ++i) {
								users += " " + args[i];
								Tim.db.ignore_list.add(args[i]);
								Tim.db.saveIgnore(args[i], "hard");
							}
							event.respond("The following users have been ignored:" + users);
						} else {
							event.respond("Usage: $ignore <user 1> [ <user 2> [<user 3> [...] ] ]");
						}
						break;
					case "unignore":
						if (args != null && args.length > 0) {
							String users = "";
							for (int i = 0; i < args.length; ++i) {
								users += " " + args[i];
								Tim.db.ignore_list.remove(args[i]);
								Tim.db.soft_ignore_list.remove(args[i]);
								Tim.db.deleteIgnore(args[i]);
							}
							event.respond("The following users have been unignored:" + users);
						} else {
							event.respond("Usage: $unignore <user 1> [ <user 2> [<user 3> [...] ] ]");
						}
						break;
					case "listignores":
						event.respond("There are " + Tim.db.ignore_list.size() + " users ignored.");
						for (String item : Tim.db.ignore_list) {
							event.respond(item);
						}
						break;
					case "listbadwords":
						event.respond("There are " + Tim.markov.badwordPatterns.keySet().size() + " total bad words.");
						for (String key : Tim.markov.badwordPatterns.keySet()) {
							event.respond("Key: " + key + "  Pattern: " + Tim.markov.badwordPatterns.get(key).toString());
						}
						break;
					case "shout":
						for (ChannelInfo cdata : Tim.db.channel_data.values()) {
							Tim.bot.sendIRC().message(cdata.channel, event.getUser().getNick() + " shouts: " + StringUtils.join(args, " "));
						}
						break;
					case "help":
						this.printAdminCommandList(event);
						break;
					default:
						if (!(Tim.amusement.parseAdminCommand(event) || Tim.challenge.parseAdminCommand(event) || Tim.markov.parseAdminCommand(event))) {
							event.respond("$" + command + " is not a valid admin command - try $help");
						}
						break;
				}
			} else {
				event.respond("You are not an admin. Only Admins have access to that command.");
			}
		}
	}

	@Override
	public void onPrivateMessage(PrivateMessageEvent event) {
		String message = Colors.removeFormattingAndColors(event.getMessage());

		if (Tim.db.admin_list.contains(event.getUser().getNick().toLowerCase())) {
			String[] args = message.split(" ");
			if (args != null && args.length > 2) {
				String msg = "";
				for (int i = 2; i < args.length; i++) {
					msg += args[i] + " ";
				}
				if (args[0].equalsIgnoreCase("say")) {
					Tim.bot.sendIRC().message(args[1], msg);
				} else if (args[0].equalsIgnoreCase("act")) {
					Tim.bot.sendIRC().action(args[1], msg);
				}
			}
		}
	}

	private void printAdminCommandList(MessageEvent event) {
		event.getChannel().send().action("whispers something to " + event.getUser().getNick() + ". (Check for a new window or tab with the help text.)");

		String[] helplines = {"Core Admin Commands:",
							  "    $listitems [ <page #> ]   - lists all currently approved !get/!getfor items",
							  "    $listpending [ <page #> ] - lists all unapproved !get/!getfor items",
							  "    $approveitem <item #>     - removes item from pending list and marks as approved for !get/!getfor",
							  "    $disapproveitem <item #>  - removes item from approved list and marks as pending for !get/!getfor",
							  "    $deleteitem <item #>      - permanently removes an item from the pending list for !get/!getfor",
							  "    $ignore <username>        - Places user on the bot's ignore list",
							  "    $unignore <username>      - Removes user from bot's ignore list",
							  "    $listignores              - Prints the list of ignored users",
							  "Channel Setting Commands:",
							  "    $setmuzzleflag <#channel> <0/1>  - Sets the channel's current muzzle state",
							  "    $automuzzlewars <#channel> <0/1> - Whether to auto-muzzle the channel during wars.",
							  "    $chatterlevel                    - Set the chatter level for Timmy.",
							  "    $chatterflag                     - Control Timmy's chatter settings in your channel",
							  "    $commandflag                     - Control which commands can be used in your channel",
							  "    $twitterrelay                    - Control Timmy's twitter relays.",
							  "    $twitterbucket                   - Control the frequency of the twitter relays."
		};

		for (int i = 0; i < helplines.length; ++i) {
			event.getUser().send().notice(helplines[i]);
		}

		Tim.challenge.adminHelpSection(event);
		Tim.markov.adminHelpSection(event);
	}
}
