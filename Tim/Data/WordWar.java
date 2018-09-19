package Tim.Data;

import Tim.Tim;
import org.pircbotx.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.UUID;

public class WordWar {
	public enum State {
		PENDING, ACTIVE, CANCELLED, FINISHED
	}

	public short year;
	public short warId;
	public UUID uuid;

	public String channel;
	public String starter;
	public String name;

	public int baseDuration;
	public int baseBreak;

	public byte totalChains;
	public byte currentChain;

	public long startEpoch;
	public long endEpoch;

	public boolean randomness;

	public State warState;

	public ChannelInfo cdata;

	public HashSet<String> warMembers = new HashSet<>();

	public WordWar(ChannelInfo cdata, User startingUser, String name, int baseDuration, long startEpoch) {
		this.uuid = UUID.randomUUID();
		this.channel = cdata.channel;
		this.starter = startingUser.getNick();
		this.name = name;

		this.baseDuration = baseDuration;
		this.startEpoch = startEpoch;
		this.endEpoch = startEpoch + baseDuration;

		this.totalChains = 1;
		this.currentChain = 1;
		this.randomness = false;

		this.warState = State.PENDING;

		this.cdata = cdata;

		Calendar cal = Calendar.getInstance();

		this.year = (short) cal.get(Calendar.YEAR);
		Tim.db.create_war(this);
	}

	public WordWar(ChannelInfo cdata, User startingUser, String name, int baseDuration, long startEpoch, byte totalChains, int baseBreak, boolean randomness) {
		this.uuid = UUID.randomUUID();
		this.channel = cdata.channel;
		this.starter = startingUser.getNick();
		this.name = name;

		this.baseDuration = baseDuration;
		this.baseBreak = baseBreak;
		this.startEpoch = startEpoch;
		this.endEpoch = startEpoch + baseDuration;

		this.totalChains = totalChains;
		this.currentChain = 1;
		this.randomness = randomness;

		this.warState = State.PENDING;

		this.cdata = cdata;

		Calendar cal = Calendar.getInstance();

		this.year = (short) cal.get(Calendar.YEAR);
		Tim.db.create_war(this);
	}

	public WordWar(short year, short warId, UUID uuid, String channel, String starter, String name, int baseDuration,
				   int baseBreak, byte totalChains, byte currentChain, long startEpoch, long endEpoch,
				   boolean randomness, State warState) {
		this.uuid = uuid;
		this.year = year;
		this.warId = warId;
		this.channel = channel;
		this.starter = starter;
		this.name = name;
		this.baseDuration = baseDuration;
		this.baseBreak = baseBreak;
		this.totalChains = totalChains;
		this.currentChain = currentChain;
		this.startEpoch = startEpoch;
		this.endEpoch = endEpoch;
		this.randomness = randomness;
		this.warState = warState;

		String channelName = channel.toLowerCase();
		this.cdata = Tim.db.channel_data.get(channelName);

		if (this.cdata == null) {
			Tim.logErrorString("PANIC!!!!!!");
			Tim.logErrorString("Failed To Get ChannelInfo For: " + channelName);
		}
	}

	public void endWar() {
		this.warState = State.FINISHED;
		this.updateDb();
	}

	public void cancelWar() {
		this.warState = State.CANCELLED;
		this.updateDb();
	}

	public void beginWar() {
		this.warState = State.ACTIVE;
		this.updateDb();
	}

	public void chainWarBreak() {
		this.warState = State.PENDING;
		this.updateDb();
	}

	public long duration() {
		return this.endEpoch - this.startEpoch;
	}

	private void updateDb() {
		Tim.db.updateWar(this);
	}

	public String getChannel() {
		return this.channel;
	}

	public String getName(boolean includeId, boolean includeDuration, int idFieldWidth, int durationFieldWidth) {
		ArrayList<String> nameParts = new ArrayList<>();

		if (includeId) {
			String db_id = String.format("%d-%d", this.year, this.warId);
			nameParts.add(String.format("[ID %"+idFieldWidth+"s]", db_id));
		}

		if (includeDuration) {
			nameParts.add(String.format("[%"+durationFieldWidth+"s]", getDurationText(this.endEpoch - this.startEpoch)));
		}

		nameParts.add(name);

		if (this.totalChains > 1) {
			nameParts.add(String.format("(%d/%d)", this.currentChain, this.totalChains));
		}

		return String.join(" ", nameParts);
	}

	public String getName(boolean includeId, boolean includeDuration) {
		return getName(includeId, includeDuration, 1, 1);
	}

	public String getSimpleName() {
		return getName(false, false);
	}

	public String getInternalName() {
		return name.toLowerCase();
	}

	public String getDurationText(long duration) {
		String text = "";
		long hours = 0, minutes = 0, seconds, tmp;

		tmp = duration;

		if (tmp > (60 * 60)) {
			hours = tmp / (60 * 60);
			tmp = tmp % (60 * 60);
		}

		if (tmp > 60) {
			minutes = tmp / 60;
			tmp = tmp % 60;
		}

		seconds = tmp;

		if (hours > 0) {
			text += hours + "H ";
		}

		if (minutes > 0 || (seconds > 0 && hours > 0)) {
			text += minutes + "M ";
		}

		if (seconds > 0) {
			text += seconds + "S";
		}

		return text.trim();
	}

	public String getStarter() {
		return starter;
	}

	public String getDescription() {
		return getDescription(1, 1);
	}

	public String getDescription(int idFieldWidth, int durationFieldWidth) {
		long currentEpoch = System.currentTimeMillis() / 1000;

		String about = this.getName(true, true, idFieldWidth, durationFieldWidth) + " :: ";
		if (currentEpoch < this.startEpoch) {
			about += "Starts In: ";
			about += getDurationText(this.startEpoch - currentEpoch);
		} else {
			about += "Ends In: ";
			about += getDurationText(this.endEpoch - currentEpoch);
		}

		return about;
	}

	public String getDescriptionWithChannel(int idFieldWidth, int durationFieldWidth) {
		return this.getDescription(idFieldWidth, durationFieldWidth) + " :: " + this.channel;
	}

	public void addMember(String nick) {
		warMembers.add(nick);
		Tim.db.saveWarMembers(this);
	}

	public void removeMember(String nick) {
		warMembers.remove(nick);
		Tim.db.saveWarMembers(this);
	}
}
