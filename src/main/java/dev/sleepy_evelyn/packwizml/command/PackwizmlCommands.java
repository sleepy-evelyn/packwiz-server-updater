package dev.sleepy_evelyn.packwizml.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.sleepy_evelyn.packwizml.exceptions.FailedHashMatchException;
import dev.sleepy_evelyn.packwizml.exceptions.PackTomlUrlException;
import dev.sleepy_evelyn.packwizml.exceptions.ProcessExitCodeException;
import dev.sleepy_evelyn.packwizml.util.TickCounter;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static dev.sleepy_evelyn.packwizml.PackwizPackManager.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static dev.sleepy_evelyn.packwizml.Packwizml.*;

public final class PackwizmlCommands {

    private static final MutableText UPDATE_START = Text.literal("Updating modpack. This may take a while...").formatted(Formatting.GRAY);
    private static final MutableText UPDATE_START_NO_BOOTSTRAP = Text.literal("Downloading the Packwiz Bootstrap and updating the modpack. This may take a while...").formatted(Formatting.GRAY);
    private static final MutableText UPDATE_FINISHED = Text.literal("Packwiz has finished updating. Restart for changes to take effect.").formatted(Formatting.GREEN);
    private static final MutableText BOOTSTRAP_DOWNLOAD_FINISHED = Text.literal("Bootstrap downloaded successfully.");
    private static final MutableText UPDATED_TOML_LINK = Text.literal("Successfully linked a Packwiz modpack. Use /packwiz update for the changes to take effect.").formatted(Formatting.GREEN);
    private static final MutableText COMMAND_FAILED = Text.literal("Command failed. Check the console for errors.").formatted(Formatting.RED);
    private static final MutableText PROCESS_INTERRUPTED = Text.literal("Process was interrupted. Check the console for details.").formatted(Formatting.RED);
    private static final MutableText FILE_HANDLING_ERROR = Text.literal("Read/write process failed. Check the console for details.").formatted(Formatting.RED);
    private static final MutableText SET_MIN_PERMISSION_LEVEL = Text.literal("Set minimum permission level required to use the /packwiz command").formatted(Formatting.GREEN);
    private static int _minPermissionLevel = 4;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        String minPermissionLevel = CONFIG_HANDLER.getValue("minimum_permission_level");
        if (minPermissionLevel == null)
            LOGGER.warn("Failed to read minimum permission level from config. Defaulting to 4 (Operator).");
        else
            _minPermissionLevel = Integer.parseInt(minPermissionLevel);

