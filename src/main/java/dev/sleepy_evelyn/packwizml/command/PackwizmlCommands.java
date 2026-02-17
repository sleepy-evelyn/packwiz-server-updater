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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static dev.sleepy_evelyn.packwizml.PackwizPackManager.*;
import static dev.sleepy_evelyn.packwizml.Packwizml.*;
import static net.minecraft.commands.Commands.*;

public final class PackwizmlCommands {

    private static final Component UPDATE_START = Component.literal("Updating modpack. This may take a while...").withStyle(ChatFormatting.GRAY);
    private static final Component UPDATE_START_NO_BOOTSTRAP = Component.literal("Downloading the Packwiz Bootstrap and updating the modpack. This may take a while...").withStyle(ChatFormatting.GRAY);
    private static final Component UPDATE_FINISHED = Component.literal("Packwiz has finished updating. Restart for changes to take effect.").withStyle(ChatFormatting.GREEN);
    private static final Component BOOTSTRAP_DOWNLOAD_FINISHED = Component.literal("Bootstrap downloaded successfully.");
    private static final Component UPDATED_TOML_LINK = Component.literal("Successfully linked a Packwiz modpack. Use /packwiz update for the changes to take effect.").withStyle(ChatFormatting.GREEN);
    private static final Component COMMAND_FAILED = Component.literal("Command failed. Check the console for errors.").withStyle(ChatFormatting.RED);
    private static final Component PROCESS_INTERRUPTED = Component.literal("Process was interrupted. Check the console for details.").withStyle(ChatFormatting.RED);
    private static final Component FILE_HANDLING_ERROR = Component.literal("Read/write process failed. Check the console for details.").withStyle(ChatFormatting.RED);
    private static final Component SET_MIN_PERMISSION_LEVEL = Component.literal("Set minimum permission level required to use the /packwiz command").withStyle(ChatFormatting.GREEN);
    private static final Component AUTO_UPDATE_ON = Component.literal("Enabled automatic modpack updates").withStyle(ChatFormatting.GREEN);
    private static final Component AUTO_UPDATE_OFF = Component.literal("Disabled automatic modpack updates").withStyle(ChatFormatting.RED);

    private static int _minPermissionLevel = 4;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
                .then(literal("autoUpdate")
                        .then(argument("autoUpdate", BoolArgumentType.bool())
                                .executes(PackwizmlCommands::toggleAutoUpdate)
                        )
                )
                .then(literal("minimumPermissionLevel")
                        .then(argument("minimumPermissionLevel", IntegerArgumentType.integer(0, 4))
                                .executes(PackwizmlCommands::setMinPermissionLevel)
                        )
                )
                .requires(
                        source -> source.hasPermission(_minPermissionLevel)
                )
        );
    }

    private static int setTomlLink(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        try {
            var url = testPackTomlLink(StringArgumentType.getString(ctx, "url"));
            setAndUpdateConfigValue("pack_toml", url.toExternalForm());
            getCommandSource(ctx).sendSystemMessage(UPDATED_TOML_LINK);
            return Command.SINGLE_SUCCESS;
        } catch (PackTomlUrlException ptue) {
            throw new SimpleCommandExceptionType(Component.literal(ptue.getMessage())).create();
        }
    }

    private static int restartAndUpdate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
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
            getCommandSource(ctx).sendSystemMessage(UPDATE_START);
        else
            getCommandSource(ctx).sendSystemMessage(UPDATE_START_NO_BOOTSTRAP);

        PACKWIZ_MANAGER.update(packTomlLink, hasBootstrap, ctx);
        return Command.SINGLE_SUCCESS;
    }

    private static int toggleAutoUpdate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        boolean autoUpdate = BoolArgumentType.getBool(ctx, "autoUpdate");
        setAndUpdateConfigValue("auto_update", String.valueOf(autoUpdate));
        if (autoUpdate)
            getCommandSource(ctx).sendSystemMessage(AUTO_UPDATE_ON);
        else
            getCommandSource(ctx).sendSystemMessage(AUTO_UPDATE_OFF);
        return Command.SINGLE_SUCCESS;
    }

    private static int setMinPermissionLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int minPermissionLevel = IntegerArgumentType.getInteger(ctx, "minimumPermissionLevel");
        setAndUpdateConfigValue("minimum_permission_level", String.valueOf(minPermissionLevel));
        getCommandSource(ctx).sendSystemMessage(SET_MIN_PERMISSION_LEVEL);
        return Command.SINGLE_SUCCESS;
    }

    public static void pollCommandStatus() {
        var tasksIterator = TASKS.listIterator();
        Exception exception = null;
        Component message = null;

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
                            message = Component.literal(ptfe.getMessage());
                        else if (cause instanceof ProcessExitCodeException pece)
                            message = Component.literal(pece.getMessage());
                        else if (cause instanceof FailedHashMatchException fhme)
                            message = Component.literal(fhme.getMessage());
                    }
                    if (message == null) message = COMMAND_FAILED;
                }
                task.sendSystemMessage(message);
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

    private static CommandSource getCommandSource(CommandContext<CommandSourceStack> ctx) {
        return (ctx.getSource().getEntity() instanceof ServerPlayer player)
                ? player : ctx.getSource().getServer();
    }

    public static class AsyncCommandTask {
        private final String name;
        private final CompletableFuture<Void> future;
        private final CommandSource co;
        private final TickCounter tc;

        public AsyncCommandTask(CompletableFuture<Void> future, String name, int pollTicks, CommandContext<CommandSourceStack> ctx) {
            this.future = future;
            this.name = name;
            this.co = getCommandSource(ctx);
            this.tc = new TickCounter(pollTicks);
        }

        public void tick() { tc.increment(); }

        public boolean pollFinished() {
            return (tc.test() && future.isDone());
        }

        public void sendSystemMessage(Component message) {
            if (future.isDone())
                co.sendSystemMessage(message);
        }

        public CompletableFuture<Void> getFuture() { return future; }
        public boolean hasName(String name) { return this.name.equals(name); }
    }
}
