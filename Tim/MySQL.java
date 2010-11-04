package Tim;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {

	private Connection conn;

	public MySQL(String server, String db, String user, String password) {
		try {
			conn = DriverManager.getConnection("jdbc:mysql://" + server + "/" + db, user, password);
		} catch (SQLException e) {
			conn = null;
		}
	}

	public synchronized void dispose() {
		this.Close();
	}

	public void Close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				// ... Who cares, we're closing.
				conn = null;
			}
		}
	}
}
