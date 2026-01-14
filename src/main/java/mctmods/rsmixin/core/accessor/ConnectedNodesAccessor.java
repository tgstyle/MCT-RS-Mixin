package mctmods.rsmixin.core.accessor;

import com.refinedmods.refinedstorage.api.network.node.INetworkNode;

import java.util.Set;

public interface ConnectedNodesAccessor {
    Set<INetworkNode> rsmixin$getConnectedNodes();

    void rsmixin$addConnectedNode(INetworkNode node);

    void rsmixin$removeConnectedNode(INetworkNode node);
}
