package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.ICraftingRebuildAccessor;
import mctmods.rsmixin.core.accessor.IGraphBatchAccessor;
import mctmods.rsmixin.core.accessor.IStorageCacheDebounceAccessor;
import mctmods.rsmixin.helper.refinedstorage.GraphRescanScheduler;

import com.google.common.collect.Sets;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.apiimpl.network.NetworkNodeGraph;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    @Shadow private INetwork network;
    @Unique private int rsmixin$batchDepth = 0;
    @Unique private long rsmixin$lastRescanGameTime = Long.MIN_VALUE;

    @Override public boolean rsmixin$isBatching() { return rsmixin$batchDepth > 0; }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newConcurrentHashSet()Ljava/util/Set;")) private Set<INetworkNode> optimizeEntries() {
        if (!Config.enableHashSetOptimize) { return Sets.newConcurrentHashSet(); }
        return new HashSet<>();
    }

    @Inject(method = "invalidate", at = @At("HEAD"), cancellable = true) private void rsmixin$coalesce(Action action, World world, BlockPos origin, CallbackInfo ci) {
        if (!Config.enableGraphRescanCoalesce) { return; }
        if (action != Action.PERFORM || world.isRemote) { return; }

        long gameTime = world.getTotalWorldTime();
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

    @Inject(method = "invalidate", at = @At("HEAD")) private void rsmixin$beginBatch(Action action, World world, BlockPos origin, CallbackInfo ci) {
        rsmixin$batchDepth++;
        if (rsmixin$batchDepth == 1 && Config.enableStorageCacheDebounce) {
            if (network.getItemStorageCache() instanceof IStorageCacheDebounceAccessor) { ((IStorageCacheDebounceAccessor) network.getItemStorageCache()).rsmixin$resetInvalidated(); }
            if (network.getFluidStorageCache() instanceof IStorageCacheDebounceAccessor) { ((IStorageCacheDebounceAccessor) network.getFluidStorageCache()).rsmixin$resetInvalidated(); }
        }
    }

    @Inject(method = "invalidate", at = @At("RETURN")) private void rsmixin$endBatch(Action action, World world, BlockPos origin, CallbackInfo ci) {
        if (rsmixin$batchDepth > 0) { rsmixin$batchDepth--; }
        if (rsmixin$batchDepth == 0
                && Config.enableCraftingRebuildDebounce
                && network.getCraftingManager() instanceof ICraftingRebuildAccessor
                && ((ICraftingRebuildAccessor) network.getCraftingManager()).rsmixin$consumeRebuildQueued()) {
            network.getCraftingManager().rebuild();
        }
    }
}
