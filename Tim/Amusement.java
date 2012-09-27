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

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author mwalker
 */
public class Amusement {
	Tim ircclient;
	private long timeout = 3000;
	private List<String> approved_items = new ArrayList<String>();
	private List<String> pending_items = new ArrayList<String>();
	private List<String> colours = new ArrayList<String>();
	private List<String> eightballs = new ArrayList<String>();
	private List<String> commandments = new ArrayList<String>();
	protected List<String> aypwips = new ArrayList<String>();
	private List<String> flavours = new ArrayList<String>();
	private List<String> deities = new ArrayList<String>();

	public Amusement( Tim ircclient ) {
		this.ircclient = ircclient;
	}

	/**
	 * Parses user-level commands passed from the main class. Returns true if the message was handled, false if it was
	 * not.
	 *
	 * @param channel What channel this came from
	 * @param sender  Who sent the command
	 * @param prefix  What prefix was on the command
	 * @param message What was the actual content of the message
	 *
	 * @return True if message was handled, false otherwise.
	 */
	public boolean parseUserCommand( String channel, String sender, String prefix, String message ) {
		String command;
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			args = message.substring(space + 1).split(" ", 0);
		} else {
			command = message.substring(1).toLowerCase();
		}

		if (prefix.equals("!")) {
			if (command.equals("sing")) {
				sing(channel);
				return true;
			} else if (command.equals("eightball") || command.equals("8-ball")) {
				eightball(channel, sender, false);
				return true;
			} else if (command.charAt(0) == 'd'
					   && Pattern.matches("d\\d+", command)) {
				dice(command.substring(1), channel, sender, args);
				return true;
			} else if (command.equals("woot")) {
				ircclient.sendAction(channel, "cheers! Hooray!");
				return true;
			} else if (command.equals("get")) {
				getItem(channel, sender, args);
				return true;
			} else if (command.equals("getfor")) {
				if (args != null && args.length > 0) {
					if (args.length > 1) {
						// Want a new args array less the first old element.
						String[] newargs = new String[args.length - 1];
						for (int i = 1; i < args.length; ++i) {
							newargs[i - 1] = args[i];
						}
						getItem(channel, args[0], newargs);
						return true;
					} else {
						getItem(channel, args[0], null);
						return true;
					}
				}
			} else if (command.equals("fridge")) {
				throwFridge(channel, sender, args, true);
				return true;
			} else if (command.equals("dance")) {
				dance(channel);
				return true;
			} else if (command.equals("lick")) {
				lick(channel, sender, args);
				return true;
			} else if (command.equals("commandment")) {
				commandment(channel, sender, args);
				return true;
			} else if (command.equals("defenestrate")) {
				defenestrate(channel, sender, args, true);
				return true;
			} else if (command.equals("summon")) {
				summon(channel, sender, args, true);
				return true;
			} else if (command.equals("foof")) {
				foof(channel, sender, args, true);
				return true;
			}
		}
		return false;
	}

	/**
	 * Parses admin-level commands passed from the main class. Returns true if the message was handled, false if it was
	 * not.
	 *
	 * @param channel What channel this came from
	 * @param sender  Who sent the command
	 * @param message What was the actual content of the message.
	 *
	 * @return True if message was handled, false otherwise
	 */
	public boolean parseAdminCommand( String channel, String sender, String message ) {
		String command;
		String argsString;
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(0, space).toLowerCase();
			argsString = message.substring(space + 1);
			args = argsString.split(" ", 0);
		} else {
			command = message.toLowerCase();
		}

		if (command.equals("listitems")) {
			int pages = ( this.approved_items.size() + 9 ) / 10;
			int wantPage = 0;
			if (args != null && args.length > 0) {
				try {
					wantPage = Integer.parseInt(args[0]) - 1;
				} catch (NumberFormatException ex) {
					ircclient.sendMessage(channel,
						"Page number was not numeric.");
					return true;
				}
			}

			if (wantPage > pages) {
				wantPage = pages;
			}

			int list_idx = wantPage * 10;
			ircclient.sendMessage(channel, String.format(
				"Showing page %d of %d (%d items total)", wantPage + 1,
				pages, this.approved_items.size()));
			for (int i = 0; i < 10 && list_idx < this.approved_items.size(); ++i, list_idx = wantPage
																							 * 10 + i) {
				ircclient.sendMessage(channel, String.format("%d: %s", list_idx,
					this.approved_items.get(list_idx)));
			}
			return true;
		} else if (command.equals("listpending")) {
			int pages = ( this.pending_items.size() + 9 ) / 10;
			int wantPage = 0;
			if (args != null && args.length > 0) {
				try {
					wantPage = Integer.parseInt(args[0]) - 1;
				} catch (NumberFormatException ex) {
					ircclient.sendMessage(channel,
						"Page number was not numeric.");
					return true;
				}
			}

			if (wantPage > pages) {
				wantPage = pages;
			}

			int list_idx = wantPage * 10;
			ircclient.sendMessage(channel, String.format(
				"Showing page %d of %d (%d items total)", wantPage + 1,
				pages, this.pending_items.size()));
			for (int i = 0; i < 10 && list_idx < this.pending_items.size(); ++i, list_idx = wantPage
																							* 10 + i) {
				ircclient.sendMessage(channel, String.format("%d: %s", list_idx,
					this.pending_items.get(list_idx)));
			}
			return true;
		} else if (command.equals("approveitem")) {
			if (args != null && args.length > 0) {
				int idx;
				String item;
				try {
					idx = Integer.parseInt(args[0]);
					item = this.pending_items.get(idx);
				} catch (NumberFormatException ex) {
					// Must be a string
					item = args[0];
					for (int i = 1; i < args.length; ++i) {
						item = item + " " + args[i];
					}
					idx = this.pending_items.indexOf(item);
				}
				if (idx >= 0) {
					this.setItemApproved(item, true);
					this.pending_items.remove(idx);
					this.approved_items.add(item);
				} else {
					ircclient.sendMessage(channel, String.format(
						"Item %s is not pending approval.", args[0]));
				}
			}
			return true;
		} else if (command.equals("disapproveitem")) {
			if (args != null && args.length > 0) {
				int idx;
				String item;
				try {
					idx = Integer.parseInt(args[0]);
					item = this.approved_items.get(idx);
				} catch (NumberFormatException ex) {
					// Must be a string
					item = args[0];
					for (int i = 1; i < args.length; ++i) {
						item = item + " " + args[i];
					}
					idx = this.approved_items.indexOf(item);
				}
				if (idx >= 0) {
					this.setItemApproved(item, false);
					this.pending_items.add(item);
					this.approved_items.remove(idx);
				} else {
					ircclient.sendMessage(channel, String.format(
						"Item %s is not pending approval.", args[0]));
				}
			}
			return true;
		} else if (command.equals("deleteitem")) {
			if (args != null && args.length > 0) {
				int idx;
				String item;
				try {
					idx = Integer.parseInt(args[0]);
					item = this.pending_items.get(idx);
				} catch (NumberFormatException ex) {
					// Must be a string
					item = args[0];
					for (int i = 1; i < args.length; ++i) {
						item = item + " " + args[i];
					}
					idx = this.pending_items.indexOf(item);
				}
				if (idx >= 0) {
					this.removeItem(item);
					this.pending_items.remove(item);
				} else {
					ircclient.sendMessage(channel, String.format(
						"Item %s is not pending approval.", args[0]));
				}
			}
			return true;
		}

		return false;
	}

	protected void helpSection( String target, String channel) {
		String[] strs = {"Amusement Commands:",
						 "    !get <anything> - I will fetch you whatever you like.",
						 "    !getfor <someone> <anything> - I will give someone whatever you like.",
						 "    !eightball <your question> - I can tell you (with some degree of inaccuracy) how likely something is.",};

		for (int i = 0; i < strs.length; ++i) {
			ircclient.sendNotice(target, strs[i]);
		}
	}

	public void refreshDbLists() {
		this.getAypwipList();
		this.getColourList();
		this.getCommandmentList();
		this.getDeityList();
		this.getEightballList();
		this.getFlavourList();
		this.getApprovedItems();
		this.getPendingItems();
	}

	protected void randomAction( String sender, String channel, String message, String type ) {
		String[] actions = {
			"item", "eightball", "fridge", "defenestrate", "sing", "foof", "dance"
		};

		String action = actions[ircclient.rand.nextInt(actions.length)];

		if ("item".equals(action)) {
			getItem(channel, sender, null);
		} else if ("eightball".equals(action)) {
			eightball(channel, sender, true);
		} else if ("fridge".equals(action)) {
			throwFridge(channel, sender, null, false);
		} else if ("defenestrate".equals(action)) {
			defenestrate(channel, sender, null, false);
		} else if ("sing".equals(action)) {
			sing(channel);
		} else if ("foof".equals(action)) {
			foof(channel, sender, null, false);
		} else if ("dance".equals(action)) {
			dance(channel);
		}
	}

	protected void dice( String number, String channel, String sender,
						 String[] args ) {
		int max;
		try {
			max = Integer.parseInt(number);
			int r = ircclient.rand.nextInt(max) + 1;
			ircclient.sendMessage(channel, sender + ": Your result is " + r);
		} catch (NumberFormatException ex) {
			ircclient.sendMessage(channel, number
										   + " is not a number I could understand.");
		}
	}

	protected void getItem( String channel, String target, String[] args ) {
		String item = "";
		if (args != null) {
			item = args[0];
			for (int i = 1; i < args.length; ++i) {
				item = item + " " + args[i];
			}
			if (!( this.approved_items.contains(item) || this.pending_items.contains(item) ) && item.length() < 300) {
				this.insertPendingItem(item);
				this.pending_items.add(item);
			}
		}

		if (item.isEmpty()) {
			// Find a random item.
			int i = ircclient.rand.nextInt(this.approved_items.size());
			item = this.approved_items.get(i);
		}

		ircclient.sendAction(channel, String.format("gets %s %s", target, item));
	}

	protected void lick( String channel, String sender, String[] args ) {
		if (ircclient.isChannelAdult(channel)) {
			if (args.length >= 1) {
				String argStr = ircclient.implodeArray(args);

				if (args[0].equalsIgnoreCase("MysteriousAges")) {
					ircclient.sendAction(channel, "licks " + argStr
												  + ". Tastes like... like...");
					ircclient.sendDelayedMessage(channel, "Like the Apocalypse.",
						1000);
					ircclient.sendDelayedAction(channel, "cowers in fear", 2400);
				} else if (args[0].equalsIgnoreCase(ircclient.getNick())) {
					ircclient.sendAction(channel, "licks " + args[0]
												  + ". Tastes like meta.");
				} else if (ircclient.admin_list.contains(args[0])) {
					ircclient.sendAction(channel, "licks " + argStr
												  + ". Tastes like perfection, pure and simple.");
				} else {
					ircclient.sendAction(
						channel,
						"licks "
						+ argStr
						+ ". Tastes like "
						+ this.flavours.get(ircclient.rand.nextInt(this.flavours.size())));
				}
			} else {
				ircclient.sendAction(
					channel,
					"licks "
					+ sender
					+ "! Tastes like "
					+ this.flavours.get(ircclient.rand.nextInt(this.flavours.size())));
			}
		} else {
			ircclient.sendMessage(channel, "Sorry, I don't do that here.");
		}
	}

	protected void eightball( String channel, String sender, boolean mutter ) {
		int r = ircclient.rand.nextInt(this.eightballs.size());
		int delay = ircclient.rand.nextInt(1500);

		if (mutter) {
			ircclient.sendDelayedAction(channel, "mutters under his breath, \"" + this.eightballs.get(r) + "\"", delay);
		} else {
			ircclient.sendDelayedMessage(channel, sender + ": " + this.eightballs.get(r), delay);
		}
	}

	protected void sing( String channel ) {
		Connection con;
		int r = ircclient.rand.nextInt(100);

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
			con = ircclient.pool.getConnection(timeout);
			PreparedStatement songName = con.prepareStatement("SELECT name FROM songs ORDER BY rand() LIMIT 1");
			ResultSet songNameRes;

			songNameRes = songName.executeQuery();
			songNameRes.next();

			ircclient.sendDelayedAction(channel, String.format(response, songNameRes.getString("name")), ircclient.rand.nextInt(1500));
			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void dance( String channel ) {
		Connection con;
		int r = ircclient.rand.nextInt(100);

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
			con = ircclient.pool.getConnection(timeout);
			PreparedStatement danceName = con.prepareStatement("SELECT name FROM dances ORDER BY rand() LIMIT 1");
			ResultSet danceNameRes;

			danceNameRes = danceName.executeQuery();
			danceNameRes.next();

			ircclient.sendDelayedAction(channel, String.format(response, danceNameRes.getString("name")), ircclient.rand.nextInt(1500));
			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void commandment( String channel, String sender, String[] args ) {
		int r = ircclient.rand.nextInt(this.commandments.size());
		if (args != null && args.length == 1 && Double.parseDouble(args[0]) > 0
			&& Double.parseDouble(args[0]) <= this.commandments.size()) {
			r = (int) Double.parseDouble(args[0]) - 1;
		}
		ircclient.sendMessage(channel, this.commandments.get(r));
	}

	protected void throwFridge( String channel, String sender, String[] args,
								Boolean righto ) {
		String target = sender;
		if (args != null && args.length > 0) {
			if (!args[0].equalsIgnoreCase(ircclient.getNick())
				&& !args[0].equalsIgnoreCase("himself")
				&& !args[0].equalsIgnoreCase("herself")
				&& !ircclient.admin_list.contains(args[0])
				&& !args[0].equalsIgnoreCase("myst")) {
				target = ircclient.implodeArray(args) + " ";
			}
		}

		if (righto) {
			ircclient.sendMessage(channel, "Righto...");
		}

		int time = 2 + ircclient.rand.nextInt(15);
		time *= 1000;
		ircclient.sendDelayedAction(channel,
			"looks back and forth, then slinks off...", time);
		time += ircclient.rand.nextInt(10) * 500 + 1500;
		String colour = this.colours.get(ircclient.rand.nextInt(this.colours.size()));
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
		int i = ircclient.rand.nextInt(100);
		String act;
		if (i > 20) {
			act = "hurls a" + colour + " coloured fridge at " + target;
		} else if (i > 3) {
			target = sender;
			act = "hurls a" + colour + " coloured fridge at " + target
				  + " and runs away giggling";
		} else {
			act = "trips and drops a" + colour + " fridge on himself";
		}
		ircclient.sendDelayedAction(channel, act, time);
	}

	protected void defenestrate( String channel, String sender, String[] args,
								 Boolean righto ) {
		String target = sender;
		if (args != null && args.length > 0) {
			if (!args[0].equalsIgnoreCase(ircclient.getNick())
				&& !args[0].equalsIgnoreCase("himself")
				&& !args[0].equalsIgnoreCase("herself")
				&& !ircclient.admin_list.contains(args[0])) {
				target = ircclient.implodeArray(args);
			}
		}

		if (righto) {
			ircclient.sendMessage(channel, "Righto...");
		}

		int time = 2 + ircclient.rand.nextInt(15);
		time *= 1000;
		ircclient.sendDelayedAction(channel,
			"looks around for a convenient window, then slinks off...",
			time);
		time += ircclient.rand.nextInt(10) * 500 + 1500;

		int i = ircclient.rand.nextInt(100);
		String act;
		String colour = this.colours.get(ircclient.rand.nextInt(this.colours.size()));

		if (i > 20) {
			act = "throws "
				  + target
				  + " through the nearest window, where they land on a giant pile of fluffy "
				  + colour + " coloured pillows.";
		} else if (i > 3) {
			target = sender;
			act = "laughs maniacally then throws "
				  + target
				  + " through the nearest window, where they land on a giant pile of fluffy "
				  + colour + " coloured pillows.";
		} else {
			act = "trips and falls out the window!";
		}
		ircclient.sendDelayedAction(channel, act, time);
	}

	protected void summon( String channel, String sender, String[] args,
						   Boolean righto ) {
		String target;
		if (args == null || args.length == 0) {
			target = this.deities.get(ircclient.rand.nextInt(this.deities.size()));
		} else {
			target = ircclient.implodeArray(args);
		}

		if (righto) {
			ircclient.sendMessage(channel, "Righto...");
		}

		int time = 2 + ircclient.rand.nextInt(15);
		time *= 1000;
		ircclient.sendDelayedAction(channel,
			"prepares the summoning circle required to bring " + target
			+ " into the world...", time);
		time += ircclient.rand.nextInt(10) * 500 + 1500;

		int i = ircclient.rand.nextInt(100);
		String act;

		if (i > 50) {
			act = "completes the ritual successfully, drawing " + target
				  + " through, and binding them into the summoning circle!";
		} else if (i > 30) {
			act = "completes the ritual, drawing "
				  + target
				  + " through, but something goes wrong and they fade away after just a few moments.";
		} else {
			String target2 = this.deities.get(ircclient.rand.nextInt(this.deities.size()));
			act = "attempts to summon "
				  + target
				  + ", but something goes horribly wrong. After the smoke clears, "
				  + target2
				  + " is left standing on the smoldering remains of the summoning circle.";
		}
		ircclient.sendDelayedAction(channel, act, time);
	}

	protected void foof( String channel, String sender, String[] args,
						 Boolean righto ) {
		String target = sender;
		if (args != null && args.length > 0) {
			if (!args[0].equalsIgnoreCase(ircclient.getNick())
				&& !args[0].equalsIgnoreCase("himself")
				&& !args[0].equalsIgnoreCase("herself")
				&& !ircclient.admin_list.contains(args[0])) {
				target = ircclient.implodeArray(args);
			}
		}

		if (righto) {
			ircclient.sendMessage(channel, "Righto...");
		}

		int time = 2 + ircclient.rand.nextInt(15);
		time *= 1000;
		ircclient.sendDelayedAction(
			channel,
			"surreptitiously works his way over to the couch, looking ever so casual...",
			time);
		time += ircclient.rand.nextInt(10) * 500 + 1500;

		int i = ircclient.rand.nextInt(100);
		String act;
		String colour = this.colours.get(ircclient.rand.nextInt(this.colours.size()));

		if (i > 20) {
			act = "grabs a " + colour + " pillow, and throws it at " + target
				  + ", hitting them squarely in the back of the head.";
		} else if (i > 3) {
			target = sender;
			act = "laughs maniacally then throws a " + colour + " pillow at "
				  + target
				  + ", then runs off and hides behind the nearest couch.";
		} else {
			act = "trips and lands on a " + colour + " pillow. Oof!";
		}
		ircclient.sendDelayedAction(channel, act, time);
	}

	protected void getApprovedItems() {
		Connection con;
		String value;

		try {
			this.approved_items.clear();

			con = ircclient.pool.getConnection(timeout);
			PreparedStatement s = con.prepareStatement("SELECT `item` FROM `items` WHERE `approved` = TRUE");
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("item");
				this.approved_items.add(value);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void getPendingItems() {
		Connection con;
		String value;
		this.pending_items.clear();

		try {
			con = ircclient.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `item` FROM `items` WHERE `approved` = FALSE");
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("item");
				this.pending_items.add(value);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void insertPendingItem( String item ) {
		Connection con;
		try {
			con = ircclient.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `items` (`item`, `approved`) VALUES (?, FALSE)");
			s.setString(1, item);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void setItemApproved( String item, Boolean approved ) {
		Connection con;
		try {
			con = ircclient.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `items` SET `approved` = ? WHERE `item` = ?");
			s.setBoolean(1, approved);
			s.setString(2, item);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void removeItem( String item ) {
		Connection con;
		try {
			con = ircclient.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `items` WHERE `item` = ?");
			s.setString(1, item);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void getAypwipList() {
		Connection con;
		try {
			con = ircclient.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `aypwips`");

			ResultSet rs = s.getResultSet();
			this.aypwips.clear();
			while (rs.next()) {
				this.aypwips.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void getColourList() {
		Connection con;
		try {
			con = ircclient.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `colours`");

			ResultSet rs = s.getResultSet();
			this.colours.clear();
			while (rs.next()) {
				this.colours.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void getCommandmentList() {
		Connection con;
		try {
			con = ircclient.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `commandments`");

			ResultSet rs = s.getResultSet();
			this.commandments.clear();
			while (rs.next()) {
				this.commandments.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void getDeityList() {
		Connection con;
		try {
			con = ircclient.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `deities`");

			ResultSet rs = s.getResultSet();
			this.deities.clear();
			while (rs.next()) {
				this.deities.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void getEightballList() {
		Connection con;
		try {
			con = ircclient.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `eightballs`");

			ResultSet rs = s.getResultSet();
			this.eightballs.clear();
			while (rs.next()) {
				this.eightballs.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void getFlavourList() {
		Connection con;
		try {
			con = ircclient.pool.getConnection(timeout);

			Statement s = con.createStatement();
			s.executeQuery("SELECT `string` FROM `flavours`");

			ResultSet rs = s.getResultSet();
			this.flavours.clear();
			while (rs.next()) {
				this.flavours.add(rs.getString("string"));
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
