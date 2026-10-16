package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.IGraphBatchAccessor;
import mctmods.rsmixin.core.accessor.IStorageCacheDebounceAccessor;
import mctmods.rsmixin.helper.refinedstorage.TrackerTouch;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.apiimpl.storage.cache.ItemStorageCache;
import com.refinedmods.refinedstorage.util.StackUtils;
import net.minecraft.world.item.ItemStack;
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

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;IZZ)V", at = @At("HEAD")) private void rsmixin$trackAdd(ItemStack stack, int size, boolean rebuilding, boolean batched, CallbackInfo ci) {
        if (!rebuilding) { rsmixin$touchTracker(stack); }
    }

    @Inject(method = "remove(Lnet/minecraft/world/item/ItemStack;IZ)V", at = @At("HEAD")) private void rsmixin$trackRemove(ItemStack stack, int size, boolean batched, CallbackInfo ci) { rsmixin$touchTracker(stack); }

    @Unique private void rsmixin$touchTracker(ItemStack stack) {
        if (Config.ENABLE_LAST_MODIFIED_TRACKER_FIX.get()) { TrackerTouch.touch(network, network.getItemStorageTracker(), stack, StackUtils::serializeStackToNbt); }
    }
}
