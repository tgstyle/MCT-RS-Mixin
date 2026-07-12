package mctmods.rsmixin.helper.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = RSMixin.MODID) public class CraftingTicker {
    private static final Set<INetwork> ACTIVE_NETWORKS = Collections.newSetFromMap(new WeakHashMap<>());

    public static void register(INetwork network) {
        ACTIVE_NETWORKS.add(network);
        if (Config.enableDebugLogging) { RSMixin.LOGGER.debug("RSMixin: Enabling dynamic crafting bypass"); }
    }

    public static void unregister(INetwork network) {
        ACTIVE_NETWORKS.remove(network);
        if (Config.enableDebugLogging) { RSMixin.LOGGER.debug("RSMixin: Disabling dynamic crafting bypass"); }
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) { return; }

        if (!Config.enableDynamicCraftingBypass ||
                !Config.enableThrottle ||
                Config.throttleInterval <= 1 ||
                !Config.enableBypassFastNodes) {
            ACTIVE_NETWORKS.clear();
            return;
        }

        ACTIVE_NETWORKS.removeIf(net -> net == null || net.world() == null);

        int interval = Config.throttleInterval;
        List<INetwork> toTick = new ArrayList<>(ACTIVE_NETWORKS);
        for (INetwork net : toTick) {
            if (net.world().getTotalWorldTime() % interval == 0) { continue; }
            if (net.canRun()) { net.getCraftingManager().update(); }
        }
    }
}
