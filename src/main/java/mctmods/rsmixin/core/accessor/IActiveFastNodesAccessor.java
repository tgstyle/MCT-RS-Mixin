package mctmods.rsmixin.core.accessor;

import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import java.util.Set;

public interface IActiveFastNodesAccessor {
    Set<INetworkNode> rsmixin$getActiveFastNodes();

    void rsmixin$addActiveFastNode(INetworkNode node);

    void rsmixin$removeActiveFastNode(INetworkNode node);
}
