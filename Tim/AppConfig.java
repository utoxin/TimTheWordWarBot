package Tim;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import java.util.Collection;

public class AppConfig extends XMLConfiguration {

    private static AppConfig instance;
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
        setFileName(fileName);
        try {
            load();
        } catch (ConfigurationException configEx) {
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

    public static void main(String args[]) {
        AppConfig config = AppConfig.getInstance();
        System.out.println(config.getString("database.user-name"));
        System.out.println(config.getString("database.password"));

        Object obj = config.getProperty("lists.list");
        if (obj instanceof Collection) {
            int size = ( (Collection) obj ).size();
            for (int i = 0; i < size; i++) {
                System.out.println(config.getProperty("lists.list(" + i + ")"));
            }
        } else if (obj instanceof String) {
            System.out.println(config.getProperty("lists.list"));
        }

        obj = config.getProperty("batch-job.job.name");
        if (obj instanceof Collection) {
            int size = ( (Collection) obj ).size();
            for (int i = 0; i < size; i++) {
                System.out.println(config.getProperty("batch-job.job(" + i + ").name"));
            }
        } else if (obj instanceof String) {
            System.out.println(config.getProperty("batch-job.job.name"));
        }
    }
}
