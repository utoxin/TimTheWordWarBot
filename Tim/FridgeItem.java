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

import java.io.Serializable;

public class FridgeItem implements Serializable
{

    private static final long serialVersionUID = -1658316674659907588L;
    private String name;
    private boolean approved;

    public FridgeItem(String str, boolean flag)
    {
        this.name = str.toLowerCase();
        this.approved = flag;
    }

    public boolean equals(FridgeItem item)
    {
        return this.name.equalsIgnoreCase(item.name);
    }

    public boolean equals(String itemname)
    {
        return this.name.equalsIgnoreCase(itemname);
    }

    public boolean isApproved()
    {
        return this.approved;
    }
}
