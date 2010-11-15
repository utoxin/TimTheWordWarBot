package Tim;

import java.io.IOException;
import java.sql.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import org.jibble.pircbot.*;

public class Tim extends PircBot
{

    public static AppConfig config = AppConfig.getInstance();

    /*
    Defining these at the top so I can conveniently change
    them in one place between checkins
     */
    public String autonick = "Timmy";
    public static String[] autochannels =
    {
        "#bottest"
    };
    public static String autoserver = "irc.kydance.net";

    /* private static String dbserver = "192.168.2.51";
    private static String dbdbase = "";
    private static String dbuser = "";
    private static String dbpass = "";*/
    public class WarClockThread extends TimerTask
    {

        private Tim parent;

        public WarClockThread(Tim pparent)
        {
            this.parent = pparent;
        }

        public void run()
        {
            try
            {
                parent._tick();
            } catch (Throwable t)
            {
                System.out.println("&&& THROWABLE CAUGHT in DelayCommand.run:");
                t.printStackTrace(System.out);
                System.out.flush();
            }
        }
    }

    protected enum ActionType
    {

        MESSAGE, ACTION, NOTICE
    };

    public class DelayCommand extends TimerTask
    {

        private ActionType type;
        private Tim parent;
        private String text;
        private String target;

        public DelayCommand(Tim pparent, String target, String text, ActionType type)
        {
            this.parent = pparent;
            this.text = text;
            this.target = target;
            this.type = type;
        }

        public void run()
        {
            try
            {
                switch (this.type)
                {
                    case MESSAGE:
                        this.parent.sendMessage(this.target, this.text);
                        break;
                    case ACTION:
                        this.parent.sendAction(this.target, this.text);
                        break;
                    case NOTICE:
                        this.parent.sendNotice(this.target, this.text);
                }
            } catch (Throwable t)
            {
                System.out.println("&&& THROWABLE CAUGHT in DelayCommand.run:");
                t.printStackTrace(System.out);
                System.out.flush();
                this.parent.sendMessage(target, "I couldn't schedule your command for some reason:");
                this.parent.sendMessage(target, t.toString());
            }

        }
    }

    private static String[] fridgeColours =
    {
        "avocado green", "powder blue", "pale yellow", "sienna", "crimson", "lime green", "fushia",
        "orangeish yellow", "neon pink", "topaz", "tope", "silver", "anguish", "teal", "aqua", "purple",
        "beige", "burgundy", "scarlet", "navy", "turquoise", "cerulean", "olive", "black", "chocolate", "beige",
        "invisible", "witchling", "maroon", "#BBC401", "oxide of chromium", "aubergine", "harvest gold"
    };
    private static String[] eightballResponses =
    {
        "Direction unclear", "Yes", "No", "Maybe, if you wiggle it a bit", "It depends", "Sometimes",
        "Ask later", "Absolutely", "Porcupine", "Captain Picard is not aboard the Enterprise", "Probably",
        "I think so", "It may be difficult", "No problem", "You have nothing to fear", "It is assured",
        "Have you tried hitting it?", "Maybe", "Yes", "Absolutely", "No", "Never", "Not in a million years",
        "I'm sure of it", "Answer unclear", "Try again later", "Ask me again tomorrow", "No, you!",
        "That's what she said", "Only if you hop on one foot and sing a song", "Have a drink and ask again",
        "Get me a drink and ask again", "Indubidably", "What, you never just want to say hi?", "Never in your wildest dreams",
        "Only in your wildest dreams", "HAHAHAHAHAHAHAHAhahahaaa...  haahaaa...  phew...  Oh yeah, totally do it.",
        "You're joking!", "What even is that?", "Penguins", "Ninjas", "Zombies", "Pirates", "Robots",
        "Throw a fridge at something", "The power of Baty compels you!", "Christ Baty wills it."
    };
    private static String[] greetings =
    {
        "Welcome to the Congregation of Christ Baty!", "Have you backed up your novel today?",
        "Have you thanked an ML or Staff member recently?",
    };
    private static String[] commandments =
    {
        "1. Thou shalt not edit during the Holy Month.", "2. Thou shalt daily offer up at least 1,667 words to the altar of Christ Baty.",
        "3. Keep thou holy the first and last days of the Holy Month, which is Novemeber.",
        "4. Take not the name of Christ Baty in vain, unless it doth provide thee with greater word count, which is good.",
        "5. Worry not about the quality of thy words, for Christ Baty cares not. Quantity is that which pleases Baty.",
        "6. Thou must tell others of the way of the truth, by leading by example in your region.",
        "7. Honor thou those who sacrifice their time. They are known as MLs and Staff members, and they are blessed.",
        "8. Once in your life, ye shall make a pilgrimage to NOWD to honor thine Christ Baty",
        "9. Those that sacrifice their money shall be blessed with a halo of gold, which shall be a sign unto others.", "10. <<WRITE THIS LATER>>",
        "11. Thou shalt back up thy novel often, for it is displeasing in the eyes of Baty that you should lose it.",
        "12. No narrative? No botheration!",
    };
    //	private Wrimo[] wrimos;
    private Map<String, WordWar> wars;
    private WarClockThread warticker;
    private Timer ticker;
    private Semaphore wars_lock, timer_lock;
    private Fridge fridge;
    private Random rand;
    private boolean shutdown;
    private Collection ignore_list;
    private Collection admin_list;
    private String password;
    //public  MySQL db;