        dispatcher.register(literal("packwiz")
                .then(literal("link")
                        .then(argument("url", StringArgumentType.greedyString())
                                .executes(PackwizmlCommands::setTomlLink)
                        )
                )
                .then(literal("update")
                        .executes(PackwizmlCommands::restartAndUpdate)
                )
                .then(literal("minimumPermissionLevel")
                        .then(argument("minimumPermissionLevel", IntegerArgumentType.integer(0, 4))
                                .executes(PackwizmlCommands::setMinPermissionLevel)
                        )
                )
                .requires(
                        source -> source.hasPermissionLevel(_minPermissionLevel)
                )
        );
    }

    private static int setTomlLink(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        try {
            var url = testPackTomlLink(StringArgumentType.getString(ctx, "url"));
            setAndUpdateConfigValue("pack_toml", url.toExternalForm());
            getCommandOutput(ctx).sendMessage(UPDATED_TOML_LINK);
            return Command.SINGLE_SUCCESS;
        } catch (PackTomlUrlException ptue) {
            throw new SimpleCommandExceptionType(Text.literal(ptue.getMessage())).create();
        }
    }

    private static int restartAndUpdate(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        try {
            if(!GAME_DIR_FILE.exists())
                throw CommandExceptions.DIRECTORY_BLANK_ERROR.create();
        } catch (SecurityException se) {
            throw CommandExceptions.DIRECTORY_SECURITY_ERROR.create();
        }

        String packTomlLink = CONFIG_HANDLER.getValue("pack_toml");
        if (!packTomlLink.contains("pack.toml"))
            throw CommandExceptions.NO_PACK_TOML.create();
        if (PACKWIZ_MANAGER.isAsyncTaskRunning(UPDATE_PACKWIZ_TASK_NAME))
            throw CommandExceptions.UPDATE_IN_PROGRESS_ERROR.create();

        boolean hasBootstrap = PACKWIZ_MANAGER.hasBootstrap();
        if (PACKWIZ_MANAGER.hasBootstrap())
            getCommandOutput(ctx).sendMessage(UPDATE_START);
        else
            getCommandOutput(ctx).sendMessage(UPDATE_START_NO_BOOTSTRAP);

        PACKWIZ_MANAGER.update(packTomlLink, hasBootstrap, ctx);
        return Command.SINGLE_SUCCESS;
    }

    private static int setMinPermissionLevel(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        int minPermissionLevel = IntegerArgumentType.getInteger(ctx, "minimumPermissionLevel");
        setAndUpdateConfigValue("minimum_permission_level", String.valueOf(minPermissionLevel));
        getCommandOutput(ctx).sendMessage(SET_MIN_PERMISSION_LEVEL);
        return Command.SINGLE_SUCCESS;
    }

    public static void pollCommandStatus() {
        var tasksIterator = TASKS.listIterator();
        Exception exception = null;
        Text message = null;

        while (tasksIterator.hasNext()) {
            var task = tasksIterator.next();
            task.tick();

            if (task.pollFinished()) {
                try {
                    task.getFuture().join();

                    if (task.hasName(UPDATE_PACKWIZ_TASK_NAME))
                        message = UPDATE_FINISHED;
                    else if (task.hasName(BOOTSTRAP_TASK_NAME))
                        message = BOOTSTRAP_DOWNLOAD_FINISHED;
                } catch (CompletionException e) {
                    var cause = e.getCause();
                    exception = e;

                    if (cause instanceof InterruptedException)
                        message = PROCESS_INTERRUPTED;
                    else if (cause instanceof IOException)
                        message = FILE_HANDLING_ERROR;

                    if (task.hasName(UPDATE_PACKWIZ_TASK_NAME)) {
                        if (cause instanceof PackTomlUrlException ptfe)
                            message = Text.literal(ptfe.getMessage());
                        else if (cause instanceof ProcessExitCodeException pece)
                            message = Text.literal(pece.getMessage());
                        else if (cause instanceof FailedHashMatchException fhme)
                            message = Text.literal(fhme.getMessage());
                    }
                    if (message == null) message = COMMAND_FAILED;
                }
                task.sendMessage(message);
                if (exception != null)
                    LOGGER.error("Unexpected exception occurred whilst polling Packwiz command status", exception);
                tasksIterator.remove();
            }
        }
    }

    private static void setAndUpdateConfigValue(String key, String value) throws CommandSyntaxException {
        try {
            CONFIG_HANDLER.setValue(key, value);
            CONFIG_HANDLER.update();
        } catch (Exception e) {
            throw CommandExceptions.FILE_UPDATE_FAILED.create();
        }
    }

    private static CommandOutput getCommandOutput(CommandContext<ServerCommandSource> ctx) {
        return (ctx.getSource().getEntity() instanceof ServerPlayerEntity player)
                ? player : ctx.getSource().getServer();
    }

    public static class AsyncCommandTask {
        private final String name;
        private final CompletableFuture<Void> future;
        private final CommandOutput co;
        private final TickCounter tc;

        public AsyncCommandTask(CompletableFuture<Void> future, String name, int pollTicks, CommandContext<ServerCommandSource> ctx) {
            this.future = future;
            this.name = name;
            this.co = getCommandOutput(ctx);
            this.tc = new TickCounter(pollTicks);
        }

        public void tick() { tc.increment(); }

        public boolean pollFinished() {
            return (tc.test() && future.isDone());
        }

        public void sendMessage(Text message) {
            if (future.isDone())
                co.sendMessage(message);
        }

        public CompletableFuture<Void> getFuture() { return future; }
        public boolean hasName(String name) { return this.name.equals(name); }
    }
}
