package mctmods.rsmixin;

import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@net.minecraftforge.common.config.Config(modid = RSMixin.MODID) @EventBusSubscriber(modid = RSMixin.MODID) public class Config {
    @Comment("Turns on extra log messages for troubleshooting. Can get spammy, leave off unless you're diagnosing a problem.")
    public static boolean enableDebugLogging = false;

    @Comment("Master switch for network update throttling. When enabled, full RS network updates run only every throttleInterval ticks instead of every tick.")
    public static boolean enableThrottle = true;

    @Comment("How often (in ticks) full network updates occur when throttling is enabled. 1 = no throttling, 20 = every second.") @RangeInt(min = 1, max = 1000)
    public static int throttleInterval = 20;

    @Comment("Allows specific nodes (importers, exporters, interfaces, etc.) to bypass throttling and update every tick.")
    public static boolean enableBypassFastNodes = true;

    @Comment("List of node class names that are allowed to bypass throttling when enableBypassFastNodes is true.")
    public static String[] fastNodeClasses = new String[]{
            "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeImporter",
            "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeExporter",
            "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeInterface",
            "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeFluidInterface",
            "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeCrafter",
            "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeConstructor",
            "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeDestructor",
            "com.raoulvdberge.refinedstorage.apiimpl.network.node.diskmanipulator.NetworkNodeDiskManipulator"
    };

    @Comment("On controller load/reload (world load, relog, chunk load), delay then force a full network rescan. Fixes connection issues with capability-based cables (e.g., EnderIO conduits).")
    public static boolean enableLoadRescan = true;

    @Comment("Ticks to wait after detecting controller load before rescanning. Default 20 (~1 second).") @RangeInt(min = 0, max = 400)
    public static int loadRescanDelay = 20;

    @Comment("Cache the controller's energy usage instead of summing every node twice per tick. Refreshed on graph changes, redstone mode changes, and every 20 ticks.")
    public static boolean enableLazyEnergy = true;

    @Comment("Replace concurrent hash sets in the network graph scan with plain HashSets for faster full rescans.")
    public static boolean enableHashSetOptimize = true;

    @Comment("Skip network/node graph operations for positions whose chunks are not loaded (prevents chunk-unload cascades and load-on-scan).")
    public static boolean enableSkipUnloaded = true;

    @Comment("Let bypassed fast nodes sleep when they had no work, waking them on network changes.")
    public static boolean enableDynamicNodeSleep = true;

    @Comment("Let crafting tasks tick every tick while active even when throttled.")
    public static boolean enableDynamicCraftingBypass = true;

    @Comment("Only tick nodes that are actually connected and active instead of iterating all graph entries.")
    public static boolean enableConnectedNodeTickOptimize = true;

    @Comment("Enable RebornStorage multiblock crafter fixes.")
    public static boolean enableRebornstorageCrafterFix = true;

    @Comment("Debounce storage cache invalidation during graph rescans so the cache rebuilds once per rescan instead of once per node.")
    public static boolean enableStorageCacheDebounce = true;

    @Comment("Debounce crafting pattern rebuilds during graph rescans so patterns rebuild once per rescan instead of once per crafter.")
    public static boolean enableCraftingRebuildDebounce = true;

    @Comment("Index crafting patterns by output item/fluid so pattern lookups are hash lookups instead of scanning every pattern. Big win with large pattern libraries (e.g. RebornStorage multiblocks).")
    public static boolean enablePatternLookupIndex = true;

    @Comment("Only mark the controller chunk dirty at most once per second while crafting tasks are running, instead of every tick.")
    public static boolean enableCraftingDirtyThrottle = true;

    @Comment("Coalesce multiple graph rescans in the same tick into a single deferred rescan at end of tick.")
    public static boolean enableGraphRescanCoalesce = true;

    @Comment("Track last successful insert index in crafters to avoid rescanning all slots.")
    public static boolean enableTrackedInsertIndex = true;

    @Comment("Guard crafting calculator/task internals against crashes from malformed patterns.")
    public static boolean enableCraftingCrashGuard = true;

    @Comment("Resync grid storage cache listeners to prevent desynced grid contents.")
    public static boolean enableGridResync = true;

    @Comment("When enabled, wireless items (Wireless Grid, Fluid Grid, Crafting Monitor, and addon wireless items) can only be used in the same dimension as the network they are linked to, and Network Transmitters will not link to a Network Receiver in another dimension. Trying to use a wireless item from another dimension shows an on-screen message instead of opening, even if an addon's cross-dimension wireless transmitter would normally allow it. Disabled by default (vanilla behavior).")
    public static boolean enableWirelessDimensionLock = false;

    @SubscribeEvent public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(RSMixin.MODID)) { ConfigManager.sync(RSMixin.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE); }
    }
}
