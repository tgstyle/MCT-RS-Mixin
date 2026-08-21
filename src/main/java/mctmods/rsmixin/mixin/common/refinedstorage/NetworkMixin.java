package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.IEnergyDirtyAccessor;

import com.refinedmods.refinedstorage.api.network.INetworkNodeGraph;
import com.refinedmods.refinedstorage.api.network.NetworkType;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.apiimpl.network.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(value = Network.class, remap = false) public abstract class NetworkMixin implements IEnergyDirtyAccessor {
    @Final @Shadow private BlockPos pos;
    @Final @Shadow private Level level;
    @Shadow private int ticksSinceUpdateChanged;
    @Shadow private int ticksSinceEnergyTypeChanged;
    @Unique private boolean rsmixin$dirtyEnergyUsage = true;
    @Unique private boolean rsmixin$wasLoaded = false;
    @Unique private long rsmixin$rescanAtGameTime = -1L;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Shadow public abstract INetworkNodeGraph getNodeGraph();

    @Override public void rsmixin$markEnergyDirty() { rsmixin$dirtyEnergyUsage = true; }

    @Inject(method = "update", at = @At("TAIL")) private void rsmixin$scaleThrottledDebounce(CallbackInfo ci) {
        if (!Config.ENABLE_THROTTLE.get()) { return; }
        int interval = Config.THROTTLE_INTERVAL.get();
        if (interval <= 1) { return; }
        if (ticksSinceUpdateChanged > 0) { ticksSinceUpdateChanged += interval - 1; }
        if (ticksSinceEnergyTypeChanged > 0) { ticksSinceEnergyTypeChanged += interval - 1; }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true) private void updateWithLoadRescan(CallbackInfo ci) {
        if (Config.ENABLE_SKIP_UNLOADED.get()) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            if (!level.hasChunk(chunkX, chunkZ)) {
                if (Config.ENABLE_DEBUG_LOGGING.get()) { rsmixin$LOGGER.debug("RSMixin: Skipping network update for pos {} during chunk unload (chunk not present)", pos); }
                ci.cancel();
                return;
            }
        }

        if (Config.ENABLE_LOAD_RESCAN.get()) {
            boolean currentlyLoaded = level.isLoaded(pos);
            long gameTime = level.getGameTime();
            if (currentlyLoaded && !rsmixin$wasLoaded) { rsmixin$rescanAtGameTime = gameTime + Config.LOAD_RESCAN_DELAY.get(); }
            if (rsmixin$rescanAtGameTime >= 0L && currentlyLoaded && gameTime >= rsmixin$rescanAtGameTime) {
                rsmixin$rescanAtGameTime = -1L;
                getNodeGraph().invalidate(Action.PERFORM, level, pos);
                if (Config.ENABLE_DEBUG_LOGGING.get()) { rsmixin$LOGGER.debug("RSMixin: Forced network graph rescan on load for controller at {}", pos); }
            }
            rsmixin$wasLoaded = currentlyLoaded;
        }
    }

    @Inject(method = "updateEnergyUsage", at = @At("HEAD"), cancellable = true) private void lazyEnergyPre(CallbackInfo ci) {
        if (Config.ENABLE_LAZY_ENERGY.get() && !rsmixin$dirtyEnergyUsage) { ci.cancel(); }
    }

    @Inject(method = "updateEnergyUsage", at = @At("RETURN")) private void lazyEnergyPost(CallbackInfo ci) {
        if (Config.ENABLE_LAZY_ENERGY.get()) { rsmixin$dirtyEnergyUsage = false; }
    }

    @Inject(method = "setRedstonePowered", at = @At("HEAD")) private void dirtyOnRedstonePower(CallbackInfo ci) { rsmixin$dirtyEnergyUsage = true; }

    @Inject(method = "setRedstoneMode", at = @At("HEAD")) private void dirtyOnRedstoneMode(CallbackInfo ci) { rsmixin$dirtyEnergyUsage = true; }

    @Inject(method = "<init>", at = @At("RETURN")) private void initFlags(Level level, BlockPos pos, NetworkType type, CallbackInfo ci) {
        rsmixin$dirtyEnergyUsage = true;
        rsmixin$wasLoaded = false;
        rsmixin$rescanAtGameTime = -1L;
        getNodeGraph().addListener(() -> rsmixin$dirtyEnergyUsage = true);
    }
}
