package sleepy_evelyn.packwizsu.config;

import java.util.Properties;

public class ConfigHandler {
    private static final String HEADER = "Packwiz serverside updater";
    private final ConfigFile configFile;

    public ConfigHandler() {
        var defaultProperties = new Properties();
        defaultProperties.setProperty("pack_toml", "");
        defaultProperties.setProperty("minimum_permission_level", "4");
        configFile = new ConfigFile("packwiz-server-updater", defaultProperties, HEADER);
    }

    public synchronized void setValue(String key, String value) {
        configFile.setPropertyValue(key, value);
    }

    public synchronized String getValue(String key) {
        return configFile.getPropertyValue(key);
    }

    public synchronized void update() throws Exception {
        configFile.save();
        configFile.load();
    }

    public void resetToDefaults() {
        configFile.setToDefaults();
    }
}
