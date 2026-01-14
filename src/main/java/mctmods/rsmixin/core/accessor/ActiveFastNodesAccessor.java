package mctmods.rsmixin.core.accessor;

import com.refinedmods.refinedstorage.api.network.node.INetworkNode;

import java.util.Set;

public interface ActiveFastNodesAccessor {
    Set<INetworkNode> rsmixin$getActiveFastNodes();

    void rsmixin$addActiveFastNode(INetworkNode node);

    void rsmixin$removeActiveFastNode(INetworkNode node);
}
