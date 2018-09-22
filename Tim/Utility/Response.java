package Tim.Utility;

public class Response {
	private static String messageFormat = "%s: %s";

	public static void sendResponse(String channel, String user, String message) {
		Tim.Tim.bot.sendIRC()
				   .message(channel, String.format(Response.messageFormat, user, message));
	}
}
