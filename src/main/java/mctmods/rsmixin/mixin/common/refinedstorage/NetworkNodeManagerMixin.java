package mctmods.rsmixin.mixin.common.refinedstorage;

import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeManager;
import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.IActiveFastNodesAccessor;
import mctmods.rsmixin.core.accessor.IConnectedNodesAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(NetworkNodeManager.class)
public abstract class NetworkNodeManagerMixin implements IActiveFastNodesAccessor, IConnectedNodesAccessor {
    @Shadow(remap = false) @Final private ConcurrentHashMap<BlockPos, INetworkNode> nodes;

    @Unique
    private Set<INetworkNode> rsmixin$activeFastNodes;

    @Unique
    private Set<INetworkNode> rsmixin$connectedNodes;

    @Override
    public Set<INetworkNode> rsmixin$getActiveFastNodes() {
        if (rsmixin$activeFastNodes == null) {
            rsmixin$activeFastNodes = ConcurrentHashMap.newKeySet();
        }
        return rsmixin$activeFastNodes;
    }

    @Override
    public void rsmixin$addActiveFastNode(INetworkNode node) {
        rsmixin$getActiveFastNodes().add(node);
    }

    @Override
    public void rsmixin$removeActiveFastNode(INetworkNode node) {
        rsmixin$getActiveFastNodes().remove(node);
    }

    @Override
    public Set<INetworkNode> rsmixin$getConnectedNodes() {
        if (rsmixin$connectedNodes == null) {
            rsmixin$connectedNodes = ConcurrentHashMap.newKeySet();
        }
        return rsmixin$connectedNodes;
    }

    @Override
    public void rsmixin$addConnectedNode(INetworkNode node) {
        rsmixin$getConnectedNodes().add(node);
    }

    @Override
    public void rsmixin$removeConnectedNode(INetworkNode node) {
        rsmixin$getConnectedNodes().remove(node);
    }

    @Inject(method = "load", at = @At("TAIL"), remap = false) private void rsmixin$tickLoadedNodes(CompoundTag tag, CallbackInfo ci) {
        if (Config.ENABLE_CONNECTED_NODE_TICK_OPTIMIZE.get()) { rsmixin$getConnectedNodes().addAll(nodes.values()); }
    }

    @Inject(method = "setNode", at = @At("TAIL"), remap = false) private void rsmixin$tickNewNode(BlockPos pos, INetworkNode node, CallbackInfo ci) {
        if (Config.ENABLE_CONNECTED_NODE_TICK_OPTIMIZE.get()) { rsmixin$addConnectedNode(node); }
    }

    @Inject(method = "removeNode", at = @At("HEAD"), remap = false) private void rsmixin$untickRemovedNode(BlockPos pos, CallbackInfo ci) {
        INetworkNode node = pos == null ? null : nodes.get(pos);
        if (node != null) { rsmixin$removeConnectedNode(node); }
    }
}
