package dev.sleepy_evelyn.packwizml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.sleepy_evelyn.packwizml.config.ConfigHandler;

import java.io.File;

public final class Packwizml {

    public static final String MOD_NAME = "Packwiz Modpack Loader";
    public static final String MOD_ID_LONG = "packwiz-modpack-loader";
    public static final String MOD_ID = "packwizml";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final File GAME_DIR_FILE = PackwizmlPlatform.getGameDirectory();

    public static final ConfigHandler CONFIG_HANDLER = new ConfigHandler();
    public static final PackwizPackManager PACKWIZ_MANAGER = new PackwizPackManager();

    public static void init() {
        String packToml = CONFIG_HANDLER.getValue("pack_toml");

        if(packToml == null || packToml.isEmpty())
            LOGGER.info(MOD_NAME + " failed to load a pack.toml file");
    }
}