    public Tim()
    {
        Object nicks = Tim.config.getProperty("nicks.nick");
        if (nicks instanceof Collection)
        {
            this.setName((String) Tim.config.getProperty("nicks.nick(0).name"));
            this.password = (String) Tim.config.getProperty("nicks.nick(0).pass");
        } else
        {
            this.setName((String) Tim.config.getProperty("nicks.nick.name"));
            this.password = (String) Tim.config.getProperty("nicks.nick.pass");
        }

        this.ignore_list = (Collection) Tim.config.getProperty("ignore.nick");
        this.admin_list = (Collection) Tim.config.getProperty("admins.admin");

        wars = Collections.synchronizedMap(new HashMap<String, WordWar>());
        warticker = new WarClockThread(this);
        ticker = new Timer(true);
        ticker.scheduleAtFixedRate(warticker, 0, 1000);
        wars_lock = new Semaphore(1, true);
        timer_lock = new Semaphore(1, true);
        rand = new Random();
        this.shutdown = false;
        //this.db = new MySQL(Tim.dbserver, Tim.dbserver, Tim.dbuser, Tim.dbpass);
    }

    // Destructor. Sort of. I think?
    @Override
    public synchronized void dispose()
    {
        //this.db.Close();
        super.dispose();
    }

    /*
    delay is in milliseconds
     */
    public void sendDelayedMessage(String target, String message, int delay)
    {
        DelayCommand talk = new DelayCommand(this, target, message, ActionType.MESSAGE);
        this.ticker.schedule(talk, delay);
    }

    public void sendDelayedAction(String target, String action, int delay)
    {
        DelayCommand act = new DelayCommand(this, target, action, ActionType.ACTION);
        this.ticker.schedule(act, delay);
    }

    public void sendDelayedNotice(String target, String action, int delay)
    {
        DelayCommand act = new DelayCommand(this, target, action, ActionType.NOTICE);
        this.ticker.schedule(act, delay);
    }

