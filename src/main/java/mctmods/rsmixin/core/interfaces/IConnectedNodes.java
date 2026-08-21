package mctmods.rsmixin.core.interfaces;

import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import java.util.Set;

public interface IConnectedNodes {
    Set<INetworkNode> rsmixin$getConnectedNodes();

    void rsmixin$addConnectedNode(INetworkNode node);

    void rsmixin$removeConnectedNode(INetworkNode node);
}
