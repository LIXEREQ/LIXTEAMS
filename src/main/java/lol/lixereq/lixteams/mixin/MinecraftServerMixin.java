package lol.lixereq.lixteams.mixin;

import lol.lixereq.lixteams.data.datManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "loadLevel", at = @At("TAIL"))
    private void onServerLoad(CallbackInfo ci) {
        try {
            datManager.get().processExpiredPendingCurrencies();
        } catch (Exception e) {
            datManager.LOGGER.error("Failed to process pending currencies on server load: " + e.getMessage());
        }
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onServerTick(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (server.getTickCount() % 600 == 0) {
            try {
                datManager.get().processExpiredPendingCurrencies();
            } catch (Exception e) {
                datManager.LOGGER.error("Failed to process pending currencies on tick: " + e.getMessage());
            }
        }
    }
}