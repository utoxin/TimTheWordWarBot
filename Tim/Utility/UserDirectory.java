package Tim.Utility;

import Tim.Data.UserData;
import Tim.Tim;
import org.apache.commons.lang3.time.DateUtils;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

import java.sql.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class UserDirectory extends ListenerAdapter {
	// Main lookup collections, for finalized data
	private ConcurrentHashMap<String, UserData> authLookup = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, UserData> nickLookup = new ConcurrentHashMap<>();
	
	// Temp storage collection, for pending lookups
	private ConcurrentHashMap<String, UserData> tempStorage = new ConcurrentHashMap<>();
	
	// Semaphore for making sure we don't request the same data many times in a row
	private Semaphore mutex = new Semaphore(0);
	
	public UserDirectory() {
		doInitialDbLoad();
		mutex.release(1);
	}
	
	public UserData findUserData(String nick) {
		nick = nick.toLowerCase();

		UserData userData = findUserData(nick, false);
		
		if (userData == null) {
			sendWhois(nick);
		}
		
		return userData;
	}
	
	private UserData findUserData(String nick, boolean includeTempData) {
		nick = nick.toLowerCase();
		
		UserData returnData = authLookup.get(nick);

		if (returnData == null) {
			returnData = nickLookup.get(nick);
		}
		
		if (returnData == null && includeTempData) {
			returnData = tempStorage.get(nick);
		}
		
		return returnData;
	}
	
	@Override
	public void onNickChange(NickChangeEvent event) {
		if (event.getUser() != null && !event.getUser().getNick().equalsIgnoreCase(Tim.bot.getNick())) {
			String oldNick = event.getOldNick().toLowerCase();
			String newNick = event.getNewNick().toLowerCase();

			if (nickLookup.containsKey(oldNick)) {
				UserData data = nickLookup.get(oldNick);
				nickLookup.put(newNick, data);
				nickLookup.remove(oldNick);
			}

			if (tempStorage.containsKey(oldNick)) {
				UserData data = tempStorage.get(oldNick);
				tempStorage.put(newNick, data);
				tempStorage.remove(oldNick);
			}

			handleNickEvent(newNick);
		}
	}
	
	@Override
	public void onJoin(JoinEvent event) {
		if (event.getUser() != null && !event.getUser().getNick().equalsIgnoreCase(Tim.bot.getNick())) {
			String nick = event.getUser().getNick().toLowerCase();
			handleNickEvent(nick);
		}
	}
	
	@Override
	public void onUserList(UserListEvent event) {
		List<User> users = event.getUsers().asList();
		for(User user : users) {
			handleNickEvent(user.getNick().toLowerCase());
		}
	}

	@Override
	public void onMessage(MessageEvent event) {
		if (event.getUser() != null && !event.getUser().getNick().equalsIgnoreCase(Tim.bot.getNick())) {
			String nick = event.getUser().getNick().toLowerCase();
			handleNickEvent(nick);
		}
	}

	@Override
	public void onAction(ActionEvent event) {
		if (event.getUser() != null && !event.getUser().getNick().equalsIgnoreCase(Tim.bot.getNick())) {
			String nick = event.getUser().getNick().toLowerCase();
			handleNickEvent(nick);
		}
	}

	private void handleNickEvent(String nick) {
		try {
			mutex.acquire(1);
			
			UserData userData = findUserData(nick, true);
			
			if (userData == null) {
				userData = new UserData();
				userData.id = UUID.randomUUID();
				userData.nicks.add(nick);
				userData.lastWhoisCheck = new Date();
				
				tempStorage.put(nick, userData);
				
				Tim.bot.send().message("#commandcenter", "Sending initial whois request for '" + nick + "'.");
				sendWhois(nick);
			} else if (!userData.registrationDataRetrieved && DateUtils.addMinutes(userData.lastWhoisCheck, 10).compareTo(new Date()) < 1) {
				userData.lastWhoisCheck = new Date();

				Tim.bot.send().message("#commandcenter", "Sending another whois request for '" + nick + "'.");
				sendWhois(nick);
			}
			
			cleanupTempList();
			mutex.release(1);
		} catch (InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}
	
	@Override
	public void onWhois(WhoisEvent event) {
		try {
			mutex.acquire(1);
			
			String nick = event.getNick().toLowerCase();
			String authedAs;
			
			if (event.isRegistered()) {
				authedAs = event.getRegisteredAs().equals("") ? nick : event.getRegisteredAs().toLowerCase();
				Tim.bot.send().message("#commandcenter", "Received whois response. Nick: '" + nick + "'. Registered as: '" + authedAs + "'");
			} else {
				authedAs = "";
				Tim.bot.send().message("#commandcenter", "Received whois response. Nick: '" + nick + "'. Not registered.");
			}

			if (!authedAs.equals("")) {
				UserData userData = authLookup.get(authedAs);
				if (userData != null) {
					Tim.bot.send().message("#commandcenter", "Added '" + nick + "' to existing user '" + userData.id + "'");
					userData.nicks.add(nick);
				} else {
					userData = tempStorage.get(nick);
					if (userData != null) {
						Tim.bot.send().message("#commandcenter", "Moving '" + nick + "' to permanent user '" + userData.id + "'");
						userData.registrationDataRetrieved = true;
						userData.authedUser = authedAs;
						userData.nicks.add(nick);
					} else {
						Tim.bot.send().message("#commandcenter", "Added '" + nick + "' to data for entirely new user. How did this happen?");
						userData = new UserData();
						userData.id = UUID.randomUUID();
						userData.authedUser = authedAs;
						userData.nicks.add(nick);
						userData.registrationDataRetrieved = true;
					}
					
					userData.save();
				}
				
				authLookup.put(authedAs, userData);
				for (String loopNick : userData.nicks) {
					nickLookup.put(loopNick, userData);
				}
			}
			
			mutex.release(1);
		} catch (InterruptedException ex) {
			Tim.printStackTrace(ex);
		}
	}
	
	private void sendWhois(String nick) {
		Tim.bot.sendIRC().whois(nick);
	}
	
	private void cleanupTempList() {
		for (String nick : tempStorage.keySet()) {
			UserData user = tempStorage.get(nick);
			if (!user.registrationDataRetrieved && DateUtils.addHours(user.lastWhoisCheck, 1).compareTo(new Date()) < 0) {
				Tim.bot.send().message("#commandcenter", "Removing stale temp user: '" + nick + "'");
				tempStorage.remove(nick);
			}
		}
	}
	
	private void doInitialDbLoad() {
		try (Connection con = Tim.db.pool.getConnection(3000)) {
			Statement s = con.createStatement();
			s.executeQuery("SELECT * FROM `users`");
			
			ResultSet rs = s.getResultSet();
			this.nickLookup.clear();
			this.authLookup.clear();
			
			while (rs.next()) {
				UserData user = new UserData();
				user.id = UUID.fromString(rs.getString("id"));
				user.authedUser = rs.getString("authed_user");
				user.totalSprintWordcount = rs.getInt("total_sprint_wordcount");
				user.totalSprints = rs.getInt("total_sprints");
				user.totalSprintDuration = rs.getInt("total_sprint_duration");
				user.raptorAdopted = rs.getBoolean("raptor_adopted");
				user.raptorName = rs.getString("raptor_name");
				user.raptorFavoriteColor = rs.getString("raptor_favorite_color");
				user.raptorBunniesStolen = rs.getInt("raptor_bunnies_stolen");
				user.lastBunnyRaid = rs.getDate("last_bunny_raid");
				user.registrationDataRetrieved = true;
				
				this.nickLookup.put(user.authedUser, user);
				this.authLookup.put(user.authedUser, user);
			}
		} catch (SQLException ex) {
			Tim.printStackTrace(ex);
		}
	}
}
