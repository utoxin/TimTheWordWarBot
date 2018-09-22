package Tim;

import java.util.concurrent.TimeUnit;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;

class ConnectListener extends ListenerAdapter {
	@Override
	public void onConnect(ConnectEvent event) {
		String post_identify = Tim.db.getSetting("post_identify");
		if (!"".equals(post_identify)) {
			String[] post_identify_lines = post_identify.split("\n");

			for (String line : post_identify_lines) {
				if (!line.equals("")) {
					event.respond(line);
				}
			}
		}

		boolean gotSemaphore = false;
		try {
			Tim.connectSemaphore.tryAcquire(1, 30, TimeUnit.SECONDS);
			gotSemaphore = true;
		} catch (InterruptedException ex) {
			Tim.printStackTrace(ex);
		}

		Tim.warticker = WarTicker.getInstance();
		Tim.deidler = DeIdler.getInstance();

		if (!Tim.db.getSetting("twitter_access_key")
				   .equals("")) {
			Tim.twitterStream = new TwitterIntegration();
			Tim.twitterStream.startStream();
		}

		if (gotSemaphore) {
			Tim.connectSemaphore.release(1);
		}
	}
}
