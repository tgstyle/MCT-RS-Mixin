package mctmods.rsmixin.helper.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.util.Action;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RSMixin.MODID) public class GraphRescanScheduler {
    private static final Map<Integer, Set<INetwork>> PENDING = new ConcurrentHashMap<>();
    private static boolean bypassOnce = false;

    public static void schedule(INetwork network) {
        PENDING.computeIfAbsent(network.world().provider.getDimension(), k -> new LinkedHashSet<>()).add(network);
        if (Config.enableDebugLogging) { RSMixin.LOGGER.debug("RSMixin: Coalesced graph rescan for network at {} (deferred to end of tick)", network.getPosition()); }
    }

    public static boolean consumeBypass() {
        boolean value = bypassOnce;
        bypassOnce = false;
        return value;
    }

    @SubscribeEvent public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) { return; }

        Set<INetwork> pending = PENDING.remove(event.world.provider.getDimension());
        if (pending == null || pending.isEmpty()) { return; }

        List<INetwork> toRescan = new ArrayList<>(pending);
        for (INetwork network : toRescan) {
            if (network.world() == null || !network.world().isBlockLoaded(network.getPosition())) { continue; }
            bypassOnce = true;
            network.getNodeGraph().invalidate(Action.PERFORM, network.world(), network.getPosition());
            bypassOnce = false;
        }

        if (Config.enableDebugLogging) { RSMixin.LOGGER.debug("RSMixin: Flushed {} coalesced graph rescan(s) in dim {}", toRescan.size(), event.world.provider.getDimension()); }
    }
}
