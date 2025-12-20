package dev.sleepy_evelyn.packwizml;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.sleepy_evelyn.packwizml.config.ConfigHandler;

import java.io.File;

public class Packwizml implements ModInitializer {

    public static final String MOD_NAME = "Packwiz Modpack Loader";
    public static final String MOD_ID_LONG = "packwiz-modpack-loader";
    public static final String MOD_ID = "packwizml";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final File GAME_DIR_FILE = FabricLoader.getInstance().getGameDir().toFile();

    public static final ConfigHandler CONFIG_HANDLER = new ConfigHandler();
    public static final PackwizPackManager PACKWIZ_MANAGER = new PackwizPackManager();

    @Override
    public void onInitialize() {
        String packToml = CONFIG_HANDLER.getValue("pack_toml");

        if(packToml == null || packToml.isEmpty())
            LOGGER.info(MOD_NAME + " failed to load a pack.toml file");
    }
}
