package dev.sleepy_evelyn.packwizml.neoforge;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;

public class PackwizmlPlatformImpl {

    public static boolean isDedicatedServer() {
        return FMLEnvironment.dist.isDedicatedServer();
    }

    public static File getGameDirectory() {
        return FMLPaths.GAMEDIR.get().toFile();
    }

    public static boolean isNeoForge() {
        return true;
    }
}
