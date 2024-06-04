package net.fabricmc.example.mixin;

import baritone.api.utils.MinecraftServerUtil;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
/*
    @Inject(method = "runServer", at = @At("HEAD"))
    private void onRun(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        // Store the server instance wherever you need it
        MinecraftServerUtil.setMinecraftServer(server);
    }*/
}

