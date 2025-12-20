package dev.sleepy_evelyn.packwizml.neoforge;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public class PackwizAutoUpdateService implements ITransformationService {

    public static final String TITLE = "packwiz-modpack-loader";
    public static final String NAME = "Packwiz Modpack Loader";
    public static final Logger EARLY_LOGGER = Logger.getLogger(NAME);

    @Override
    public @NotNull String name() {
        return "packwiz_auto_update";
    }

    @Override
    public void initialize(IEnvironment environment) {
        var configFile = new File(PackwizmlPlatformImpl.getGameDirectory() + "/" + TITLE + ".properties");

        try (var configStream = new FileInputStream(configFile)){
            var configProperties = new Properties();
            configProperties.load(configStream);

            if (Boolean.parseBoolean(configProperties.getProperty("auto_update"))) {
                PackwizEarlyLoader.update(configProperties.getProperty("pack_toml"));
            }
        } catch (IOException e) {
            throw new RuntimeException(NAME + " failed to load the " + TITLE + ".properties file required to automatically update a modpack on initial launch", e);
        }
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) {}

    @Override
    public @NotNull List<? extends ITransformer<?>> transformers() {
        return List.of();
    }
}