    @Override
    protected void onAction(String sender, String login, String hostname, String target, String action)
    {
        if (this.admin_list.contains(sender))
        {
            if (action.equalsIgnoreCase("punches " + this.getNick() + " in the face!"))
            {
                this.sendAction(target, "falls over and dies.  x.x");
                this.shutdown = true;
                this.quitServer();
                System.exit(0);
            }
        }
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        if (this.ignore_list.contains(sender))
        {
            return;
        } else
        {
            // Find all messages that start with ! and pass them to a method for further processing.
            if (message.charAt(0) == '!')
            {
                this.doCommand(channel, sender, "!", message);
            } // Notation for wordcounts
            else if (message.charAt(0) == '@')
            {
                this.doCommand(channel, sender, "@", message);
            }
            // Other fun stuff we can make him do
            if (message.toLowerCase().contains("hello") && message.toLowerCase().contains(this.getNick().toLowerCase()))
            {
                this.sendMessage(channel, "Hi, " + sender + "!");
            }
            if (message.toLowerCase().startsWith("how many lights"))
            {
                this.sendMessage(channel, "There are FOUR LIGHTS!");
            }
            if (message.startsWith(":("))
            {
                this.sendAction(channel, "gives " + sender + " a hug");
            }
        }
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message)
    {
        if (this.admin_list.contains(sender))
        {
            String[] args = message.split(" ");
            if (args != null && args.length > 2)
            {
                String msg = "";
                for (int i = 2; i < args.length; i++)
                {
                    msg += args[i] + " ";
                }
                if (args[0].equalsIgnoreCase("say"))
                {
                    this.sendMessage(args[1], msg);
                } else if (args[0].equalsIgnoreCase("act"))
                {
                    this.sendAction(args[1], msg);
                }
            }
        }
    }

