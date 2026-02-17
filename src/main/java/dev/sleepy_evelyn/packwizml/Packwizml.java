package dev.sleepy_evelyn.packwizml;

import dev.sleepy_evelyn.packwizml.command.PackwizmlCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.sleepy_evelyn.packwizml.config.ConfigHandler;

import java.io.File;

@Mod(Packwizml.MOD_ID)
public final class Packwizml {

    public static final String MOD_NAME = "Packwiz Modpack Loader";
    public static final String MOD_ID_LONG = "packwiz-modpack-loader";
    public static final String MOD_ID = "packwizml";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final File GAME_DIR_FILE = FMLPaths.GAMEDIR.get().toFile();

    public static final ConfigHandler CONFIG_HANDLER = new ConfigHandler();
    public static final PackwizPackManager PACKWIZ_MANAGER = new PackwizPackManager();

    public Packwizml() {
        String packToml = CONFIG_HANDLER.getValue("pack_toml");

        if(packToml == null || packToml.isEmpty())
            LOGGER.info(MOD_NAME + " failed to load a pack.toml file");
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent e) {
        PackwizmlCommands.register(e.getDispatcher());
    }

    @SubscribeEvent
    public static void onStartTick(ServerTickEvent e) {
        PackwizmlCommands.pollCommandStatus();
    }
}
