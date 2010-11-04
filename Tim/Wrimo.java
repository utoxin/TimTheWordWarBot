package Tim;

public class Wrimo {

    private String irc_nick;
    private int nano_id;
    private String nano_nick;
    private long word_count;

    public String getIrc_nick() {
        return irc_nick;
    }

    public int getNano_id() {
        return nano_id;
    }

    public String getNano_nick() {
        return nano_nick;
    }

    public long getWord_count() {
        return word_count;
    }

    public void setIrc_nick(String irc_nick) {
        this.irc_nick = irc_nick;
    }

    public void setNano_id(int nano_id) {
        this.nano_id = nano_id;
    }

    public void setNano_nick(String nano_nick) {
        this.nano_nick = nano_nick;
    }

    public void setWord_count(long word_count) {
        this.word_count = word_count;
    }

    @Override
    public String toString() {
        return "Wrimo info: " + this.irc_nick + " is " + this.nano_nick
                + " on the NaNoWriMo website, #" + this.nano_id
                + " and currently has " + this.word_count + " words written.";
    }
}
