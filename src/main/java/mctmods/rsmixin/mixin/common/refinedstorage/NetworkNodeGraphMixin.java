package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.ICraftingRebuildAccessor;
import mctmods.rsmixin.core.accessor.IGraphBatchAccessor;
import mctmods.rsmixin.core.accessor.IStorageCacheDebounceAccessor;
import mctmods.rsmixin.helper.refinedstorage.GraphRescanScheduler;

import com.google.common.collect.Sets;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.INetworkNodeGraphEntry;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashSet;
import java.util.Set;

@Mixin(value = NetworkNodeGraph.class, remap = false) public class NetworkNodeGraphMixin implements IGraphBatchAccessor {
    @Final @Shadow private INetwork network;
    @Unique private int rsmixin$batchDepth = 0;
    @Unique private long rsmixin$lastRescanGameTime = Long.MIN_VALUE;

    @Override public boolean rsmixin$isBatching() { return rsmixin$batchDepth > 0; }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newConcurrentHashSet()Ljava/util/Set;")) private Set<INetworkNodeGraphEntry> optimizeEntries() {
        if (!Config.ENABLE_HASHSET_OPTIMIZE.get()) { return Sets.newConcurrentHashSet(); }
        return new HashSet<>();
    }

    @Inject(method = "invalidate", at = @At("HEAD"), cancellable = true) private void rsmixin$coalesce(Action action, Level level, BlockPos origin, CallbackInfo ci) {
        if (!Config.ENABLE_GRAPH_RESCAN_COALESCE.get()) { return; }
        if (action != Action.PERFORM || !(level instanceof ServerLevel)) { return; }

        long gameTime = level.getGameTime();
        if (GraphRescanScheduler.consumeBypass()) {
            rsmixin$lastRescanGameTime = gameTime;
            return;
        }
        if (rsmixin$lastRescanGameTime == gameTime) {
            GraphRescanScheduler.schedule(network);
            ci.cancel();
            return;
        }
        rsmixin$lastRescanGameTime = gameTime;
    }

    @Inject(method = "invalidate", at = @At("HEAD")) private void rsmixin$beginBatch(Action action, Level level, BlockPos origin, CallbackInfo ci) {
        rsmixin$batchDepth++;
        if (rsmixin$batchDepth == 1 && Config.ENABLE_STORAGE_CACHE_DEBOUNCE.get()) {
            if (network.getItemStorageCache() instanceof IStorageCacheDebounceAccessor accessor) { accessor.rsmixin$resetInvalidated(); }
            if (network.getFluidStorageCache() instanceof IStorageCacheDebounceAccessor accessor) { accessor.rsmixin$resetInvalidated(); }
        }
    }

    @Inject(method = "invalidate", at = @At("RETURN")) private void rsmixin$endBatch(Action action, Level level, BlockPos origin, CallbackInfo ci) {
        if (rsmixin$batchDepth > 0) { rsmixin$batchDepth--; }
        if (rsmixin$batchDepth == 0
                && Config.ENABLE_CRAFTING_REBUILD_DEBOUNCE.get()
                && network.getCraftingManager() instanceof ICraftingRebuildAccessor accessor
                && accessor.rsmixin$consumeRebuildQueued()) {
            network.getCraftingManager().invalidate();
        }
    }
}
