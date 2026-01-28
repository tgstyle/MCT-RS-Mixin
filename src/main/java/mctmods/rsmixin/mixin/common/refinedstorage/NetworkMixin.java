package mctmods.rsmixin.mixin.common.refinedstorage;

import com.refinedmods.refinedstorage.api.network.INetworkNodeGraph;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(value = Network.class, remap = false)
public abstract class NetworkMixin {
    @Final @Shadow private BlockPos pos;
    @Final @Shadow private Level level;
    @Unique private boolean rsmixin$dirtyEnergyUsage = true;
    @Unique private boolean rsmixin$wasLoaded = false;
    @Unique private boolean rsmixin$listenerAdded = false;
    @Unique private int rsmixin$reloadDelay = 0;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Shadow public abstract INetworkNodeGraph getNodeGraph();

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void updateWithLoadRescan(CallbackInfo ci) {
        if (mctmods.rsmixin.Config.ENABLE_SKIP_UNLOADED.get()) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            if (!level.hasChunk(chunkX, chunkZ)) {
                if (mctmods.rsmixin.Config.ENABLE_DEBUG_LOGGING.get()) {
                    rsmixin$LOGGER.debug("RSMixin: Skipping network update for pos {} during chunk unload (chunk not present)", pos);
                }
                ci.cancel();
                return;
            }
        }

        if (mctmods.rsmixin.Config.ENABLE_LOAD_RESCAN.get()) {
            boolean currentlyLoaded = level.isLoaded(pos);
            if (currentlyLoaded && !rsmixin$wasLoaded) {
                rsmixin$reloadDelay = mctmods.rsmixin.Config.LOAD_RESCAN_DELAY.get();
            }
            if (rsmixin$reloadDelay > 0 && currentlyLoaded) {
                rsmixin$reloadDelay--;
                if (rsmixin$reloadDelay == 0) {
                    getNodeGraph().invalidate(Action.PERFORM, level, pos);
                    if (mctmods.rsmixin.Config.ENABLE_DEBUG_LOGGING.get()) {
                        rsmixin$LOGGER.debug("RSMixin: Forced network graph rescan on load for controller at {}", pos);
                    }
                }
            }
            rsmixin$wasLoaded = currentlyLoaded;
        }
    }

    @Inject(method = "updateEnergyUsage", at = @At("HEAD"), cancellable = true)
    private void lazyEnergyPre(CallbackInfo ci) {
        if (mctmods.rsmixin.Config.ENABLE_LAZY_ENERGY.get() && !rsmixin$dirtyEnergyUsage) {
            ci.cancel();
        }
    }

    @Inject(method = "updateEnergyUsage", at = @At("RETURN"))
    private void lazyEnergyPost(CallbackInfo ci) {
        if (mctmods.rsmixin.Config.ENABLE_LAZY_ENERGY.get()) {
            rsmixin$dirtyEnergyUsage = false;
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initFlags(Level level, BlockPos pos, com.refinedmods.refinedstorage.api.network.NetworkType type, CallbackInfo ci) {
        if (mctmods.rsmixin.Config.ENABLE_LAZY_ENERGY.get()) {
            rsmixin$dirtyEnergyUsage = true;
        }
        rsmixin$wasLoaded = false;
        rsmixin$listenerAdded = false;
        rsmixin$reloadDelay = 0;
    }

    @Inject(method = "getNodeGraph", at = @At("RETURN"))
    private void addLazyListener(CallbackInfoReturnable<INetworkNodeGraph> cir) {
        if (mctmods.rsmixin.Config.ENABLE_LAZY_ENERGY.get() && !rsmixin$listenerAdded) {
            cir.getReturnValue().addListener(() -> rsmixin$dirtyEnergyUsage = true);
            rsmixin$listenerAdded = true;
        }
    }
}
