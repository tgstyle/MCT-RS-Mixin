package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.IGraphBatchAccessor;
import mctmods.rsmixin.core.accessor.IStorageCacheDebounceAccessor;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.apiimpl.storage.cache.ItemStorageCache;
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

@Mixin(value = ItemStorageCache.class, remap = false) public abstract class ItemStorageCacheMixin implements IStorageCacheDebounceAccessor {
    @Final @Shadow private INetwork network;
    @Unique private boolean rsmixin$invalidatedDuringBatch = false;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Override public void rsmixin$resetInvalidated() { rsmixin$invalidatedDuringBatch = false; }

    @Inject(method = "invalidate", at = @At("HEAD"), cancellable = true) private void rsmixin$debouncePre(CallbackInfo ci) {
        if (!Config.ENABLE_STORAGE_CACHE_DEBOUNCE.get()) { return; }
        if (!(network.getNodeGraph() instanceof IGraphBatchAccessor graph) || !graph.rsmixin$isBatching()) { return; }
        if (rsmixin$invalidatedDuringBatch) {
            if (Config.ENABLE_DEBUG_LOGGING.get()) { rsmixin$LOGGER.debug("RSMixin: Skipped redundant item storage cache rebuild for network at {}", network.getPosition()); }
            ci.cancel();
        }
    }

    @Inject(method = "invalidate", at = @At("RETURN")) private void rsmixin$debouncePost(CallbackInfo ci) {
        if (!Config.ENABLE_STORAGE_CACHE_DEBOUNCE.get()) { return; }
        if (network.getNodeGraph() instanceof IGraphBatchAccessor graph && graph.rsmixin$isBatching()) { rsmixin$invalidatedDuringBatch = true; }
    }
}
