package dev.sleepy_evelyn.packwizml.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sleepy_evelyn.packwizml.command.PackwizmlCommands;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V"), method = "tick")
    private void onStartTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        PackwizmlCommands.pollCommandStatus();
    }
}
