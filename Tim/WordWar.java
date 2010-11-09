package Tim;

/**
 * 
 * @author Marc
 *
 */
public class WordWar
{

    private String channel;
    private long duration;
    private String name;
    public long remaining;
    private String starter;
    public long time_to_start;

    public WordWar(long time, long to_start, String warname,
            String startingUser, String hosting_channel)
    {
        this.starter = startingUser;
        this.time_to_start = to_start;
        this.duration = this.remaining = time;
        this.name = warname;
        this.channel = hosting_channel;
    }

    public String getChannel()
    {
        return this.channel;
    }

    public long getDuration()
    {
        return duration;
    }

    public String getName()
    {
        return name;
    }

    public String getStarter()
    {
        return starter;
    }

    public long getTime_to_start()
    {
        return time_to_start;
    }

    public String getDescription()
    {
        int count = 0;
        long minutes;
        long seconds;
        minutes = seconds = 0;
        count++;
        String about = "WordWar '" + this.getName() + "':";
        if (this.time_to_start > 0)
        {
            minutes = this.time_to_start / 60;
            seconds = this.time_to_start % 60;
            about += " starts in ";
        } else
        {
            minutes = this.remaining / 60;
            seconds = this.remaining % 60;
            about += " ends in ";
        }
        if (minutes > 0)
        {
            about += minutes + " minutes";
            if (seconds > 0)
            {
                about += " and ";
            }
        }
        if (seconds > 0)
        {
            about += seconds + " seconds";
        }
        return about;
    }

    public String getDescriptionWithChannel()
    {
        return this.getDescription() + " in " + this.channel;
    }
}
