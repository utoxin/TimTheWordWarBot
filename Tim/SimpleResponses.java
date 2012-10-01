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
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.MessageEvent;

/**
 *
 * @author Matthew Walker
 */
public class SimpleResponses extends ListenerAdapter {
	@Override
	public void onMessage(MessageEvent event) {
		String message = event.getMessage();
		PircBotX bot = event.getBot();

		// Other fun stuff we can make him do
		if (message.toLowerCase().contains("how many lights")) {
			bot.sendMessage(event.getChannel(), "There are FOUR LIGHTS!");
		} else if (message.contains(":(") || message.contains("):")) {
			bot.sendAction(event.getChannel(), "gives " + event.getUser().getNick() + " a hug.");
		} else if (message.contains(":'(")) {
			bot.sendAction(event.getChannel(), "passes " + event.getUser().getNick() + " a tissue.");
		} else if (Pattern.matches("(?i).*how do i (change|set) my (nick|name).*", message)) {
			event.respond("To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere");
		}
	}

	@Override
	public void onAction(ActionEvent event) {
		String message = event.getMessage();
		PircBotX bot = event.getBot();

		// Other fun stuff we can make him do
		if (message.toLowerCase().contains("how many lights")) {
			bot.sendMessage(event.getChannel(), "There are FOUR LIGHTS!");
		} else if (message.contains(":(") || message.contains("):")) {
			bot.sendAction(event.getChannel(), "gives " + event.getUser().getNick() + " a hug.");
		} else if (message.contains(":'(")) {
			bot.sendAction(event.getChannel(), "passes " + event.getUser().getNick() + " a tissue.");
		} else if (Pattern.matches("(?i).*how do i (change|set) my (nick|name).*", message)) {
			event.respond("To change your name type the following, putting the name you want instead of NewNameHere: /nick NewNameHere");
		}
	}
}
