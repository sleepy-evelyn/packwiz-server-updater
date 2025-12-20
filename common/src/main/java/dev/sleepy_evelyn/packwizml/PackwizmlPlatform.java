package dev.sleepy_evelyn.packwizml;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.io.File;

public class PackwizmlPlatform {

    @ExpectPlatform
    public static boolean isDedicatedServer() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isNeoForge() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static File getGameDirectory() {
        throw new AssertionError();
    }
}
