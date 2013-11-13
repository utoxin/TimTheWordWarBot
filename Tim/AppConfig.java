/**
 * This file is part of Timmy, the Wordwar Bot.
 *
 * Timmy is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Timmy is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Timmy. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package Tim;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

public class AppConfig extends XMLConfiguration {

	// Need this to make warnings shut up more.
	private static final long serialVersionUID = -4534296896412602302L;
	private static final AppConfig instance;
	private static final String configFile = "BotConfig.xml";

	// Singleton initialiser
	static {
		instance = new AppConfig(configFile);
	}

	/**
	 * Constructor
	 *
	 * @param fileName Configuration file name.
	 */
	private AppConfig(String fileName) {
		init(fileName);
	}

	/**
	 * Initialize the class.
	 *
	 * @param fileName Configuration file name.
	 */
	private void init(String fileName) {
		try {
			setFileName(fileName);
			load();
		} catch (ConfigurationException ex) {
			Logger.getLogger(AppConfig.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Singleton access method.
	 *
	 * @return Singleton
	 */
	public static AppConfig getInstance() {
		return instance;
	}

	public void main(String args[]) {
		AppConfig config = AppConfig.getInstance();
		System.out.println(config.getString("database.user-name"));
		System.out.println(config.getString("database.password"));

		Object obj = config.getProperty("lists.list");
		if (obj instanceof Collection) {
			int size = ((Collection) obj).size();
			for (int i = 0; i < size; i++) {
				System.out.println(config.getProperty("lists.list(" + i + ")"));
			}
		} else {
			if (obj instanceof String) {
				System.out.println(config.getProperty("lists.list"));
			}
		}

		obj = config.getProperty("batch-job.job.name");
		if (obj instanceof Collection) {
			int size = ((Collection) obj).size();
			for (int i = 0; i < size; i++) {
				System.out.println(config.getProperty("batch-job.job(" + i + ").name"));
			}
		} else {
			if (obj instanceof String) {
				System.out.println(config.getProperty("batch-job.job.name"));
			}
		}
	}
}
