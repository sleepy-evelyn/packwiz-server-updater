package dev.sleepy_evelyn.packwizml.fabric;

import dev.sleepy_evelyn.packwizml.Packwizml;
import net.fabricmc.api.ModInitializer;

public final class PackwizmlFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Packwizml.init();
    }
}
