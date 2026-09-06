package mctmods.rsmixin.helper.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.interfaces.IActiveFastNodes;

import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.network.NetworkNodeManager;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RSMixin.MODID) public class FastNodeTicker {
    private static final Logger LOGGER = LogManager.getLogger(RSMixin.MODID);
    private static final Map<Integer, Integer> previousActiveCounts = new ConcurrentHashMap<>();

    public static boolean assertForced(INetworkNode node, boolean forced, String what) {
        World world = node.getWorld();
        if (world == null || world.isRemote || node.getNetwork() == null) { return forced; }

        boolean wanted = Config.enableBypassFastNodes && Arrays.asList(Config.fastNodeClasses).contains(node.getClass().getName());
        IActiveFastNodes manager = (IActiveFastNodes) API.instance().getNetworkNodeManager(world);

        if (wanted) {
            manager.rsmixin$addActiveFastNode(node);
            if (!forced && Config.enableDebugLogging) { LOGGER.debug("Forcing {} at {} into the fast node set", what, node.getPos()); }
        }
        else if (forced) {
            manager.rsmixin$removeActiveFastNode(node);
            if (Config.enableDebugLogging) { LOGGER.debug("Released {} at {} from the fast node set", what, node.getPos()); }
        }

        return wanted;
    }

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
        Set<INetworkNode> active = ((IActiveFastNodes) manager).rsmixin$getActiveFastNodes();

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