    @Override
    protected void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String channel)
    {
        if (!this.ignore_list.contains(sourceNick) && targetNick.equals(this.getNick()))
        {
            String[] chanlist = this.getChannels();
            boolean isIn = false;
            for (int i = 0; i < chanlist.length; i++)
            {
                if (chanlist[i].equalsIgnoreCase(channel))
                {
                    isIn = true;
                    break;
                }
            }
            if (!isIn)
            {
                this.joinChannel(channel);
            }
        }
    }

    @Override
    protected void onPart(String channel, String sender, String login, String hostname)
    {
        if (!sender.equals(this.getNick()))
        {
            User[] userlist = this.getUsers(channel);
            if (userlist.length <= 1)
            {
                this.partChannel(channel);
            }
        }
    }

    @Override
    public void onNotice(String sender, String nick, String hostname, String target, String notice)
    {
        if (notice.contains("This nickname is registered"))
        {
            this.sendMessage("NickServ", "identify " + this.password);
        }
    }

    @Override
    protected void onDisconnect()
    {
        if (!this.shutdown)
        {
            this.connectToServer();
        }
    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostname)
    {
        if (!sender.equals(this.getName()) && !login.equals(this.getLogin()))
        {
            String message = "Hello, " + sender + "!";
            if (this.wars.size() > 0)
            {
                int warscount = 0;
                String winfo = "";
                for (Map.Entry<String, WordWar> wm : this.wars.entrySet())
                {
                    if (wm.getValue().getChannel().equalsIgnoreCase(channel))
                    {
                        winfo += wm.getValue().getDescription();
                        if (warscount > 0)
                        {
                            winfo += " || ";
                        }
                        warscount++;
                    }
                }

                if (warscount > 0)
                {
                    boolean plural = warscount >= 2 || warscount == 0;
                    message += " There " + ( ( plural ) ? "are" : "is" ) + " " + warscount
                               + " war" + ( ( plural ) ? "s" : "" ) + " currently "
                               + "running in this channel" + ( ( warscount > 0 ) ? ( ": " + winfo ) : "." );
                }
            }
            this.sendDelayedMessage(channel, message, 1600);

            int r = this.rand.nextInt(100);

            if (r < 5)
            {
                r = this.rand.nextInt(Tim.greetings.length);
                this.sendDelayedMessage(channel, Tim.greetings[r], 2400);
            }
        }
    }

    public void doCommand(String channel, String sender, String prefix, String message)
    {
        String command;
        String[] args = null;

        int space = message.indexOf(" ");
        if (space > 0)
        {
            command = message.substring(1, space).toLowerCase();
            args = message.substring(space + 1).split(" ", 0);
        } else
        {
            command = message.substring(1).toLowerCase();
        }
        if (prefix.equals("!"))
        {
            if (command.equals("startwar"))
            {
                if (args != null && args.length > 1)
                {
                    this.startWar(channel, sender, args);
                } else
                {
                    this.sendMessage(channel, "Use: !startwar <duration in min> [<time to start in min> [<name>]]");
                }
            } else if (command.equals("startjudgedwar"))
            {
                this.sendMessage(channel, "Not done yet, sorry!");
            } else if (command.equals("joinwar"))
            {
                this.sendMessage(channel, "Not done yet, sorry!");
            } else if (command.equals("endwar"))
            {
                this.endWar(channel, sender, args);
            } else if (command.equals("listwars"))
            {
                this.listWars(channel, sender, args, false);
            } else if (command.equals("listall"))
            {
                this.listAllWars(channel, sender, args);
            } else if (command.equals("eggtimer"))
            {
                double time = 15;
                if (args != null)
                {
                    try
                    {
                        time = Double.parseDouble(args[0]);
                    } catch (Exception e)
                    {
                        this.sendMessage(channel, "Could not understand first parameter. Was it numeric?");
                        return;
                    }
                }
                this.sendMessage(channel, sender + ": your timer has been set.");
                this.sendDelayedNotice(sender, "Your timer has expired!", (int) ( time * 60 * 1000 ));
            } else if (command.equals("settopic"))
            {
                if (args != null && args.length > 0)
                {
                    String topic = args[0];
                    for (int i = 1; i < args.length; i++)
                    {
                        topic += " " + args[i];
                    }
                    this.setTopic(channel, topic + " --" + sender);
                }
            } else if (command.equals("sing"))
            {
                int r = this.rand.nextInt(100);
                String response = "";
                if (r > 90)
                {
                    response = "sings a beautiful song";
                } else if (r > 60)
                {
                    response = "chants a snappy ditty";
                } else if (r > 30)
                {
                    response = "starts singing 'It's a Small World'";
                } else
                {
                    response = "screeches, and all the windows shatter";
                }
                this.sendAction(channel, response);
            } else if (command.equals("eightball") || command.equals("8-ball"))
            {
                this.eightball(channel, sender, args);
            } else if (command.equals("woot"))
            {
                this.sendAction(channel, "cheers! Hooray!");
            } else if (command.equals("coffee") || command.equals("drink"))
            {
                this.fridge(channel, sender, args);
            } else if (command.equals("getfor") || command.equals("drinkfor"))
            {
                this.fridgeFor(channel, sender, args);
            } else if (command.equals("fridge"))
            {
                this.throwFridge(channel, sender, args);
            } else if (command.equals("dance"))
            {
                this.sendAction(channel, "dances a cozy jig");
            } else if (command.equals("commandment"))
            {
                this.commandment(channel, sender, args);
            } // add additional commands above here!!
            else if (command.equals("help"))
            {
                String str = "I am a robot trained by the WordWar Monks of Honolulu. You have "
                             + "never heard of them. It is because they are awesome. I am capable "
                             + "of running the following commands:";
                this.sendMessage(channel, str);
                str = "!startwar <duration> <time to start> <an optional name> - Starts a word war";
                this.sendMessage(channel, str);
                str = "!listwars - I will tell you about the wars currently in progress.";
                this.sendMessage(channel, str);
                str = "!eggtimer <time> - I will send you a message after <time> minutes.";
                this.sendMessage(channel, str);
                str = "!coffee - I will get you a nice cup of coffee.";
                this.sendMessage(channel, str);
                str = "!drink <anything> - I will fetch you whatever you like.";
                this.sendMessage(channel, str);
                str = "I will also respond to invite commands if you would like to see me in another channel.";
                this.sendMessage(channel, str);
            } else if (command.equals("shutdown"))
            {
                if (this.admin_list.contains(sender) || this.admin_list.contains(channel))
                {
                    this.sendMessage(channel, "Shutting down...");
                    this.shutdown = true;
                    this.quitServer("I am shutting down! Bye!");
                    System.exit(0);
                } else
                {
                    this.sendAction(channel, "sticks out his tounge");
                    this.sendMessage(channel, "You can't make me, " + sender);
                }
            } else if (command.equals("reset"))
            {
                if (this.admin_list.contains(sender) || this.admin_list.contains(channel))
                {
                    this.sendMessage(channel, "Rebooting ...");

                    warticker = new WarClockThread(this);
                    ticker = new Timer(true);
                    ticker.scheduleAtFixedRate(warticker, 0, 1000);
                    wars_lock = new Semaphore(1, true);
                    timer_lock = new Semaphore(1, true);
                    rand = new Random();
                    this.shutdown = false;

                    this.sendDelayedMessage(channel, "Can you hear me now?", 2400);
                } else
                {
                    this.sendAction(channel, "sticks out his tounge");
                    this.sendMessage(channel, "You can't make me, " + sender);
                }
            } /*else if (command.equals("restarttimer"))
            {
            if (sender.equals("Utoxin") || sender.equals("Sue___b"))
            {
            ticker.cancel();
            ticker = null;
            ticker = new Timer(true);
            ticker.scheduleAtFixedRate(warticker, 0, 1000);
            }
            }*/ else
            {
                this.sendMessage(channel, sender + ": I don't know !" + command + ".");
            }
        } else if (prefix.equals("@"))
        {
            long wordcount;
            try
            {
                wordcount = (long) ( Double.parseDouble(command) );
                for (Map.Entry<String, WordWar> wm : this.wars.entrySet())
                {
                    this.sendMessage(channel, wm.getKey());
                }
            } catch (Exception e)
            {
            }

        }
    }

    private void eightball(String channel, String sender, String[] args)
    {
        int r = this.rand.nextInt(Tim.eightballResponses.length);
        this.sendMessage(channel, Tim.eightballResponses[r]);
    }

    private void commandment(String channel, String sender, String[] args)
    {
        int r = this.rand.nextInt(Tim.commandments.length);
        if (args != null && args.length == 1 && Double.parseDouble(args[0]) > 0 && Double.parseDouble(args[0]) <= Tim.commandments.length) {
            r = (int) Double.parseDouble(args[0]) - 1;
        }
        this.sendMessage(channel, Tim.commandments[r]);
    }

    private void throwFridge(String channel, String sender, String[] args)
    {
        String target = sender;
        if (args != null && args.length > 0)
        {
            if (!args[0].equalsIgnoreCase(this.getNick()) && !args[0].equalsIgnoreCase("himself")
                && !args[0].equalsIgnoreCase("herself") && !this.admin_list.contains(args[0])
                && !args[0].equalsIgnoreCase("myst"))
            {
                target = "";
                for (int i = 0; i < args.length; i++)
                {
                    target += args[i] + " ";
                }
            }
        }
        this.sendMessage(channel, "Righto...");
        int time = 5 + rand.nextInt(15);
        time *= 1000;
        this.sendDelayedAction(channel, "looks back and forth, then slinks off...", time);
        time += rand.nextInt(10) * 500 + 1500;
        String colour = Tim.fridgeColours[rand.nextInt(Tim.fridgeColours.length - 1)];
        switch (colour.charAt(0))
        {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
                colour = "n " + colour;
                break;
            default:
                colour = " " + colour;
        }
        int i = rand.nextInt(100);
        String act = "";
        if (i > 20)
        {
            act = "hurls a" + colour + " coloured fridge at " + target;
        } else if (i > 3)
        {
            target = sender;
            act = "hurls a" + colour + " coloured fridge at " + target + " and runs away giggling";
        } else
        {
            act = "trips and drops a" + colour + " fridge on himself";
        }
        this.sendDelayedAction(channel, act, time);
    }

    private void fridge(String channel, String sender, String[] args)
    {
        if (args != null)
        {
            String drink = args[0];
            for (int i = 1; i < args.length; i++)
            {
                drink = drink + " " + args[i];
            }
            this.sendAction(channel, "gets " + sender + " a fresh cup of " + drink);
        } else
        {
            this.sendAction(channel, "gets " + sender + " a freshly brewed cup of coffee");
        }
    }

    private void fridgeFor(String channel, String sender, String[] args)
    {
        if (args != null && args.length > 0)
        {
            if (args.length > 1)
            {
                String drink = "";
                for (int i = 1; i < args.length; i++)
                {
                    drink = drink + " " + args[i];
                }
                this.sendAction(channel, "gets " + args[0] + " a fresh cup of" + drink);
            } else
            {
                this.sendAction(channel, "gets " + args[0] + " a fresh cup of coffee");
            }
        } else
        {
            this.sendMessage(channel, sender + ": I need someone to give something to.");
        }
    }

    // !endwar <name>
    private void endWar(String channel, String sender, String[] args)
    {
        if (args != null && args.length > 0)
        {
            String name = args[0];
            for (int i = 1; i < args.length; i++)
            {
                name += " " + args[i];
            }
            if (this.wars.containsKey(name.toLowerCase()))
            {
                if (sender.equalsIgnoreCase(this.wars.get(name.toLowerCase()).getStarter())
                    || this.admin_list.contains(sender) || this.admin_list.contains(channel))
                {
                    WordWar war = this.wars.remove(name.toLowerCase());
                    this.sendMessage(channel, "The war '" + war.getName() + "' has been ended.");
                } else
                {
                    this.sendMessage(channel, sender + ": Only the starter of a war can end it early.");
                }
            } else
            {
                this.sendMessage(channel, sender + ": I don't know of a war with name: '" + name + "'");
            }
        } else
        {
            this.sendMessage(channel, sender + ": I need a war name to end.");
        }
    }

    private void startWar(String channel, String sender, String[] args)
    {
        long time;
        long to_start = 5000;
        String warname = "";
        try
        {
            time = (long) ( Double.parseDouble(args[0]) * 60 );
        } catch (Exception e)
        {
            this.sendMessage(channel, sender + ": could not understand the duration parameter. Was it numeric?");
            return;
        }
        if (args.length >= 2)
        {
            try
            {
                to_start = (long) ( Double.parseDouble(args[1]) * 60 );
            } catch (Exception e)
            {
                this.sendMessage(channel, sender + ": could not understand the time to start parameter. Was it numeric?");
                return;
            }

        }
        if (args.length >= 3)
        {
            warname = args[2];
            for (int i = 3; i < args.length; i++)
            {
                warname = warname + " " + args[i];
            }
        } else
        {
            warname = sender + "'s war";
        }

        if (Double.parseDouble(args[0]) < 1 || Double.parseDouble(args[1]) < 1)
        {
            this.sendMessage(channel, sender + ": Start delay and duration most both be at least 1.");
            return;
        }

        if (!this.wars.containsKey(warname.toLowerCase()))
        {
            WordWar war = new WordWar(time, to_start, warname, sender, channel);
            this.wars.put(war.getName().toLowerCase(), war);
            if (to_start > 0)
            {
                this.sendMessage(channel, sender + ": your wordwar will start in " + to_start / 60.0 + " minutes.");
            } else
            {
                this.beginWar(war);
            }
        } else
        {
            this.sendMessage(channel, sender + ": there is already a war with the name '" + warname + "'");
        }
    }

    private void listAllWars(String channel, String sender, String[] args)
    {
        this.listWars(channel, sender, args, true);
    }

    private void listWars(String channel, String sender, String[] args, boolean all)
    {
        String target = ( args != null ) ? sender : channel;
        if (this.wars != null && this.wars.size() > 0)
        {
            for (Map.Entry<String, WordWar> wm : this.wars.entrySet())
            {
                if (all || wm.getValue().getChannel().equalsIgnoreCase(channel))
                {
                    this.sendMessage(target, ( all ) ? wm.getValue().getDescriptionWithChannel()
                                             : wm.getValue().getDescription());
                }
            }
        } else
        {
            this.sendMessage(target, "No wars are currently available.");
        }
    }

    private void _tick()
    {
        this._warsUpdate();
    }

    private void _warsUpdate()
    {
        if (this.wars != null && this.wars.size() > 0)
        {
            try
            {
                this.wars_lock.acquire();
                Iterator<String> itr = this.wars.keySet().iterator();
                WordWar war;
                while (itr.hasNext())
                {
                    war = this.wars.get(itr.next());
                    if (war.time_to_start > 0)
                    {
                        war.time_to_start--;
                        switch ((int) war.time_to_start)
                        {
                            case 30:
                            case 15:
                            case 5:
                            case 4:
                            case 3:
                            case 2:
                            case 1:
                                this.warStartCount(war);
                                break;
                            default:
                                break;
                        }
                        if (war.time_to_start == 0)
                        {
                            this.beginWar(war);
                        }
                    } else if (war.remaining > 0)
                    {
                        war.remaining--;
                        switch ((int) war.remaining)
                        {
                            case 60:
                            case 5:
                            case 4:
                            case 3:
                            case 2:
                            case 1:
                                this.warEndCount(war);
                                break;
                            case 0:
                                this.endWar(war);
                                break;
                            default:
                                if (( (int) war.remaining ) % 300 == 0)
                                {
                                    this.warEndCount(war);
                                }
                                // do nothing
                                break;
                        }
                    }
                }
                this.wars_lock.release();
            } catch (Throwable e)
            {
            }
        }
    }

    private void warStartCount(WordWar war)
    {
        this.sendMessage(war.getChannel(), war.getName() + ": Starting in "
                                           + ( ( war.time_to_start == 60 ) ? "one minute" : war.time_to_start + ( ( war.time_to_start == 1 ) ? " second" : " seconds" ) )
                                           + "!");
    }

    private void warEndCount(WordWar war)
    {
        if (war.remaining < 60)
        {
            this.sendMessage(war.getChannel(), war.getName() + ": "
                                               + war.remaining + ( ( war.remaining == 1 ) ? " second" : " seconds" )
                                               + " remaining!");
        } else
        {
            int remaining = (int) war.remaining / 60;
            this.sendMessage(war.getChannel(), war.getName() + ": " + remaining
                                               + ( ( remaining == 1 ) ? " minute" : " minutes" )
                                               + " remaining.");
        }
    }

    private void beginWar(WordWar war)
    {
        this.sendNotice(war.getChannel(), "WordWar: '" + war.getName() + " 'starts now! (" + war.getDuration() / 60 + " minutes)");
    }

    private void endWar(WordWar war)
    {
        this.sendNotice(war.getChannel(), "WordWar: '" + war.getName() + "' is over!");
        this.wars.remove(war.getName().toLowerCase());
    }

    private void useBackupNick()
    {
        this.setName("ThereAreSomeWhoCallMeTim");
    }

    private void connectToServer()
    {
        try
        {
            this.connect(Tim.config.getString("server"));
        } catch (Exception e)
        {
            this.useBackupNick();
            try
            {
                this.connect(Tim.config.getString("server"));
            } catch (Exception ex)
            {
                System.err.print("Could not connect - name & backup in use");
                System.exit(1);
            }
        }

        Object channels = Tim.config.getProperty("channels.channel");
        if (channels instanceof Collection)
        {
            int size = ( (Collection) channels ).size();
            for (int i = 0; i < size; i++)
            {
                this.joinChannel((String) Tim.config.getProperty("channels.channel(" + i + ")"));
            }
        } else if (channels instanceof String)
        {
            this.joinChannel((String) Tim.config.getProperty("channels.channel"));
        }
    }

    public static void main(String[] args)
    {
        Tim bot = new Tim();

        // Redirect output to SOMETHING I CAN DEBUG!!
		/*try {
        System.setOut(new java.io.PrintStream("Timmy.log.txt"));
        } catch (IOException e) {
        }*/

        bot.setLogin("ThereAreSomeWhoCallMeTim_Bot");
        bot.setVerbose(true);
        bot.setMessageDelay(350);
        bot.connectToServer();
    }
}
