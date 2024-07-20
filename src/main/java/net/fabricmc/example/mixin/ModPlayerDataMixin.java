package net.fabricmc.example.mixin;

import net.fabricmc.example.client.darkness.ModPlayerData;
import net.fabricmc.example.client.darkness.ModPlayerDataImpl;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ModPlayerDataMixin implements ModPlayerData {
    @Unique
    private final ModPlayerData data = new ModPlayerDataImpl();

    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void onCopyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo info) {
        data.setHasMod(((ModPlayerData) oldPlayer).hasMod());
    }

    @Override
    public void setHasMod(boolean hasMod) {
        data.setHasMod(hasMod);
    }

    @Override
    public boolean hasMod() {
        return data.hasMod();
    }
}
