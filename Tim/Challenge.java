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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author mwalker
 */
class Challenge {

	private final List<String> approved = new ArrayList<>();
	private final List<String> pending = new ArrayList<>();
	private final long timeout = 3000;

	/**
	 * Parses user-level commands passed from the main class. Returns true if the message was handled, false if it was
	 * not.
	 *
	 * @param event Event to process
	 *
	 * @return True if message was handled, false otherwise.
	 */
	boolean parseUserCommand(MessageEvent event) {
		if (event.getUser() == null) {
			return false;
		}

		ChannelInfo cdata = Tim.db.channel_data.get(event.getChannel().getName().toLowerCase());
		String message = Colors.removeFormattingAndColors(event.getMessage());
		String command;
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
				if (cdata.commands_enabled.get("chainstory")) {
					issueChallenge(event.getChannel(), event.getUser().getNick(), argsString);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
			case "challengefor":
				if (cdata.commands_enabled.get("chainstory")) {
					String target;
					space = argsString.indexOf(" ");
					if (space > 0) {
						target = argsString.substring(0, space);
						argsString = argsString.substring(space + 1);
					} else {
						target = argsString;
						argsString = "";
					}

					issueChallenge(event.getChannel(), target, argsString);
				} else {
					event.respond("I'm sorry. I don't do that here.");
				}

				return true;
		}

		return false;
	}

	/**
	 * Parses admin-level commands passed from the main class. Returns true if the message was handled, false if it was
	 * not.
	 *
	 * @param event Event to process
	 *
	 * @return True if message was handled, false otherwise
	 */
	boolean parseAdminCommand(MessageEvent event) {
		String message = Colors.removeFormattingAndColors(event.getMessage());
		String command;
		String argsString;
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			argsString = message.substring(space + 1);
			args = argsString.split(" ", 0);
		} else {
			command = message.toLowerCase().substring(1);
		}

		if (command.equals("challenge")) {
			assert args != null;
			switch (args[0]) {
				case "approved": {
					int pages = (this.approved.size() + 9) / 10;
					int wantPage = 0;
					if (args.length > 1) {
						try {
							wantPage = Integer.parseInt(args[1]) - 1;
						} catch (NumberFormatException ex) {
							event.respond("Page number was not numeric.");
							return true;
						}
					}

					if (wantPage > pages) {
						wantPage = pages;
					}

					int list_idx = wantPage * 10;
					event.respond(String.format("Showing page %d of %d (%d challenges total)", wantPage + 1, pages, this.approved.size()));
					for (int i = 0; i < 10 && list_idx < this.approved.size(); ++i, list_idx = wantPage * 10 + i) {
						event.respond(String.format("%d: %s", list_idx, this.approved.get(list_idx)));
					}
					return true;
				}
				case "pending": {
					int pages = (this.pending.size() + 9) / 10;
					int wantPage = 0;
					if (args.length > 1) {
						try {
							wantPage = Integer.parseInt(args[1]) - 1;
						} catch (NumberFormatException ex) {
							event.respond("Page number was not numeric.");
							return true;
						}
					}

					if (wantPage > pages) {
						wantPage = pages;
					}

					int list_idx = wantPage * 10;
					event.respond(String.format("Showing page %d of %d (%d challenges total)", wantPage + 1, pages, this.pending.size()));
					for (int i = 0; i < 10 && list_idx < this.pending.size(); ++i, list_idx = wantPage * 10 + i) {
						event.respond(String.format("%d: %s", list_idx, this.pending.get(list_idx)));
					}
					return true;
				}
				case "approve":
					if (args.length > 1) {
						int idx;
						String challenge;
						try {
							idx = Integer.parseInt(args[1]);
							challenge = this.pending.get(idx);
						} catch (NumberFormatException ex) {
							// Must be a string
							challenge = args[1];
							for (int i = 2; i < args.length; ++i) {
								challenge = challenge + " " + args[i];
							}
							idx = this.pending.indexOf(challenge);
						}
						if (idx >= 0) {
							this.setChallengeApproved(challenge, true);
							this.pending.remove(idx);
							this.approved.add(challenge);
							event.respond(String.format("Challenge %s approved.", args[1]));
						} else {
							event.respond(String.format("Challenge %s is not pending approval.", args[1]));
						}
					}
					return true;
				case "unapprove":
					if (args.length > 1) {
						int idx;
						String challenge;
						try {
							idx = Integer.parseInt(args[1]);
							challenge = this.approved.get(idx);
						} catch (NumberFormatException ex) {
							// Must be a string
							challenge = args[1];
							for (int i = 2; i < args.length; ++i) {
								challenge = challenge + " " + args[i];
							}
							idx = this.approved.indexOf(challenge);
						}
						if (idx >= 0) {
							this.setChallengeApproved(challenge, false);
							this.pending.add(challenge);
							this.approved.remove(idx);
							event.respond(String.format("Challenge %s unapproved.", args[1]));
						} else {
							event.respond(String.format("Challenge %s is not in approved list.", args[1]));
						}
						return true;
					}
					break;
				case "delete":
					if (args.length > 1) {
						int idx;
						String challenge;
						try {
							idx = Integer.parseInt(args[1]);
							challenge = this.pending.get(idx);
						} catch (NumberFormatException ex) {
							// Must be a string
							challenge = args[1];
							for (int i = 2; i < args.length; ++i) {
								challenge = challenge + " " + args[i];
							}
							idx = this.pending.indexOf(challenge);
						}
						if (idx >= 0) {
							this.removeChallenge(challenge);
							this.pending.remove(challenge);
							event.respond(String.format("Challenge %s deleted from pending list.", args[0]));
						} else {
							event.respond(String.format("Challenge %s is not pending approval.", args[0]));
						}
						return true;
					}
					break;
			}
		}

