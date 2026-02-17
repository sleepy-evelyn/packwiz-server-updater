package dev.sleepy_evelyn.packwizml.config;

import dev.sleepy_evelyn.packwizml.Packwizml;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;

import static dev.sleepy_evelyn.packwizml.Packwizml.GAME_DIR_FILE;

public class ConfigFile {

    private final File file;
    private final Properties defaults;
    private final String headerComments;

    private Properties properties;

    ConfigFile(String title, Properties defaults, String headerComments) {
        this.defaults = defaults;
        this.headerComments = headerComments;

        this.file = new File(GAME_DIR_FILE + "/" + title + ".properties");
        try {
            if(Files.notExists(GAME_DIR_FILE.toPath())) { // Create the main config directory if it doesn't exist
                Files.createDirectory(GAME_DIR_FILE.toPath());
            }
            if(file.createNewFile()) { // Create file & set to defaults if it doesn't exist
                setToDefaults();
                save();
            }
            load();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize config file " + title + ".properties", e);
        }
    }

    void save() throws Exception {
        var outputStream = new FileOutputStream(file);
        properties.store(outputStream, headerComments);
        outputStream.close();
    }

    void load() throws Exception {
        if(file.createNewFile()) { // If no file currently exists
            setToDefaults();
        } else {
            var inputStream = new FileInputStream(file);
            properties = new Properties();
            properties.load(inputStream);
        }
    }

    void setToDefaults()
    {
        properties = defaults;
    }

    void setPropertyValue(String key, String value) { properties.setProperty(key, value); }

    String getPropertyValue(String key) { return properties.getProperty(key); }
}
