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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL
{

    private Connection conn;

    public MySQL(String server, String db, String user, String password)
    {
        try
        {
            conn = DriverManager.getConnection("jdbc:mysql://" + server + "/" + db, user, password);
        } catch (SQLException e)
        {
            conn = null;
        }
    }

    public synchronized void dispose()
    {
        this.Close();
    }

    public void Close()
    {
        if (conn != null)
        {
            try
            {
                conn.close();
            } catch (SQLException e)
            {
                // ... Who cares, we're closing.
                conn = null;
            }
        }
    }
}
