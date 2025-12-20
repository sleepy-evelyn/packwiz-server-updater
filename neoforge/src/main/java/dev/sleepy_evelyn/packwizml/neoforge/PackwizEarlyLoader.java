package dev.sleepy_evelyn.packwizml.neoforge;

import com.moandjiezana.toml.Toml;
import dev.sleepy_evelyn.packwizml.neoforge.exceptions.FailedHashMatchException;
import dev.sleepy_evelyn.packwizml.neoforge.exceptions.PackTomlUrlException;
import dev.sleepy_evelyn.packwizml.neoforge.exceptions.ProcessExitCodeException;
import dev.sleepy_evelyn.packwizml.neoforge.util.HashedFileDownloader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PackwizEarlyLoader {

    public static final String BOOTSTRAP_URL = "https://github.com/packwiz/packwiz-installer-bootstrap/releases/download/v0.0.3/packwiz-installer-bootstrap.jar";
    public static final String BOOTSTRAP_HASH = "a8fbb24dc604278e97f4688e82d3d91a318b98efc08d5dbfcbcbcab6443d116c";

    private static final List<String> PACKWIZ_COMMAND_PREFIX = List.of("java", "-jar", "packwiz-installer-bootstrap.jar");
    private static final Set<String> PACK_TOML_REQUIRED_KEYS = Set.of("name", "version", "index");

    public static void update(String packTomlLink) {
        List<String> command = new ArrayList<>(PACKWIZ_COMMAND_PREFIX);
        boolean isDedicatedServer = false;

        if (isDedicatedServer)
            command.addAll(List.of("-g", "-s", "server"));
        command.add(packTomlLink);

        try {
            if (!hasBootstrap()) {
                var bootstrapPath = Path.of("./packwiz-installer-bootstrap.jar");
                var downloader = new HashedFileDownloader(BOOTSTRAP_URL, BOOTSTRAP_HASH, bootstrapPath);

                downloader.download();
                if (!downloader.hashesMatch()) {
                    var bootstrapFile = bootstrapPath.toFile();

                    if (bootstrapFile.exists()) {
                        if (!bootstrapFile.delete()) {
                            throw new IOException("Cannot verify the integrity of downloaded file 'packwiz-installer-bootstrap.jar'" +
                                    "Please delete this file manually from your main server directory and replace with the correct file" +
                                    "from https://github.com/packwiz/packwiz-installer-bootstrap/releases.");
                        }
                    }
                    throw new FailedHashMatchException();
                }
            }
            testPackTomlLink(packTomlLink);

            var process = new ProcessBuilder(command).inheritIO().start();
            try (var bufferedReader = process.inputReader()) {
                bufferedReader.lines().forEach(PackwizAutoUpdateService.EARLY_LOGGER::info);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0)
                throw new ProcessExitCodeException("Process failed with exit code: " + exitCode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull URL testPackTomlLink(@NotNull final String packTomllink) throws PackTomlUrlException {
        try {
            var url = URI.create(packTomllink).toURL();
            var connection = url.openConnection();
            var toml = new Toml().read(connection.getInputStream());

            if (!PACK_TOML_REQUIRED_KEYS.stream().allMatch(toml::contains)) {
                String requiredKeys = String.join(", ", PACK_TOML_REQUIRED_KEYS);
                throw new PackTomlUrlException("The file does not contain all the required keys: " + requiredKeys);
            }
            return url;
        } catch (MalformedURLException mue) {
            throw new PackTomlUrlException("The link submitted is not a valid URL");
        } catch (IOException ioe) {
            throw new PackTomlUrlException("Check this file exists and is a valid TOML file");
        } catch (IllegalStateException ise) {
            throw new PackTomlUrlException("The file contains invalid data");
        }
    }

    public static boolean hasBootstrap() {
        return new File("packwiz-installer-bootstrap.jar").exists();
    }
}
