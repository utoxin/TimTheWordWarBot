/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Tim;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mwalker
 */
public class Challenge {
	Tim ircclient;
	private List<String> approved = new ArrayList<String>();
	private List<String> pending = new ArrayList<String>();

	public Challenge(Tim ircclient) {
		this.ircclient = ircclient;
	}

	/**
	 * Parses user-level commands passed from the main class. Returns true if 
	 * the message was handled, false if it was not.
	 * 
	 * @param channel
	 * @param sender
	 * @param prefix
	 * @param message
	 * @return 
	 */
	public boolean parseUserCommand(String channel, String sender, String prefix, String message) {
		String command;
		String argsString = "";
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			argsString = message.substring(space + 1);
			args = argsString.split(" ", 0);
		}
		else {
			command = message.substring(1).toLowerCase();
		}

		if (prefix.equals("!")) {
			if (command.equals("challengehelp")) {
				help(sender, channel);
				return true;
			}
			else if (command.equals("challenge")) {
				issueChallenge(channel, sender, argsString);
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Parses admin-level commands passed from the main class. Returns true if 
	 * the message was handled, false if it was not.
	 * 
	 * @param channel
	 * @param sender
	 * @param prefix
	 * @param message
	 * @return 
	 */
	public boolean parseAdminCommand(String channel, String sender, String message) {
		String command;
		String argsString = "";
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(0, space).toLowerCase();
			argsString = message.substring(space + 1);
			args = argsString.split(" ", 0);
		}
		else {
			command = message.toLowerCase();
		}

		if (command.equals("challengehelp")) {
			adminHelp(sender, channel);
			return true;
		}
		else if (command.equals("challenge")) {
			if (args[0].equals("approved")) {
				int pages = ( this.approved.size() + 9 ) / 10;
				int wantPage = 0;
				if (args != null && args.length > 1) {
					try {
						wantPage = Integer.parseInt(args[1]) - 1;
					}
					catch (NumberFormatException ex) {
						this.ircclient.sendMessage(channel,
										 "Page number was not numeric.");
						return true;
					}
				}

				if (wantPage > pages) {
					wantPage = pages;
				}

				int list_idx = wantPage * 10;
				this.ircclient.sendMessage(channel, String.format(
						"Showing page %d of %d (%d challenges total)", wantPage + 1,
						pages, this.approved.size()));
				for (int i = 0; i < 10 && list_idx < this.approved.size(); ++i, list_idx = wantPage
																								 * 10 + i) {
					this.ircclient.sendMessage(channel, String.format("%d: %s", list_idx,
															this.approved.get(list_idx)));
				}
				return true;
			}
			else if (args[0].equals("pending")) {
				int pages = ( this.pending.size() + 9 ) / 10;
				int wantPage = 0;
				if (args != null && args.length > 1) {
					try {
						wantPage = Integer.parseInt(args[1]) - 1;
					}
					catch (NumberFormatException ex) {
						this.ircclient.sendMessage(channel,
										 "Page number was not numeric.");
						return true;
					}
				}

				if (wantPage > pages) {
					wantPage = pages;
				}

				int list_idx = wantPage * 10;
				this.ircclient.sendMessage(channel, String.format(
						"Showing page %d of %d (%d challenges total)", wantPage + 1,
						pages, this.pending.size()));
				for (int i = 0; i < 10 && list_idx < this.pending.size(); ++i, list_idx = wantPage
																								* 10 + i) {
					this.ircclient.sendMessage(channel, String.format("%d: %s", list_idx,
															this.pending.get(list_idx)));
				}
				return true;
			}
			else if (args[0].equals("approve")) {
				if (args != null && args.length > 1) {
					int idx = 0;
					String challenge = "";
					try {
						idx = Integer.parseInt(args[1]);
						challenge = this.pending.get(idx);
					}
					catch (NumberFormatException ex) {
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
					}
					else {
						this.ircclient.sendMessage(channel, String.format(
								"Challenge %s is not pending approval.", args[1]));
					}
				}
				return true;
			}
			else if (args[0].equals("unapprove")) {
				if (args != null && args.length > 1) {
					int idx = 0;
					String challenge = "";
					try {
						idx = Integer.parseInt(args[1]);
						challenge = this.approved.get(idx);
					}
					catch (NumberFormatException ex) {
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
					}
					else {
						this.ircclient.sendMessage(channel, String.format(
								"Challenge %s is not pending approval.", args[1]));
					}
					return true;
				}
			}
			else if (args[0].equals("delete")) {
				if (args != null && args.length > 1) {
					int idx = 0;
					String challenge = "";
					try {
						idx = Integer.parseInt(args[1]);
						challenge = this.pending.get(idx);
					}
					catch (NumberFormatException ex) {
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
					}
					else {
						this.ircclient.sendMessage(channel, String.format(
								"Challenge %s is not pending approval.", args[0]));
					}
					return true;
				}
			}
		}
		
		return false;
	}
	
	private void help(String target, String channel) {
		int msgdelay = 9;
		String[] strs = { 
						  "!challenge - Request a challenge",
						  "!challenge <challenge> - Add a challenge",
						  "!challengehelp - Get help on my challenge commands",
		};

		this.ircclient.sendAction(channel, "whispers in " + target + "'s ear. (Check for a new windor or tab with the help text.)");
		for (int i = 0; i < strs.length; ++i) {
			this.ircclient.sendDelayedMessage(target, strs[i], msgdelay * i);
		}
	}

	private void adminHelp(String target, String channel) {
		int msgdelay = 9;
		String[] strs = { 
						  "$challenge pending [<page>] - List a page of pending items",
						  "$challenge approved [<page>] - List a page of approved items",
						  "$challenge approve <# from pending> - Approve a pending item",
						  "$challenge delete <# from pending> - Delete pending item",
						  "$challenge unapprove <# from approved> - Unapprove a previously approved item",
						  "$challengehelp - Get help on the admin challenge commands",
		};

		this.ircclient.sendAction(channel, "whispers in " + target + "'s ear. (Check for a new windor or tab with the help text.)");
		for (int i = 0; i < strs.length; ++i) {
			this.ircclient.sendDelayedMessage(target, strs[i], msgdelay * i);
		}
	}

	public void refreshDbLists() {
		this.getApprovedChallenges();
		this.getPendingChallenges();
	}
	
	public void issueChallenge(String channel, String target, String challenge) {
		if (challenge != null && !("".equals(challenge))) {
			if (!( this.approved.contains(challenge) || this.pending.contains(challenge) ) && challenge.length() < 300) {
				this.insertPendingChallenge(challenge);
				this.pending.add(challenge);
			}
		} else {
			// Find a random challenge.
			int i = this.ircclient.rand.nextInt(this.approved.size());
			challenge = this.approved.get(i);
		}

		this.ircclient.sendAction(channel, String.format("challenges %s: %s", target, challenge));
	}

	private void getApprovedChallenges() {
		long timeout = 3000;
		Connection con = null;

		try {
			String value = "";
			this.approved.clear();

			con = ircclient.pool.getConnection(timeout);
			PreparedStatement s = con.prepareStatement("SELECT `challenge` FROM `challenges` WHERE `approved` = TRUE");
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("challenge");
				this.approved.add(value);
			}
			
			con.close();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		return;
	}

	private void getPendingChallenges() {
		long timeout = 3000;
		Connection con = null;
		String value = "";
		this.pending.clear();

		try {
			con = ircclient.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("SELECT `challenge` FROM `challenges` WHERE `approved` = FALSE");
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				value = rs.getString("challenge");
				this.pending.add(value);
			}
			
			con.close();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		return;
	}

	private void insertPendingChallenge(String challenge) {
		long timeout = 3000;
		Connection con = null;
		try {
			con = ircclient.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("INSERT INTO `challenges` (`challenge`, `approved`) VALUES (?, FALSE)");
			s.setString(1, challenge);
			s.executeUpdate();
			
			con.close();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		return;
	}

	private void setChallengeApproved(String challenge, Boolean approved) {
		long timeout = 3000;
		Connection con = null;
		try {
			con = ircclient.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("UPDATE `challenges` SET `approved` = ? WHERE `challenge` = ?");
			s.setBoolean(1, approved);
			s.setString(2, challenge);
			s.executeUpdate();
			
			con.close();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		return;
	}

	private void removeChallenge(String challenge) {
		long timeout = 3000;
		Connection con = null;
		try {
			con = ircclient.pool.getConnection(timeout);

			PreparedStatement s = con.prepareStatement("DELETE FROM `challenges` WHERE `challenge` = ?");
			s.setString(1, challenge);
			s.executeUpdate();
			
			con.close();
		}
		catch (SQLException ex) {
			Logger.getLogger(Tim.class.getName()).log(Level.SEVERE, null, ex);
		}
		return;
	}
}