		return false;
	}

	void helpSection(MessageEvent event) {
		if (event.getUser() == null) {
			return;
		}

		String[] strs = {
			"Challenge Commands:",
			"    !challenge - Request a challenge",
			"    !challenge <challenge> - Add a challenge",
			"    !challengefor <name> - Challenge someone else",
			"    !challengefor <name> <challenge> - Challenge someone else, and store it for approval",};

		for (String str : strs) {
			event.getUser().send().notice(str);
		}
	}

	void adminHelpSection(MessageEvent event) {
		if (event.getUser() == null) {
			return;
		}

		String[] strs = {
			"Challenge Commands:",
			"    $challenge pending [<page>] - List a page of pending items",
			"    $challenge approved [<page>] - List a page of approved items",
			"    $challenge approve <# from pending> - Approve a pending item",
			"    $challenge delete <# from pending> - Delete pending item",
			"    $challenge unapprove <# from approved> - Unapprove a previously approved item",};

		for (String str : strs) {
			event.getUser().send().notice(str);
		}
	}

	void refreshDbLists() {
		this.getApprovedChallenges();
		this.getPendingChallenges();
	}

	void randomAction(User sender, String channel) {
		String[] actions = {
			"challenge"
		};

		String action = actions[Tim.rand.nextInt(actions.length)];

		if ("challenge".equals(action)) {
			issueChallenge(Tim.channelStorage.channelList.get(channel), sender.getNick(), null);
		}
	}

	private void issueChallenge(Channel channel, String target, String challenge) {
		if (challenge != null && !("".equals(challenge))) {
			if (!(this.approved.contains(challenge) || this.pending.contains(challenge)) && challenge.length() < 300) {
				this.insertPendingChallenge(challenge);
				this.pending.add(challenge);
			}
		} else {
			// Find a random challenge.
			int i = Tim.rand.nextInt(this.approved.size());
			challenge = this.approved.get(i);
		}

		channel.send().action(String.format("challenges %s: %s", target, challenge));
	}

	private void getApprovedChallenges() {
		Connection con;
		String value;
		this.approved.clear();

		try {
			con = Tim.db.pool.getConnection(timeout);
			PreparedStatement s = con.prepareStatement("SELECT `challenge` FROM `challenges` WHERE `approved` = TRUE");
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("challenge");
				this.approved.add(value);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void getPendingChallenges() {
		Connection con;
		String value;
		this.pending.clear();

		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `challenge` FROM `challenges` WHERE `approved` = FALSE");
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("challenge");
				this.pending.add(value);
			}

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void insertPendingChallenge(String challenge) {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `challenges` (`challenge`, `approved`) VALUES (?, FALSE)");
			s.setString(1, challenge);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void setChallengeApproved(String challenge, Boolean approved) {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `challenges` SET `approved` = ? WHERE `challenge` = ?");
			s.setBoolean(1, approved);
			s.setString(2, challenge);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void removeChallenge(String challenge) {
		Connection con;
		try {
			con = Tim.db.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `challenges` WHERE `challenge` = ?");
			s.setString(1, challenge);
			s.executeUpdate();

			con.close();
		} catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
