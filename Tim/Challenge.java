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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author mwalker
 */
public class Challenge {
	private List<String> approved = new ArrayList<String>();
	private List<String> pending = new ArrayList<String>();
	private long timeout = 3000;

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
	public boolean parseUserCommand( MessageEvent event ) {
		String message = event.getMessage();
		String command;
		String argsString = "";
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			argsString = message.substring(space + 1);
			args = argsString.split(" ", 0);
		} else {
			command = message.substring(1).toLowerCase();
		}

		if (command.equals("challenge")) {
			issueChallenge(event, event.getUser().getNick(), argsString);
			return true;
		} else if (command.equals("challengefor")) {
			String target;
			space = argsString.indexOf(" ");
			if (space > 0) {
				target = argsString.substring(0, space);
				argsString = argsString.substring(space + 1);
			} else {
				target = argsString;
				argsString = "";
			}

			issueChallenge(event, target, argsString);
			return true;
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

		if (command.equals("challenge")) {
			if (args[0].equals("approved")) {
				int pages = ( this.approved.size() + 9 ) / 10;
				int wantPage = 0;
				if (args != null && args.length > 1) {
					try {
						wantPage = Integer.parseInt(args[1]) - 1;
					} catch (NumberFormatException ex) {
						Tim.bot.sendMessage(channel,
							"Page number was not numeric.");
						return true;
					}
				}

				if (wantPage > pages) {
					wantPage = pages;
				}

				int list_idx = wantPage * 10;
				Tim.bot.sendMessage(channel, String.format(
					"Showing page %d of %d (%d challenges total)", wantPage + 1,
					pages, this.approved.size()));
				for (int i = 0; i < 10 && list_idx < this.approved.size(); ++i, list_idx = wantPage
																						   * 10 + i) {
					Tim.bot.sendMessage(channel, String.format("%d: %s", list_idx,
						this.approved.get(list_idx)));
				}
				return true;
			} else if (args[0].equals("pending")) {
				int pages = ( this.pending.size() + 9 ) / 10;
				int wantPage = 0;
				if (args != null && args.length > 1) {
					try {
						wantPage = Integer.parseInt(args[1]) - 1;
					} catch (NumberFormatException ex) {
						Tim.bot.sendMessage(channel,
							"Page number was not numeric.");
						return true;
					}
				}

				if (wantPage > pages) {
					wantPage = pages;
				}

				int list_idx = wantPage * 10;
				Tim.bot.sendMessage(channel, String.format(
					"Showing page %d of %d (%d challenges total)", wantPage + 1,
					pages, this.pending.size()));
				for (int i = 0; i < 10 && list_idx < this.pending.size(); ++i, list_idx = wantPage
																						  * 10 + i) {
					Tim.bot.sendMessage(channel, String.format("%d: %s", list_idx,
						this.pending.get(list_idx)));
				}
				return true;
			} else if (args[0].equals("approve")) {
				if (args != null && args.length > 1) {
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
					} else {
						Tim.bot.sendMessage(channel, String.format(
							"Challenge %s is not pending approval.", args[1]));
					}
				}
				return true;
			} else if (args[0].equals("unapprove")) {
				if (args != null && args.length > 1) {
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
					} else {
						Tim.bot.sendMessage(channel, String.format(
							"Challenge %s is not pending approval.", args[1]));
					}
					return true;
				}
			} else if (args[0].equals("delete")) {
				if (args != null && args.length > 1) {
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
					} else {
						Tim.bot.sendMessage(channel, String.format(
							"Challenge %s is not pending approval.", args[0]));
					}
					return true;
				}
			}
		}

		return false;
	}

	protected void helpSection( MessageEvent event ) {
		String[] strs = {
			"Challenge Commands:",
			"    !challenge - Request a challenge",
			"    !challenge <challenge> - Add a challenge",
			"    !challengefor <name> - Challenge someone else",
			"    !challengefor <name> <challenge> - Challenge someone else, and store it for approval",};

		for (int i = 0; i < strs.length; ++i) {
			Tim.bot.sendNotice(event.getUser(), strs[i]);
		}
	}

	protected void adminHelpSection( MessageEvent event ) {
		String[] strs = {
			"Challenge Commands:",
			"    $challenge pending [<page>] - List a page of pending items",
			"    $challenge approved [<page>] - List a page of approved items",
			"    $challenge approve <# from pending> - Approve a pending item",
			"    $challenge delete <# from pending> - Delete pending item",
			"    $challenge unapprove <# from approved> - Unapprove a previously approved item",};

		for (int i = 0; i < strs.length; ++i) {
			Tim.bot.sendNotice(event.getUser(), strs[i]);
		}
	}

	public void refreshDbLists() {
		this.getApprovedChallenges();
		this.getPendingChallenges();
	}

	protected void randomAction( String sender, String channel, String message, String type ) {
		String[] actions = {
			"challenge"
		};

		String action = actions[Tim.rand.nextInt(actions.length)];
		
		if ("challenge".equals(action)) {
			issueChallenge(channel, sender, null);
		}
	}

	public void issueChallenge( MessageEvent event, String target, String challenge ) {
		if (challenge != null && !( "".equals(challenge) )) {
			if (!( this.approved.contains(challenge) || this.pending.contains(challenge) ) && challenge.length() < 300) {
				this.insertPendingChallenge(challenge);
				this.pending.add(challenge);
			}
		} else {
			// Find a random challenge.
			int i = Tim.rand.nextInt(this.approved.size());
			challenge = this.approved.get(i);
		}

		Tim.bot.sendAction(event.getChannel(), String.format("challenges %s: %s", target, challenge));
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

	private void insertPendingChallenge( String challenge ) {
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

	private void setChallengeApproved( String challenge, Boolean approved ) {
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

	private void removeChallenge( String challenge ) {
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
