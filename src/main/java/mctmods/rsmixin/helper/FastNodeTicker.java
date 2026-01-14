package mctmods.rsmixin.helper;

import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeManager;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.ActiveFastNodesAccessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static mctmods.rsmixin.RSMixin.MODID;

public class FastNodeTicker {
    private static final Logger LOGGER = LogManager.getLogger(MODID);

    private static final Map<ResourceKey<Level>, Integer> previousActiveCounts = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!Config.ENABLE_THROTTLE.get() || !Config.ENABLE_BYPASS_FAST_NODES.get()) return;

        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;

        int interval = Config.THROTTLE_INTERVAL.get();
        if (interval <= 1) return;

        long gameTime = event.level.getGameTime();
        ResourceKey<Level> dimension = event.level.dimension();

        if (gameTime % interval == 0) {
            if (Config.ENABLE_DEBUG_LOGGING.get()) {
                LOGGER.debug("RS Throttle: Full update tick in {}", dimension.location());
            }
            return;
        }

        event.level.getProfiler().push("rs fast node ticking");

        NetworkNodeManager manager = (NetworkNodeManager) API.instance().getNetworkNodeManager((ServerLevel) event.level);
        Set<INetworkNode> active = ((ActiveFastNodesAccessor) manager).rsmixin$getActiveFastNodes();

        if (active == null) {
            LOGGER.error("Active fast nodes set is null in dimension {}! Verify NetworkNodeManagerMixin is applied and field initialized.", dimension.location());
            event.level.getProfiler().pop();
            return;
        }

        int currentCount = active.size();
        Integer prevCount = previousActiveCounts.get(dimension);

        if (Config.ENABLE_DEBUG_LOGGING.get() && (prevCount == null || prevCount != currentCount)) {
            LOGGER.debug("Active fast nodes in {}: {} (changed from {})", dimension.location(), currentCount, prevCount == null ? "none" : prevCount);
        }
        previousActiveCounts.put(dimension, currentCount);

        for (INetworkNode node : active) {
            node.update();
        }

        event.level.getProfiler().pop();
    }
}
