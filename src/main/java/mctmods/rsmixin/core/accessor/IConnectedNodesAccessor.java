package mctmods.rsmixin.core.accessor;

import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import java.util.Set;

public interface IConnectedNodesAccessor {
    Set<INetworkNode> rsmixin$getConnectedNodes();

    void rsmixin$addConnectedNode(INetworkNode node);

    void rsmixin$removeConnectedNode(INetworkNode node);
}
