package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.interfaces.IConnectedNodes;

import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeManager;
import com.raoulvdberge.refinedstorage.apiimpl.network.NetworkNodeListener;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mixin(value = NetworkNodeListener.class, remap = false) public abstract class NetworkNodeListenerMixin {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Inject(method = "onWorldTick", at = @At("HEAD"), cancellable = true) private void throttle(TickEvent.WorldTickEvent e, CallbackInfo ci) {
        if (!Config.enableThrottle) { return; }
        if (e.phase != TickEvent.Phase.END || e.world.isRemote) { return; }

        int interval = Config.throttleInterval;
        if (interval > 1 && e.world.getTotalWorldTime() % interval != 0) {
            ci.cancel();
            return;
        }
        if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RS Throttle: Full update tick"); }
    }

    @Redirect(method = "onWorldTick", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/api/network/node/INetworkNodeManager;all()Ljava/util/Collection;")) private Collection<INetworkNode> redirectAll(INetworkNodeManager manager) {
        if (!Config.enableConnectedNodeTickOptimize) { return manager.all(); }

        Set<INetworkNode> connected = ((IConnectedNodes) manager).rsmixin$getConnectedNodes();
        List<INetworkNode> toTick = new ArrayList<>(connected);
        for (INetworkNode node : manager.all()) {
            if (!(node instanceof NetworkNode)) { toTick.add(node); }
        }
        return toTick;
    }
}
