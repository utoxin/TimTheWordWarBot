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

import java.util.ArrayList;
import java.io.*;

public class Fridge implements Serializable
{

    private static final long serialVersionUID = 6583411952855142866L;
    private String filename;
    private ArrayList<FridgeItem> items;

    public Fridge(String filename)
    {
        this.filename = filename;
        try
        {
        } catch (Exception e)
        {
            // Something went wrong. Maybe we don't have a file??
            this.items = new ArrayList<FridgeItem>();
            this.items.add(new FridgeItem("coffee", true));
            this.items.add(new FridgeItem("hot chocolate", true));
            this.items.add(new FridgeItem("confetti", true));
        }
    }

    public void addItem(String itemname)
    {
        if (!this.items.contains(itemname))
        {
        }
    }
}
