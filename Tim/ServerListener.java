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

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.PartEvent;

/**
 *
 * @author Matthew Walker
 */
public class ServerListener extends ListenerAdapter {
	@Override
	public void onKick( KickEvent event ) {
		if (!event.getSource().getNick().equals(Tim.bot.getNick())) {
			Tim.db.deleteChannel(event.getChannel().getName());
		}
	}

	@Override
	public void onInvite (InviteEvent event) {
		if (!Tim.db.ignore_list.contains(event.getUser())) {
			if (!Tim.db.channel_data.containsKey(event.getChannel())) {
				Tim.bot.joinChannel(event.getChannel());
				Tim.db.saveChannel(event.getChannel());
			}
		}
	}
	
	@Override
	public void onPart( PartEvent event ) {
		if (!event.getUser().getNick().equals(Tim.bot.getNick())) {
			Set<User> userlist = event.getChannel().getUsers();
			if (userlist.size() <= 1) {
				Tim.bot.partChannel(event.getChannel());
				Tim.db.deleteChannel(event.getChannel().getName());
			}
		}
	}

	@Override
	public void onJoin( JoinEvent event ) {
		if (!event.getUser().getNick().equals(Tim.bot.getNick())) {
			String message = String.format(Tim.db.greetings.get(Tim.rand.nextInt(Tim.db.greetings.size())), event.getUser().getNick());
			
			if (Tim.warticker.wars.size() > 0) {
				int warscount = 0;
				String winfo = "";
				
				for (Map.Entry<String, WordWar> wm : Tim.warticker.wars.entrySet()) {
					if (wm.getValue().getChannel().equals(event.getChannel())) {
						winfo += wm.getValue().getDescription();
						if (warscount > 0) {
							winfo += " || ";
						}
						warscount++;
					}
				}
				
				if (warscount > 0) {
					boolean plural = warscount >= 2;
					message += " There " + ( plural ? "are" : "is" ) + " " + warscount + " war" + (plural ? "S" : "")
						+ " currently running in this channel: " + winfo;
				}
			}
			
			Tim.sendDelayedMessage(event.getChannel(), message, 500);

			if (Pattern.matches("(?i)mib_......", event.getUser().getNick()) || Pattern.matches("(?i)guest.*", event.getUser().getNick())) {
				Tim.sendDelayedMessage(
					event.getChannel(), 
					String.format("%s: To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere", event.getUser().getNick()), 
					500);
			}

			int r = Tim.rand.nextInt(100);

			if (r < 15) {
				r = Tim.rand.nextInt(Tim.db.extra_greetings.size());
				Tim.sendDelayedMessage(event.getChannel(), Tim.db.extra_greetings.get(r), 500);
			}
		}
	}
}
