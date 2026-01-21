package mctmods.rsmixin.helper.refinedstorage;

import com.refinedmods.refinedstorage.api.network.INetwork;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class CraftingTicker {
    private static final Set<INetwork> ACTIVE_NETWORKS = Collections.newSetFromMap(new WeakHashMap<>());

    public static void register(INetwork network) {
        ACTIVE_NETWORKS.add(network);
        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            RSMixin.LOGGER.debug("RSMixin: Enabling dynamic crafting bypass");
        }
    }

    public static void unregister(INetwork network) {
        ACTIVE_NETWORKS.remove(network);
        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            RSMixin.LOGGER.debug("RSMixin: Disabling dynamic crafting bypass");
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!Config.ENABLE_DYNAMIC_CRAFTING_BYPASS.get() ||
                !Config.ENABLE_THROTTLE.get() ||
                Config.THROTTLE_INTERVAL.get() <= 1 ||
                !Config.ENABLE_BYPASS_FAST_NODES.get()) {
            ACTIVE_NETWORKS.clear();
            return;
        }

        ACTIVE_NETWORKS.removeIf(net -> net == null || net.getLevel() == null);

        List<INetwork> toTick = new ArrayList<>(ACTIVE_NETWORKS);
        for (INetwork net : toTick) {
            if (net.canRun()) {
                net.getCraftingManager().update();
            }
        }
    }
}
