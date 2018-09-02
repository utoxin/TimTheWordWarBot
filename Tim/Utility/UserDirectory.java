package Tim.Utility;

import Tim.Data.UserData;
import Tim.Tim;
import org.apache.commons.lang3.time.DateUtils;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.*;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class UserDirectory extends ListenerAdapter {
	// Main lookup collections, for finalized data
	private HashMap<String, UserData> authLookup = new HashMap<>();
	private HashMap<String, UserData> nickLookup = new HashMap<>();
	
	// Temp storage collection, for pending lookups
	private HashMap<String, UserData> tempStorage = new HashMap<>();
	
	public UserData findUserData(String nick) {
		return findUserData(nick, false);
	}
	
	public UserData findUserData(String nick, boolean includeTempData) {
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
	public void onJoin(JoinEvent event) {
		if (event.getUser() != null && !event.getUser().getNick().equals(Tim.bot.getNick())) {
			userAdditionChecks(event.getUser().getNick());
		}
	}
	
	@Override
	public void onWhois(WhoisEvent event) {
		String nick = event.getNick();
		String authedAs = event.getRegisteredAs();
		
		if (!authedAs.equals("")) {
			UserData userData = authLookup.get(authedAs);
			if (userData != null) {
				userData.nicks.add(nick);
			} else {
				userData = tempStorage.get(nick);
				if (userData != null) {
					userData.registrationDataRetrieved = true;
					userData.authedUser = authedAs;
					userData.nicks.add(nick);
				} else {
					userData = new UserData();
					userData.id = UUID.randomUUID();
					userData.authedUser = authedAs;
					userData.nicks.add(nick);
					userData.registrationDataRetrieved = true;
				}
			}
			
			authLookup.put(authedAs, userData);
			for (String loopNick : userData.nicks){
				nickLookup.put(loopNick, userData);
			}
		}
	}
	
	private void userAdditionChecks(String nick) {
		UserData userData = findUserData(nick, true);
		if (userData == null) {
			userData = new UserData();
			userData.id = UUID.randomUUID();
			userData.nicks.add(nick);
			userData.lastWhoisCheck = new Date();
			
			tempStorage.put(nick, userData);
			
			sendWhois(nick);
		} else if (!userData.registrationDataRetrieved
				&& DateUtils.addMinutes(userData.lastWhoisCheck, 1).compareTo(new Date()) < 1) {
			sendWhois(nick);
		}
	}

	private void sendWhois(String nick) {
		Tim.bot.sendIRC().whois(nick);
	}
}
