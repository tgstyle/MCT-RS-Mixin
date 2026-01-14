package mctmods.rsmixin;

import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.apiimpl.API;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FastNodeTicker {
    private static final Logger LOGGER = LogManager.getLogger("RSMixin");

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!Config.ENABLE_THROTTLE.get() || !Config.ENABLE_BYPASS_FAST_NODES.get()) return;

        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;

        int interval = Config.THROTTLE_INTERVAL.get();
        if (interval <= 1) return;

        if (event.level.getGameTime() % interval == 0) return; // Full updates handled by original listener

        event.level.getProfiler().push("rs fast node ticking");

        for (INetworkNode node : API.instance().getNetworkNodeManager((ServerLevel) event.level).all()) {
            if (isFastNode(node)) {
                node.update();
            }
        }

        event.level.getProfiler().pop();

        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            LOGGER.debug("RS Throttle: Fast nodes update tick");
        }
    }

    private boolean isFastNode(INetworkNode node) {
        String className = node.getClass().getName();
        return Config.FAST_NODE_CLASSES.get().contains(className);
    }
}
