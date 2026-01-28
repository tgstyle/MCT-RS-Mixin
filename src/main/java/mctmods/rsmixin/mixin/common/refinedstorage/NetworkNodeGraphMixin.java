package mctmods.rsmixin.mixin.common.refinedstorage;

import com.google.common.collect.Sets;
import com.refinedmods.refinedstorage.api.network.INetworkNodeGraphEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeGraph.class, remap = false)
public class NetworkNodeGraphMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Sets;newConcurrentHashSet()Ljava/util/Set;"))
    private Set<INetworkNodeGraphEntry> optimizeEntries() {
        if (!mctmods.rsmixin.Config.ENABLE_HASHSET_OPTIMIZE.get()) {
            return Sets.newConcurrentHashSet();
        }
        return new HashSet<>();
    }
}
