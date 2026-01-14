package mctmods.rsmixin.mixin;

import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeManager;
import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.ActiveFastNodesAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(NetworkNodeManager.class)
public abstract class NetworkNodeManagerMixin implements ActiveFastNodesAccessor {
    @Unique
    private final Set<INetworkNode> rsmixin$activeFastNodes = ConcurrentHashMap.newKeySet();

    @Override
    public Set<INetworkNode> rsmixin$getActiveFastNodes() {
        return rsmixin$activeFastNodes;
    }

    @Override
    public void rsmixin$addActiveFastNode(INetworkNode node) {
        rsmixin$activeFastNodes.add(node);
    }

    @Override
    public void rsmixin$removeActiveFastNode(INetworkNode node) {
        rsmixin$activeFastNodes.remove(node);
    }

    @Inject(method = "setNode", at = @At("TAIL"), remap = false)
    private void onSetNode(net.minecraft.core.BlockPos pos, INetworkNode node, CallbackInfo ci) {
        if (Config.FAST_NODE_CLASSES.get().contains(node.getClass().getName())) {
            rsmixin$addActiveFastNode(node);
        }
    }

    @Inject(method = "removeNode", at = @At("HEAD"), remap = false)
    private void onRemoveNode(net.minecraft.core.BlockPos pos, CallbackInfo ci) {
        INetworkNode node = ((NetworkNodeManager) (Object) this).getNode(pos);
        if (node != null) {
            rsmixin$removeActiveFastNode(node);
        }
    }
}
