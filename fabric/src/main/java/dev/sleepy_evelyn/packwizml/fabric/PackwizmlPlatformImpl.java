package dev.sleepy_evelyn.packwizml.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

public class PackwizmlPlatformImpl {

    public static boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    public static File getGameDirectory() {
        return FabricLoader.getInstance().getGameDir().toFile();
    }

    public static boolean isNeoForge() {
        return false;
    }
}
