package sleepy_evelyn.packwizsu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sleepy_evelyn.packwizsu.config.ConfigHandler;

import java.io.File;

public class Packwizsu implements ModInitializer {

    private static final String MOD_ID = "packwizsu";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final File GAME_DIR_FILE = FabricLoader.getInstance().getGameDir().toFile();

    private static ConfigHandler configHandler;

    @Override
    public void onInitialize() {
        configHandler = new ConfigHandler();
        String packToml = configHandler.getValue("pack_toml");

        if(packToml == null || packToml.isEmpty())
            LOGGER.info("Packwiz Server Updater loaded without a pack.toml file to update from");
        else
            LOGGER.info("Packwiz Server Updater loaded with pack.toml link: {}", configHandler.getValue("pack_toml"));
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static ConfigHandler getConfigHandler() {
        return configHandler;
    }
}
