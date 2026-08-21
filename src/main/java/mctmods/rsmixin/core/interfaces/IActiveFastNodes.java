package mctmods.rsmixin.core.interfaces;

import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import java.util.Set;

public interface IActiveFastNodes {
    Set<INetworkNode> rsmixin$getActiveFastNodes();

    void rsmixin$addActiveFastNode(INetworkNode node);

    void rsmixin$removeActiveFastNode(INetworkNode node);
}
