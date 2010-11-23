/**
 *  This file is part of Timmy, the Wordwar Bot.
 *
 *  Timmy is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Timmy is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Timmy.  If not, see <http://www.gnu.org/licenses/>.
 */
package Tim;

public class Wrimo
{

    private String irc_nick;
    private int nano_id;
    private String nano_nick;
    private long word_count;

    public String getIrc_nick()
    {
        return irc_nick;
    }

    public int getNano_id()
    {
        return nano_id;
    }

    public String getNano_nick()
    {
        return nano_nick;
    }

    public long getWord_count()
    {
        return word_count;
    }

    public void setIrc_nick(String irc_nick)
    {
        this.irc_nick = irc_nick;
    }

    public void setNano_id(int nano_id)
    {
        this.nano_id = nano_id;
    }

    public void setNano_nick(String nano_nick)
    {
        this.nano_nick = nano_nick;
    }

    public void setWord_count(long word_count)
    {
        this.word_count = word_count;
    }

    @Override
    public String toString()
    {
        return "Wrimo info: " + this.irc_nick + " is " + this.nano_nick
                + " on the NaNoWriMo website, #" + this.nano_id
                + " and currently has " + this.word_count + " words written.";
    }
}
