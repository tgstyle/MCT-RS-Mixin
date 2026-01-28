package mctmods.rsmixin.mixin.common.refinedstorage;

import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.api.network.node.INetworkNodeManager;
import com.refinedmods.refinedstorage.apiimpl.network.NetworkListener;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.ConnectedNodesAccessor;

import net.minecraftforge.event.TickEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(NetworkListener.class)
public abstract class NetworkListenerMixin {
    @Unique
    private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Inject(method = "onLevelTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void throttle(TickEvent.LevelTickEvent event, CallbackInfo ci) {
        if (!Config.ENABLE_THROTTLE.get()) return;

        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;

        int interval = Config.THROTTLE_INTERVAL.get();
        if (interval > 1 && event.level.getGameTime() % interval != 0) {
            ci.cancel();
            return;
        }
        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            rsmixin$LOGGER.debug("RS Throttle: Full update tick");
        }
    }

    @Redirect(method = "onLevelTick", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/api/network/node/INetworkNodeManager;all()Ljava/util/Collection;", remap = false), remap = false)
    private Collection<INetworkNode> redirectAll(INetworkNodeManager manager) {
        if (Config.ENABLE_CONNECTED_NODE_TICK_OPTIMIZE.get()) {
            return ((ConnectedNodesAccessor) manager).rsmixin$getConnectedNodes();
        } else {
            return manager.all();
        }
    }
}
