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

    @Comment({
            "Master switch for network update throttling.",
            "When enabled, full RS network updates run only every throttleInterval ticks instead of every tick.",
            "This reduces server load in large networks but slows down some operations if not bypassed."})
    public static boolean enableThrottle = true;

    @Comment({
            "How often (in ticks) full network updates occur when throttling is enabled.",
            "1 = no throttling (every tick), 20 = every second (default, ~1 operation/sec for non-bypassed nodes).",
            "Only matters if enableThrottle is true."}) @RangeInt(min = 1, max = 1000)
    public static int throttleInterval = 20;

    @Comment({
            "Allows specific nodes (importers, exporters, interfaces, etc.) to bypass throttling and update more frequently.",
            "",
            "- ENABLED (default): Listed nodes can update every tick (full speed when active; speed upgrades respected).",
            "  Higher performance cost when nodes are busy, but fast transfer rates.",
            "",
            "- DISABLED: All nodes strictly follow throttleInterval (~1 operation/sec at interval=20).",
            "  Speed upgrades are ignored (forced to 1) for consistent rate.",
            "",
            "Interacts with fastNodeClasses (defines which nodes can bypass) and enableDynamicNodeSleep (controls node sleep behaviour when bypassing)."})
    public static boolean enableBypassFastNodes = true;

    @Comment({
            "List of node class names that are allowed to bypass throttling when enableBypassFastNodes is true.",
            "Defaults cover core RS nodes (importers, exporters, interfaces, crafters, etc.).",
            "Adding extra classes is safe—even if the mod isn't installed (string matching only).",
            "Only relevant when enableBypassFastNodes is enabled."})
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

    @Comment({
            "On controller load/reload (world load, relog, chunk load), delay then force a full network rescan.",
            "Fixes connection issues with capability-based cables (e.g., EnderIO conduits) that aren't detected immediately on load.",
            "Safe and recommended for most setups."})
    public static boolean enableLoadRescan = true;

    @Comment({
            "Ticks to wait after detecting controller load before rescanning.",
            "Gives time for block entities and capabilities to initialize. Default 20 (~1 second).",
            "Only matters if enableLoadRescan is true."}) @RangeInt(min = 0, max = 400)
    public static int loadRescanDelay = 20;

    @Comment("Cache the controller's energy usage instead of summing every node twice per tick. Refreshed on graph changes, redstone mode changes, and every 20 ticks.")
    public static boolean enableLazyEnergy = true;

    @Comment("Replace ConcurrentHashMap/ConcurrentHashSet with regular HashMap/HashSet in single-threaded contexts. Small performance gain.")
    public static boolean enableHashSetOptimize = true;

    @Comment("Skip processing unloaded positions during network graph updates. Improves performance and prevents rare deadlocks on chunk unload.")
    public static boolean enableSkipUnloaded = true;

    @Comment({
            "Makes fast nodes sleep when idle (only relevant when enableBypassFastNodes is true and node is in fastNodeClasses).",
            "",
            "- ENABLED (default): Nodes update every tick only when work is available.",
            "  When idle for a few cycles, they fall back to throttled updates → lower overhead.",
            "",
            "- DISABLED: Nodes always update every tick when bypassing is allowed (full speed even when idle)."})
    public static boolean enableDynamicNodeSleep = true;

    @Comment({
            "Enables dynamic throttling bypass for the crafting manager.",
            "",
            "- ENABLED (default): When there are active crafting tasks, the crafting manager updates every tick → fast crafting while active.",
            "  Falls back to throttled updates when idle → lower overhead.",
            "",
            "- DISABLED: Crafting manager strictly follows throttleInterval.",
            "",
            "Only relevant when enableThrottle is true, throttleInterval > 1, and enableBypassFastNodes is true.",
            "Fully respects crafter speed upgrades."})
    public static boolean enableDynamicCraftingBypass = true;

    @Comment("Optimizes ticking by only updating nodes that are connected to a network. Disconnected nodes are skipped, reducing unnecessary overhead.")
    public static boolean enableConnectedNodeTickOptimize = true;

    @Comment({
            "Fixes RebornStorage multiblock crafters rebuilding their patterns and re-queuing crafting jobs more often than needed.",
            "- Force-refreshes the multiblock's status when its GUI is opened, instead of waiting for its own update timer.",
            "- Only re-queues the crafting manager when a pattern actually changed, instead of on every scheduled rebuild.",
            "Safe to disable if newer versions fix these."})
    public static boolean enableRebornstorageCrafterFix = true;

    @Comment({
            "Stops your item/fluid list from rebuilding itself over and over when things reconnect.",
            "Normally, every storage block on your network can trigger a full list rebuild at the same time,",
            "which causes lag spikes when your world loads or when you place/break a lot of storage at once.",
            "This makes it rebuild the list just once instead."})
    public static boolean enableStorageCacheDebounce = true;

    @Comment({
            "Stops autocrafting patterns from being re-scanned over and over when crafters reconnect.",
            "Normally, every crafter reconnecting (like on world load) can trigger its own full pattern re-scan,",
            "which gets very slow with lots of crafters. This waits until everything has reconnected, then scans once.",
            "Works for regular RS crafters and RebornStorage crafters."})
    public static boolean enableCraftingRebuildDebounce = true;

    @Comment("Index crafting patterns by output item/fluid so pattern lookups are hash lookups instead of scanning every pattern. Big win with large pattern libraries (e.g. RebornStorage multiblocks).")
    public static boolean enablePatternLookupIndex = true;

    @Comment("Only mark the controller chunk dirty at most once per second while crafting tasks are running, instead of every tick.")
    public static boolean enableCraftingDirtyThrottle = true;

    @Comment({
            "Stops network rescans from piling up when you place or break lots of network blocks at once.",
            "The first block still connects instantly, just like normal.",
            "Any extra rescans triggered in that same instant are merged into one, run a moment later.",
            "Great for builders, schematics, or quarries that place many cables/blocks in one go.",
            "Worst case, a block takes one extra tick to connect (not noticeable in normal play)."})
    public static boolean enableGraphRescanCoalesce = true;

    @Comment({
            "Speeds up autocrafting for big crafting trees.",
            "Normally, every item that comes back into a crafting job gets checked against every single step",
            "of that job to see where it belongs, which gets slow with big/multi-step crafts.",
            "This looks it up directly instead, so it only checks the steps that actually need that item."})
    public static boolean enableTrackedInsertIndex = true;

    @Comment({
            "Stops a broken autocrafting task from crashing the whole server.",
            "RS can hit an internal inventory mismatch during processing crafts and hard-crash",
            "the server, and because the broken task is saved with the world, it crashes again on every reboot.",
            "With this on, the broken task is safely cancelled instead: its items are refunded to storage,",
            "an error is written to the log, and the server keeps running."})
    public static boolean enableCraftingCrashGuard = true;

    @Comment({
            "Fixes grids randomly showing missing/wrong/empty item lists until reopened.",
            "Whenever the network rebuilds its item list (any block placed/broken on the network, chunks",
            "loading, etc.) while a grid is open, vanilla RS forgets to tell the player's screen about it,",
            "leaving it permanently out of sync until the grid is reopened.",
            "With this on, open grids are refreshed automatically whenever that happens."})
    public static boolean enableGridResync = true;

    @Comment({
            "When enabled, wireless items (Wireless Grid, Fluid Grid, Crafting Monitor, and addon wireless items)",
            "can only be used in the same dimension as the network they are linked to, and Network Transmitters",
            "will not link to a Network Receiver in another dimension.",
            "Trying to use a wireless item from another dimension shows an on-screen message instead of opening,",
            "even if an addon's infinite/cross-dimension wireless transmitter would normally allow it.",
            "Disabled by default (vanilla behavior)."})
    public static boolean enableWirelessDimensionLock = false;

    @SubscribeEvent public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(RSMixin.MODID)) { ConfigManager.sync(RSMixin.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE); }
    }
}
