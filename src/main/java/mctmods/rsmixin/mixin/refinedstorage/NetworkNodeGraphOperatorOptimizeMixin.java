package mctmods.rsmixin.mixin.refinedstorage;

import com.google.common.collect.Sets;
import com.refinedmods.refinedstorage.api.network.INetworkNodeGraphEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Mixin(targets = "com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeGraph$Operator", remap = false)
public class NetworkNodeGraphOperatorOptimizeMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newConcurrentHashSet()Ljava/util/Set;", ordinal = 0))
    private Set<INetworkNodeGraphEntry> optimizeFoundNodes() {
        if (!mctmods.rsmixin.Config.ENABLE_HASHSET_OPTIMIZE.get()) {
            return Sets.newConcurrentHashSet();
        }
        return new HashSet<>();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newConcurrentHashSet()Ljava/util/Set;", ordinal = 1))
    private Set<INetworkNodeGraphEntry> optimizeNewEntries() {
        if (!mctmods.rsmixin.Config.ENABLE_HASHSET_OPTIMIZE.get()) {
            return Sets.newConcurrentHashSet();
        }
        return new HashSet<>();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newConcurrentHashSet(Ljava/lang/Iterable;)Ljava/util/Set;"))
    private Set<INetworkNodeGraphEntry> optimizePreviousEntries(Iterable<? extends INetworkNodeGraphEntry> entries) {
        if (!mctmods.rsmixin.Config.ENABLE_HASHSET_OPTIMIZE.get()) {
            return Sets.newConcurrentHashSet(entries);
        }
        return new HashSet<>((Collection<? extends INetworkNodeGraphEntry>) entries);
    }
}
