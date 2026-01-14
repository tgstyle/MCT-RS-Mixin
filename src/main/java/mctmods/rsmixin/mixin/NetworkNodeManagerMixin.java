package mctmods.rsmixin.mixin;

import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeManager;
import mctmods.rsmixin.core.accessor.ActiveFastNodesAccessor;
import mctmods.rsmixin.core.accessor.ConnectedNodesAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(NetworkNodeManager.class)
public abstract class NetworkNodeManagerMixin implements ActiveFastNodesAccessor, ConnectedNodesAccessor {
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
}
