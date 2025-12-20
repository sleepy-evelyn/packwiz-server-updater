package dev.sleepy_evelyn.packwizml.config;

import dev.sleepy_evelyn.packwizml.Packwizml;

import java.util.Properties;

public class ConfigHandler {
    private final ConfigFile configFile;

    public ConfigHandler() {
        var defaultProperties = new Properties();
        defaultProperties.setProperty("pack_toml", "");
        defaultProperties.setProperty("minimum_permission_level", "4");
        defaultProperties.setProperty("auto_update", "false");
        defaultProperties.setProperty("dedicated_server", "true");

        configFile = new ConfigFile(Packwizml.MOD_ID_LONG, defaultProperties, Packwizml.MOD_NAME);
    }

    public void setValue(String key, String value) {
        configFile.setPropertyValue(key, value);
    }

    public String getValue(String key) {
        return configFile.getPropertyValue(key);
    }

    public void update() throws Exception {
        configFile.save();
        configFile.load();
    }

    public void resetToDefaults() {
        configFile.setToDefaults();
    }
}
