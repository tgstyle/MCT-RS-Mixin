package mctmods.rsmixin.mixin;

import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Unique;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(targets = "com.refinedmods.refinedstorage.apiimpl.network.NetworkListener")
public class NetworkListenerMixin {
    @Unique
    private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Inject(method = "onLevelTick(Lnet/minecraftforge/event/TickEvent$LevelTickEvent;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void throttle(TickEvent.LevelTickEvent event, CallbackInfo ci) {
        if (!mctmods.rsmixin.Config.ENABLE_THROTTLE.get()) return;

        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;

        int interval = mctmods.rsmixin.Config.THROTTLE_INTERVAL.get();
        if (interval > 1 && event.level.getGameTime() % interval != 0) {
            ci.cancel();
            return;
        }
        if (mctmods.rsmixin.Config.ENABLE_DEBUG_LOGGING.get()) {
            rsmixin$LOGGER.debug("RS Throttle: Full update tick");
        }
    }
}
