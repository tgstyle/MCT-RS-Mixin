package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.google.common.collect.Sets;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Mixin(targets = "com.raoulvdberge.refinedstorage.apiimpl.network.NetworkNodeGraph$Operator", remap = false) public class NetworkNodeGraphOperatorOptimizeMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newConcurrentHashSet()Ljava/util/Set;", ordinal = 0)) private Set<INetworkNode> optimizeFoundNodes() {
        if (!Config.enableHashSetOptimize) { return Sets.newConcurrentHashSet(); }
        return new HashSet<>();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newConcurrentHashSet()Ljava/util/Set;", ordinal = 1)) private Set<INetworkNode> optimizeNewNodes() {
        if (!Config.enableHashSetOptimize) { return Sets.newConcurrentHashSet(); }
        return new HashSet<>();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newConcurrentHashSet(Ljava/lang/Iterable;)Ljava/util/Set;")) private Set<INetworkNode> optimizePreviousNodes(Iterable<? extends INetworkNode> elements) {
        if (!Config.enableHashSetOptimize) { return Sets.newConcurrentHashSet(elements); }
        return new HashSet<>((Collection<? extends INetworkNode>) elements);
    }
}
