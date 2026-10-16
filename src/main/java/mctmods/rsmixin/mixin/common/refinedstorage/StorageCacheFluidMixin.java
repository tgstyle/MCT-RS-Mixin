package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.interfaces.IFluidTrackerTouch;
import mctmods.rsmixin.core.interfaces.IGraphBatch;
import mctmods.rsmixin.core.interfaces.IStorageCacheDebounce;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.apiimpl.storage.StorageCacheFluid;
import net.minecraftforge.fluids.FluidStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StorageCacheFluid.class, remap = false) public abstract class StorageCacheFluidMixin implements IStorageCacheDebounce {
    @Shadow private INetwork network;
    @Unique private boolean rsmixin$invalidatedDuringBatch = false;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Override public void rsmixin$resetInvalidated() { rsmixin$invalidatedDuringBatch = false; }

    @Inject(method = "invalidate", at = @At("HEAD"), cancellable = true) private void rsmixin$debouncePre(CallbackInfo ci) {
        if (!Config.enableStorageCacheDebounce) { return; }
        if (!(network.getNodeGraph() instanceof IGraphBatch) || !((IGraphBatch) network.getNodeGraph()).rsmixin$isBatching()) { return; }
        if (rsmixin$invalidatedDuringBatch) {
            if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Skipped redundant fluid storage cache rebuild for network at {}", network.getPosition()); }
            ci.cancel();
        }
    }

    @Inject(method = "invalidate", at = @At("RETURN")) private void rsmixin$debouncePost(CallbackInfo ci) {
        if (!Config.enableStorageCacheDebounce) { return; }
        if (network.getNodeGraph() instanceof IGraphBatch && ((IGraphBatch) network.getNodeGraph()).rsmixin$isBatching()) { rsmixin$invalidatedDuringBatch = true; }
    }

    @Inject(method = "add(Lnet/minecraftforge/fluids/FluidStack;IZZ)V", at = @At("HEAD")) private void rsmixin$trackAdd(FluidStack stack, int size, boolean rebuilding, boolean batched, CallbackInfo ci) {
        if (!rebuilding) { rsmixin$touchTracker(stack); }
    }

    @Inject(method = "remove(Lnet/minecraftforge/fluids/FluidStack;IZ)V", at = @At("HEAD")) private void rsmixin$trackRemove(FluidStack stack, int size, boolean batched, CallbackInfo ci) { rsmixin$touchTracker(stack); }

    @Unique private void rsmixin$touchTracker(FluidStack stack) {
        if (Config.enableLastModifiedTrackerFix && network.getFluidStorageTracker() instanceof IFluidTrackerTouch) { ((IFluidTrackerTouch) network.getFluidStorageTracker()).rsmixin$touchFluid(stack); }
    }
}
