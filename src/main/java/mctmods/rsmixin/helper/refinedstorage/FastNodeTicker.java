package mctmods.rsmixin.helper.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.accessor.IActiveFastNodesAccessor;

import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.network.NetworkNodeManager;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RSMixin.MODID) public class FastNodeTicker {
    private static final Logger LOGGER = LogManager.getLogger(RSMixin.MODID);
    private static final Map<Integer, Integer> previousActiveCounts = new ConcurrentHashMap<>();

    @SubscribeEvent public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (!Config.enableThrottle || !Config.enableBypassFastNodes) { return; }
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) { return; }

        int interval = Config.throttleInterval;
        if (interval <= 1) { return; }

        long gameTime = event.world.getTotalWorldTime();
        int dimension = event.world.provider.getDimension();

        if (gameTime % interval == 0) {
            if (Config.enableDebugLogging) { LOGGER.debug("RS Throttle: Full update tick in dim {}", dimension); }
            return;
        }

        event.world.profiler.startSection("rs fast node ticking");

        NetworkNodeManager manager = (NetworkNodeManager) API.instance().getNetworkNodeManager(event.world);
        Set<INetworkNode> active = ((IActiveFastNodesAccessor) manager).rsmixin$getActiveFastNodes();

        if (active == null) {
            LOGGER.error("Active fast nodes set is null in dimension {}! Verify NetworkNodeManagerMixin is applied and field initialized.", dimension);
            event.world.profiler.endSection();
            return;
        }

        int currentCount = active.size();
        Integer prevCount = previousActiveCounts.get(dimension);

        if (Config.enableDebugLogging && (prevCount == null || prevCount != currentCount)) { LOGGER.debug("Active fast nodes in dim {}: {} (changed from {})", dimension, currentCount, prevCount == null ? "none" : prevCount); }
        previousActiveCounts.put(dimension, currentCount);

        for (INetworkNode node : active) { node.update(); }

        event.world.profiler.endSection();
    }
}
