package Tim;

/*
 * Copyright (C) 2015 mwalker
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

import Tim.Commands.Amusement.Defenestrate;
import Tim.Commands.Amusement.Dice;
import Tim.Commands.Amusement.Fridge;
import Tim.Commands.Amusement.Summon;
import Tim.Commands.ICommandHandler;
import Tim.Commands.Utility.InteractionControls;
import Tim.Data.ChannelInfo;
import Tim.Data.CommandData;
import Tim.Utility.TagReplacer;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

/**
 * @author mwalker
 */
class Amusement implements ICommandHandler {
	private final long timeout = 3000;

	private final List<String> pendingItems = new ArrayList<>();
	private final List<String> commandments = new ArrayList<>();

	List<String> ponderingList = new ArrayList<>();

	private ChannelInfo cdata;

	private Fridge       fridge       = new Fridge();
	private Summon       summon       = new Summon();
	private Defenestrate defenestrate = new Defenestrate();
	private TagReplacer  tagReplacer  = new TagReplacer();

	private ICommandHandler[] commandHandlers = {
		new Dice()
	};

	@Override
	public boolean handleCommand(CommandData commandData) {
		cdata = Tim.db.channel_data.get(commandData.getChannelEvent()
												   .getChannel()
												   .getName()
												   .toLowerCase());
		String   command = commandData.command;
		String[] args    = commandData.args;

		switch (command) {
			case "attack":
				if (cdata.commands_enabled.get("attack")) {
					if (args != null && args.length > 0) {
						StringBuilder target = new StringBuilder(args[0]);
						for (int i = 1; i < args.length; ++i) {
							target.append(" ")
								  .append(args[i]);
						}

						if (InteractionControls.interactWithUser(target.toString(), "attack")) {
							attackCommand(commandData.getChannelEvent()
													 .getChannel(), commandData.getUserEvent()
																			   .getUser(), target.toString());
						} else {
							commandData.getMessageEvent()
									   .respond("I'm sorry, it's been requested that I not do that.");
						}
					} else {
						if (InteractionControls.interactWithUser(commandData.issuer, "attack")) {
							attackCommand(commandData.getChannelEvent()
													 .getChannel(), commandData.getUserEvent()
																			   .getUser(), null);
						} else {
							commandData.getMessageEvent()
									   .respond("I'm sorry, it's been requested that I not do that.");
						}
					}
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}

				return true;

			case "boxodoom":
				boxodoom(commandData.getMessageEvent(), args);
				return true;

			case "catch":
				if (cdata.commands_enabled.get("catch")) {
					String target = String.join(" ", args);

					if (InteractionControls.interactWithUser(target, "catch")) {
						catchCommand(commandData.getChannelEvent()
												.getChannel(), args);
					} else {
						commandData.getMessageEvent()
								   .respond("I'm sorry, it's been requested that I not do that.");
					}
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "commandment":
				if (cdata.commands_enabled.get("commandment")) {
					commandment(commandData.getChannelEvent()
										   .getChannel(), args);
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "dance":
				if (cdata.commands_enabled.get("dance")) {
					dance(commandData.getChannelEvent()
									 .getChannel());
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "eightball":
				if (cdata.commands_enabled.get("eightball")) {
					eightball(commandData.getChannelEvent()
										 .getChannel(), commandData.getUserEvent()
																   .getUser(), false, commandData.argString);
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "expound":
				if (cdata.commands_enabled.get("expound")) {
					int    which = Tim.rand.nextInt(100);
					String type  = "say";
					if (which < 5 || (which < 10 && !commandData.argString.equals(""))) {
						type = "mutter";
					} else if (which < 20) {
						type = "emote";
					} else if (which < 40) {
						type = "novel";
					}

					Tim.markov.randomAction(commandData.getChannelEvent()
													   .getChannel()
													   .getName(), type, commandData.argString);
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "foof":
				if (cdata.commands_enabled.get("foof")) {
					if (InteractionControls.interactWithUser(commandData.argString, "foof")) {
						foof(commandData.getChannelEvent()
										.getChannel(), commandData.getUserEvent()
																  .getUser(), args, true);
					} else {
						commandData.getMessageEvent()
								   .respond("I'm sorry, it's been requested that I not do that.");
					}
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "get":
				if (cdata.commands_enabled.get("get")) {
					getItem(commandData.getChannelEvent()
									   .getChannel(), commandData.issuer, args);
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "getfor":
				if (args != null && args.length > 0) {
					if (cdata.commands_enabled.get("get")) {
						if (InteractionControls.interactWithUser(commandData.args[0], "get")) {
							if (args.length > 1) {
								// Want a new args array less the first old element.
								String[] newargs = new String[args.length - 1];
								System.arraycopy(args, 1, newargs, 0, args.length - 1);
								getItem(commandData.getChannelEvent()
												   .getChannel(), args[0], newargs);
							} else {
								getItem(commandData.getChannelEvent()
												   .getChannel(), args[0], null);
							}
						} else {
							commandData.getMessageEvent()
									   .respond("I'm sorry, it's been requested that I not do that.");
						}
					} else {
						commandData.getMessageEvent()
								   .respond("I'm sorry. I don't do that here.");
					}
				} else {
					commandData.event.respond("Syntax: !getfor <user> [<item>]");
				}
				return true;

			case "getfrom":
				if (args != null && args.length > 0) {
					if (cdata.commands_enabled.get("get")) {
						if (InteractionControls.interactWithUser(commandData.args[0], "get")) {
							if (args.length > 1) {
								// Want a new args array less the first old element.
								String[] newargs = new String[args.length - 1];
								System.arraycopy(args, 1, newargs, 0, args.length - 1);
								getItemFrom(commandData.getChannelEvent()
													   .getChannel(), commandData.issuer, args[0], newargs);
							} else {
								getItemFrom(commandData.getChannelEvent()
													   .getChannel(), commandData.issuer, args[0], null);
							}
						} else {
							commandData.getMessageEvent()
									   .respond("I'm sorry, it's been requested that I not do that.");
						}
					} else {
						commandData.getMessageEvent()
								   .respond("I'm sorry. I don't do that here.");
					}
				} else {
					commandData.event.respond("Syntax: !getfrom <user> [<item>]");
				}
				return true;

			case "herd":
				if (cdata.commands_enabled.get("herd")) {
					if (InteractionControls.interactWithUser(commandData.argString, "herd")) {
						herd(commandData.getChannelEvent()
										.getChannel(), commandData.getUserEvent()
																  .getUser(), args);
					} else {
						commandData.getMessageEvent()
								   .respond("I'm sorry, it's been requested that I not do that.");
					}
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "lick":
				if (cdata.commands_enabled.get("lick")) {
					if (InteractionControls.interactWithUser(commandData.argString, "lick")) {
						lick(commandData);
					} else {
						commandData.getMessageEvent()
								   .respond("I'm sorry, it's been requested that I not do that.");
					}
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "pickone":
				pickone(commandData.getMessageEvent(), args);
				return true;

			case "ping":
				if (cdata.commands_enabled.get("ping")) {
					if (Tim.rand.nextInt(100) < 80) {
						commandData.getMessageEvent()
								   .respond("Pong!");
					} else {
						commandData.getChannelEvent()
								   .getChannel()
								   .send()
								   .action(tagReplacer.doTagReplacment("dives for the ball, but misses, and lands on a%(acolor) couch."));
					}
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "raptorstats":
				if (cdata.commands_enabled.get("velociraptor")) {
					commandData.getMessageEvent()
							   .respond(String.format("There have been %d velociraptor sightings in this channel, and %d are still here.",
													  cdata.velociraptorSightings, cdata.activeVelociraptors));
					commandData.getMessageEvent()
							   .respond(String.format("%d velociraptors in this channel have been killed by other swarms.", cdata.deadVelociraptors));
					commandData.getMessageEvent()
							   .respond(String.format("Swarms from this channel have killed %d other velociraptors.", cdata.killedVelociraptors));
					if (cdata.raptorStrengthBoost > 0) {
						commandData.getMessageEvent().respond(String.format("It looks like the training you've been doing has helped organize the local raptors. The area can now support %d of them.", 100 + cdata.raptorStrengthBoost));
					}
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "search":
				if (cdata.commands_enabled.get("search")) {
					if (args != null && args.length > 0) {
						if (InteractionControls.interactWithUser(commandData.argString, "search")) {
							StringBuilder target = new StringBuilder(args[0]);
							for (int i = 1; i < args.length; ++i) {
								target.append(" ")
									  .append(args[i]);
							}
							search(commandData.getChannelEvent()
											  .getChannel(), commandData.getUserEvent()
																		.getUser(), target.toString());
						} else {
							commandData.getMessageEvent()
									   .respond("I'm sorry, it's been requested that I not do that.");
						}
					} else {
						search(commandData.getChannelEvent()
										  .getChannel(), commandData.getUserEvent()
																	.getUser(), null);
					}
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "sing":
				if (cdata.commands_enabled.get("sing")) {
					sing(commandData.getChannelEvent()
									.getChannel());
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			case "woot":
				if (cdata.commands_enabled.get("woot")) {
					commandData.getChannelEvent()
							   .getChannel()
							   .send()
							   .action("cheers! Hooray!");
				} else {
					commandData.getMessageEvent()
							   .respond("I'm sorry. I don't do that here.");
				}
				return true;

			default:
				boolean commandHandled = false;

				for (ICommandHandler handler : this.commandHandlers) {
					if (handler.handleCommand(commandData)) {
						commandHandled = true;
						break;
					}
				}

				return commandHandled;
		}
	}

	private void attackCommand(Channel channel, User sender, String target) {
		DecimalFormat formatter = new DecimalFormat("#,###");
		String        item      = Tim.db.items.get(Tim.rand.nextInt(Tim.db.items.size()));

		if (target != null && Tim.rand.nextInt(100) > 75) {
			channel.send()
				   .action(String.format("decides he likes %s, so he attacks %s instead...", target, sender.getNick()));
			target = sender.getNick();
		} else if (target == null) {
			target = sender.getNick();
		}

		int damage;

		switch (Tim.rand.nextInt(8)) {
			case 2:
			case 3:
				damage = Tim.rand.nextInt(100);
				break;
			case 4:
			case 5:
				damage = Tim.rand.nextInt(1000);
				break;
			case 6:
				damage = Tim.rand.nextInt(10000);
				break;
			case 7:
				damage = Tim.rand.nextInt(100000);
				break;
			default:
				damage = Tim.rand.nextInt(10);
		}

		String damageString;
		if (damage > 9000) {
			damageString = "OVER 9000";
		} else {
			damageString = formatter.format(damage);
		}

		channel.send()
			   .action(String.format("hits %s with %s for %s points of damage.", target, item, damageString));
	}

	private void boxodoom(GenericMessageEvent event, String[] args) {
		int    goal;
		long   duration, base_wpm;
		double modifier;

		String     difficulty = "", original_difficulty = "";
		Connection con;

		if (args == null || args.length != 2) {
			event.respond("!boxodoom requires two parameters.");
			return;
		}

		try {
			if (Pattern.matches("(?i)((extra|super)?easy)|average|medium|normal|hard|extreme|insane|impossible|tadiera", args[0])) {
				original_difficulty = difficulty = args[0].toLowerCase();
				duration = (long) Double.parseDouble(args[1]);
			} else if (Pattern.matches("(?i)((extra|super)?easy)|average|medium|normal|hard|extreme|insane|impossible|tadiera", args[1])) {
				original_difficulty = difficulty = args[1].toLowerCase();
				duration = (long) Double.parseDouble(args[0]);
			} else {
				event.respond("Difficulty must be one of: extraeasy, easy, average, hard, extreme, insane, impossible, tadiera");
				return;
			}
		} catch (NumberFormatException ex) {
			duration = 0;
		}

		if (duration < 1) {
			event.respond("Duration must be greater than or equal to 1.");
			return;
		}

		switch (difficulty) {
			case "extraeasy":
			case "supereasy":
				difficulty = "easy";
				break;
			case "medium":
			case "normal":
				difficulty = "average";
				break;
			case "extreme":
			case "insane":
			case "impossible":
			case "tadiera":
				difficulty = "hard";
				break;
		}

		String value = "";
		try {
			con = Tim.db.pool.getConnection(timeout);
			PreparedStatement s = con.prepareStatement("SELECT `challenge` FROM `box_of_doom` WHERE `difficulty` = ? ORDER BY rand() LIMIT 1");
			s.setString(1, difficulty);
			s.executeQuery();

			ResultSet rs = s.getResultSet();
			while (rs.next()) {
				value = rs.getString("challenge");
			}

			con.close();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}

		base_wpm = (long) Double.parseDouble(value);
		switch (original_difficulty) {
			case "extraeasy":
			case "supereasy":
				base_wpm *= 0.65;
				break;
			case "extreme":
				base_wpm *= 1.4;
				break;
			case "insane":
				base_wpm *= 1.8;
				break;
			case "impossible":
				base_wpm *= 2.2;
				break;
			case "tadiera":
				base_wpm *= 3;
				break;
		}

		modifier = 1.0 / Math.log(duration + 1.0) / 1.5 + 0.68;
		goal = (int) (duration * base_wpm * modifier / 10) * 10;

		event.respond("Your goal is " + goal);
	}

	private void catchCommand(Channel channel, String[] args) {
		try {
			String target;
			if (args == null || args.length == 0) {
				target = Tim.db.dynamic_lists.get("pokemon")
											 .get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("pokemon")
																					   .size()));
			} else {
				target = StringUtils.join(args, " ");
			}

			tagReplacer.setDynamicTag("target", target);

			channel.send()
				   .action(tagReplacer.doTagReplacment("grabs a%(acolor) and %(color.2) pokeball, and attempts to catch %(target)!"));

			int    i = Tim.rand.nextInt(100);
			String act;

			if (i > 65) {
				act = tagReplacer.doTagReplacment("catches %(target), and adds them to his collection!");
			} else if (i > 50) {
				act = tagReplacer.doTagReplacment("almost catches %(target), but they manage to break out of the pokeball and escape!");
			} else if (i > 25) {
				String target2;
				do {
					target2 = Tim.db.dynamic_lists.get("pokemon")
												  .get(Tim.rand.nextInt(Tim.db.dynamic_lists.get("pokemon")
																							.size()));
				} while (Objects.equals(target2, target));
				tagReplacer.setDynamicTag("target2", target2);

				act = tagReplacer.doTagReplacment("somehow misses %(target), and his pokeball captures %(target2) instead. Oops!");
			} else {
				act = "misses his throw completely, and catches nothing. Awwwww.";
			}

			Thread.sleep(1000);
			channel.send()
				   .action(act);
		} catch (InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void commandment(Channel channel, String[] args) {
		int r = Tim.rand.nextInt(this.commandments.size());
		if (args != null && args.length == 1 && Double.parseDouble(args[0]) > 0 && Double.parseDouble(args[0]) <= this.commandments.size()) {
			r = (int) Double.parseDouble(args[0]) - 1;
		}

		channel.send()
			   .message(this.commandments.get(r));
	}

	private void dance(Channel channel) {
		Connection con;
		int        r = Tim.rand.nextInt(100);

		String response;
		if (r > 90) {
			response = "dances the %s so well he should be on Dancing with the Stars!";
		} else if (r > 60) {
			response = "does the %s, and tears up the dance floor.";
		} else if (r > 30) {
			response = "attempts to do the %s, but obviously needs more practice.";
		} else {
			response = "flails about in a fashion that vaguely resembles the %s. Sort of.";
		}

		try {
			con = Tim.db.pool.getConnection(timeout);
			PreparedStatement danceName = con.prepareStatement("SELECT name FROM dances ORDER BY rand() LIMIT 1");
			ResultSet         danceNameRes;

			danceNameRes = danceName.executeQuery();
			danceNameRes.next();

			int delay = Tim.rand.nextInt(500) + 500;
			Thread.sleep(delay);
			channel.send()
				   .action(String.format(response, danceNameRes.getString("name")));
			con.close();
		} catch (SQLException | InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void eightball(Channel channel, User sender, boolean mutter, String argStr) {
		try {
			int r     = Tim.rand.nextInt(Tim.db.eightBalls.size());
			int delay = Tim.rand.nextInt(1000) + 1000;
			Thread.sleep(delay);

			if (mutter) {
				channel.send()
					   .action("mutters under his breath, \"" + Tim.db.eightBalls.get(r) + "\"");
			} else {
				if (Tim.rand.nextInt(100) < 5) {
					channel.send()
						   .message(sender.getNick() + ": " + Tim.markov.generate_markov("say", argStr));
				} else {
					channel.send()
						   .message(sender.getNick() + ": " + Tim.db.eightBalls.get(r));
				}
			}
		} catch (InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void foof(Channel channel, User sender, String[] args, Boolean righto) {
		try {
			String target = sender.getNick();
			if (args != null && args.length > 0) {
				target = StringUtils.join(args, " ");
			}

			if (righto) {
				channel.send()
					   .message("Righto...");
			}

			int time = Tim.rand.nextInt(1500) + 1500;
			Thread.sleep(time);
			channel.send()
				   .action("surreptitiously works his way over to the couch, looking ever so casual...");
			int    i = Tim.rand.nextInt(100);
			String act;

			if (i > 33) {
				act = "grabs a%(acolor) pillow, and throws it at %(target), hitting them squarely in the back of the head.";
			} else if (i > 11) {
				target = sender.getNick();
				act = "laughs maniacally then throws a%(acolor) pillow at %(target), then runs off and hides behind the nearest couch.";
			} else {
				act = "trips and lands on a%(acolor) pillow. Oof!";
			}

			tagReplacer.setDynamicTag("target", target);
			act = tagReplacer.doTagReplacment(act);

			time = Tim.rand.nextInt(3000) + 2000;
			Thread.sleep(time);
			channel.send()
				   .action(act);
		} catch (InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getItem(Channel channel, String target, String[] args) {
		StringBuilder item = new StringBuilder();
		if (args != null && args.length > 0) {
			if (Tim.rand.nextInt(100) < 65) {
				item = new StringBuilder(args[0]);
				for (int i = 1; i < args.length; ++i) {
					item.append(" ")
						.append(args[i]);
				}

				if (!(Tim.db.items.contains(item.toString()) || this.pendingItems.contains(item.toString())) && item.length() < 300) {
					this.insertPendingItem(item.toString());
					this.pendingItems.add(item.toString());
				}

				if (item.toString()
						.toLowerCase()
						.contains("spoon")) {
					item = new StringBuilder();
					channel.send()
						   .action("rummages around in the back room for a bit, then calls out. \"Sorry... there is no spoon. Maybe this will do...\"");
				} else if (Tim.rand.nextInt(100) > 10 && Pattern.matches("(\\W|^)rum(\\W|$)", item.toString()
																								  .toLowerCase())) {
					item = new StringBuilder();
					channel.send()
						   .action("rummages around in the back room for a bit, then calls out. \"All the rum is gone. I have this instead...\"");
				}
			} else {
				channel.send()
					   .action("rummages around in the back room for a bit, then calls out. \"Sorry... I don't think I have that. Maybe this will do...\"");
			}
		}

		if (item.length() == 0) {
			// Find a random item.
			int i = Tim.rand.nextInt(Tim.db.items.size());
			item = new StringBuilder(Tim.db.items.get(i));
		}

		channel.send()
			   .action(String.format("gets %s %s.", target, item.toString()));
	}

	private void getItemFrom(Channel channel, String recipient, String target, String[] args) {
		StringBuilder item = new StringBuilder();
		if (args != null) {
			if (Tim.rand.nextInt(100) < 65) {
				item = new StringBuilder(args[0]);
				for (int i = 1; i < args.length; ++i) {
					item.append(" ")
						.append(args[i]);
				}

				if (!(Tim.db.items.contains(item.toString()) || this.pendingItems.contains(item.toString())) && item.length() < 300) {
					this.insertPendingItem(item.toString());
					this.pendingItems.add(item.toString());
				}

				if (item.toString()
						.toLowerCase()
						.contains("spoon")) {
					item = new StringBuilder();
					channel.send()
						   .action("rummages around in " + target
								   + "'s things for a bit, then calls out. \"Sorry... there is no spoon. But I did find something else...\"");
				} else if (Tim.rand.nextInt(100) > 10 && Pattern.matches("(\\W|^)rum(\\W|$)", item.toString()
																								  .toLowerCase())) {
					item = new StringBuilder();
					channel.send()
						   .action("rummages around in the back room for a bit, then calls out. \"All the rum is gone. But they did have this...\"");
				}
			} else {
				channel.send()
					   .action("rummages around in " + target
							   + "'s things for a bit, then calls out. \"Sorry... I don't think they have that. But I did find something else...\"");
			}
		}

		if (item.length() == 0) {
			// Find a random item.
			int i = Tim.rand.nextInt(Tim.db.items.size());
			item = new StringBuilder(Tim.db.items.get(i));
		}

		channel.send()
			   .action(String.format("takes %s from %s, and gives it to %s.", item.toString(), target, recipient));
	}

	private void herd(Channel channel, User sender, String[] args) {
		try {
			String target = sender.getNick();
			if (args != null && args.length > 0) {
				target = StringUtils.join(args, " ");

				for (User t : Tim.db.channel_data.get(channel.getName()
															 .toLowerCase()).userList.values()) {
					if (t.getNick()
						 .toLowerCase()
						 .equals(target.toLowerCase())) {
						target = t.getNick();
						break;
					}
				}
			}

			int time;
			time = Tim.rand.nextInt(1000) + 500;
			Thread.sleep(time);
			channel.send()
				   .action(tagReplacer.doTagReplacment("collects several %(color) boxes, and lays them around to attract cats..."));

			int    i    = Tim.rand.nextInt(100);
			String herd = Tim.db.cat_herds.get(Tim.rand.nextInt(Tim.db.cat_herds.size()));
			String act;

			if (i > 33) {
				act = String.format(herd, target, target);
			} else if (i > 11) {
				target = sender.getNick();
				act = String.format("gets confused and " + herd, target, target);
			} else {
				act = "can't seem to find any cats. Maybe he used the wrong color of box?";
			}

			time = Tim.rand.nextInt(3000) + 2000;
			Thread.sleep(time);
			channel.send()
				   .action(act);
		} catch (InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void lick(CommandData commandData) {
		if (commandData.args != null && commandData.args.length >= 1) {
			String argStr = StringUtils.join(commandData.args, " ");

			commandData.getChannelEvent()
					   .getChannel()
					   .send()
					   .action("licks " + argStr + ". Tastes like " + Tim.db.flavours.get(Tim.rand.nextInt(Tim.db.flavours.size())));
		} else if (commandData.getUserEvent()
							  .getUser() != null) {
			commandData.getChannelEvent()
					   .getChannel()
					   .send()
					   .action("licks " + commandData.getUserEvent()
													 .getUser()
													 .getNick() + "! Tastes like " + Tim.db.flavours.get(Tim.rand.nextInt(Tim.db.flavours.size())));
		}
	}

	private void pickone(GenericMessageEvent event, String[] args) {
		String   argStr  = StringUtils.join(args, " ");
		String[] choices = argStr.split(",", 0);

		if (choices.length < 1) {
			event.respond("I don't see any choices there!");
		} else if (choices.length == 1) {
			event.respond("You only gave one option, silly!");
		} else {
			int r = Tim.rand.nextInt(choices.length);
			event.respond("I pick... " + choices[r]);
		}
	}

	private void search(Channel channel, User sender, String target) {
		StringBuilder item = new StringBuilder();

		int count = Tim.rand.nextInt(4);
		if (count == 1) {
			item = new StringBuilder(Tim.db.items.get(Tim.rand.nextInt(Tim.db.items.size())));
		} else if (count > 1) {
			for (int i = 0; i < count; i++) {
				if (i > 0 && count > 2) {
					item.append(",");
				}

				if (i == (count - 1)) {
					item.append(" and ");
				} else if (i > 0) {
					item.append(" ");
				}

				item.append(Tim.db.items.get(Tim.rand.nextInt(Tim.db.items.size())));
			}
		}

		if (target != null && Tim.rand.nextInt(100) > 75) {
			channel.send()
				   .action("decides at the last second to search " + sender.getNick() + "'s things instead...");
			target = sender.getNick();
		} else {
			if (target == null) {
				target = sender.getNick();
			}

			channel.send()
				   .action("searches through " + target + "'s things, looking for contraband...");
		}

		if (item.toString()
				.equals("")) {
			channel.send()
				   .action(String.format("can't find anything, and grudgingly clears %s.", target));
		} else {
			channel.send()
				   .action(String.format("reports %s to Skynet for possesion of %s.", target, item.toString()));
		}
	}

	private void sing(Channel channel) {
		Connection con;
		int        r = Tim.rand.nextInt(100);

		String response;
		if (r > 90) {
			response = "sings the well known song '%s' better than the original artist!";
		} else if (r > 60) {
			response = "chants some obscure lyrics from '%s'. At least you think that's the name of the song...";
		} else if (r > 30) {
			response = "starts singing '%s'. You've heard better...";
		} else {
			response = "screeches out some words from '%s', and all the nearby windows shatter... Ouch.";
		}

		try {
			con = Tim.db.pool.getConnection(timeout);
			PreparedStatement songNameQuery = con.prepareStatement("SELECT name FROM songs ORDER BY rand() LIMIT 1");
			ResultSet         songNameRes;

			songNameRes = songNameQuery.executeQuery();
			songNameRes.next();

			String songName = songNameRes.getString("name");
			con.close();

			r = Tim.rand.nextInt(500) + 500;
			Thread.sleep(r);
			channel.send()
				   .action(String.format(response, songName));
		} catch (SQLException | InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void insertPendingItem(String item) {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `items` (`item`, `approved`) VALUES (?, FALSE)");
			s.setString(1, item);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void helpSection(MessageEvent event) {
		if (event.getUser() == null) {
			return;
		}

		String[] strs = {
			"Amusement Commands:", "    !get <anything> - I will fetch you whatever you like.",
			"    !getfor <someone> <anything> - I will give someone whatever you like.",
			"    !eightball <your question> - I can tell you (with some degree of inaccuracy) how likely something is.",
			"    !raptorstats - Details about this channel's raptor activity.",
			};

		for (String str : strs) {
			event.getUser()
				 .send()
				 .message(str);
		}
	}

	void refreshDbLists() {
		this.getAypwipList();
		this.getCommandmentList();
		this.getPendingItems();
	}

	private void getAypwipList() {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `aypwips`");

			ResultSet rs = s.getResultSet();
			this.ponderingList.clear();
			while (rs.next()) {
				this.ponderingList.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getCommandmentList() {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `commandments`");

			ResultSet rs = s.getResultSet();
			this.commandments.clear();
			while (rs.next()) {
				this.commandments.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	private void getPendingItems() {
		Connection con;
		String     value;
		this.pendingItems.clear();

		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s  = con.prepareStatement("SELECT `item` FROM `items` WHERE `approved` = FALSE");
			ResultSet         rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("item");
				this.pendingItems.add(value);
			}

			con.close();
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}

	void randomAction(User sender, String channel) {
		cdata = Tim.db.channel_data.get(channel);

		String[] actions = new String[]{
			"get", "eightball", "fridge", "defenestrate", "sing", "foof", "dance", "summon", "search", "herd", "banish", "catch"
		};

		if (sender == null) {
			HashSet<User> finalUsers = new HashSet<>(10);

			Collection<User> users = Tim.db.channel_data.get(channel).userList.values();

			int size = users.size();
			users.stream()
				 .filter((user) -> (!user.getNick()
										 .equalsIgnoreCase("Timmy") && (size <= 2 || !user.getNick()
																						  .equalsIgnoreCase("Skynet")) && !Tim.db.ignore_list.contains(
					 user.getNick()
						 .toLowerCase()) && !Tim.db.soft_ignore_list.contains(user.getNick()
																				  .toLowerCase())))
				 .forEach(finalUsers::add);

			if (finalUsers.size() > 0) {
				int r = Tim.rand.nextInt(finalUsers.size());
				int i = 0;

				for (User user : finalUsers) {
					if (i == r) {
						sender = user;
					}
					i++;
				}
			} else {
				actions = new String[]{
					"eightball", "sing", "dance", "summon"
				};
			}
		}

		Set<String> enabled_actions = new HashSet<>(16);
		for (String action : actions) {
			if (cdata.chatter_enabled.get(action)) {
				enabled_actions.add(action);
			}
		}

		if (enabled_actions.isEmpty()) {
			return;
		}

		String  action      = enabled_actions.toArray(new String[0])[Tim.rand.nextInt(enabled_actions.size())];
		Channel sendChannel = Tim.channelStorage.channelList.get(channel);

		switch (action) {
			case "item":
				assert sender != null;
				if (InteractionControls.interactWithUser(sender.getNick(), "item")) {
					getItem(sendChannel, sender.getNick(), null);
				}
				break;

			case "eightball":
				if (sender == null || InteractionControls.interactWithUser(sender.getNick(), "silly_reactions")) {
					eightball(sendChannel, sender, true, "");
				}
				break;

			case "fridge":
				assert sender != null;
				if (InteractionControls.interactWithUser(sender.getNick(), "fridge")) {
					fridge.throwFridge(sendChannel, sender, null, false);
				}
				break;

			case "defenestrate":
				assert sender != null;
				if (InteractionControls.interactWithUser(sender.getNick(), "defenestrate")) {
					defenestrate.defenestrate(sendChannel, sender, null, false);
				}
				break;

			case "sing":
				sing(sendChannel);
				break;

			case "foof":
				assert sender != null;
				if (InteractionControls.interactWithUser(sender.getNick(), "foof")) {
					foof(sendChannel, sender, null, false);
				}
				break;

			case "dance":
				dance(sendChannel);
				break;

			case "summon":
				summon.summon(sendChannel);
				break;

			case "banish":
				summon.banish(sendChannel);
				break;

			case "catch":
				catchCommand(sendChannel, null);
				break;

			case "search":
				assert sender != null;
				if (InteractionControls.interactWithUser(sender.getNick(), "search")) {
					search(sendChannel, sender, null);
				}
				break;

			case "herd":
				assert sender != null;
				if (InteractionControls.interactWithUser(sender.getNick(), "herd")) {
					herd(sendChannel, sender, null);
				}
				break;
		}
	}
}
