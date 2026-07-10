package mctmods.rsmixin.helper.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.Action;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GraphRescanScheduler {
    private static final Map<ResourceKey<Level>, Set<INetwork>> PENDING = new ConcurrentHashMap<>();
    private static boolean bypassOnce = false;

    public static void schedule(INetwork network) {
        PENDING.computeIfAbsent(network.getLevel().dimension(), k -> new LinkedHashSet<>()).add(network);
        if (Config.ENABLE_DEBUG_LOGGING.get()) { RSMixin.LOGGER.debug("RSMixin: Coalesced graph rescan for network at {} (deferred to end of tick)", network.getPosition()); }
    }

    public static boolean consumeBypass() {
        boolean value = bypassOnce;
        bypassOnce = false;
        return value;
    }

    @SubscribeEvent public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) { return; }

        Set<INetwork> pending = PENDING.remove(event.level.dimension());
        if (pending == null || pending.isEmpty()) { return; }

        List<INetwork> toRescan = new ArrayList<>(pending);
        for (INetwork network : toRescan) {
            if (network.getLevel() == null || !network.getLevel().isLoaded(network.getPosition())) { continue; }
            bypassOnce = true;
            network.getNodeGraph().invalidate(Action.PERFORM, network.getLevel(), network.getPosition());
            bypassOnce = false;
        }

        if (Config.ENABLE_DEBUG_LOGGING.get()) { RSMixin.LOGGER.debug("RSMixin: Flushed {} coalesced graph rescan(s) in {}", toRescan.size(), event.level.dimension().location()); }
    }
}
