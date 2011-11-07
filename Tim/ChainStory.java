/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Tim;

/**
 *
 * @author mwalker
 */
public class ChainStory {
	Tim ircclient;

	public ChainStory(Tim ircclient) {
		this.ircclient = ircclient;
	}

	public boolean parseUserCommand(String channel, String sender, String prefix, String message) {
		String command;
		String[] args = null;

		int space = message.indexOf(" ");
		if (space > 0) {
			command = message.substring(1, space).toLowerCase();
			args = message.substring(space + 1).split(" ", 0);
		}
		else {
			command = message.substring(1).toLowerCase();
		}

		if (prefix.equals("!")) {
			if (command.equals("testchain")) {
				this.ircclient.sendMessage(channel, "This is a test");
				return true;
			}
		}
		
		return false;
	}
}
