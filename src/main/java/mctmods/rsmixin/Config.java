package mctmods.rsmixin;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DEBUG_LOGGING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_THROTTLE;
    public static final ForgeConfigSpec.IntValue THROTTLE_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BYPASS_FAST_NODES;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> FAST_NODE_CLASSES;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LOAD_RESCAN;
    public static final ForgeConfigSpec.IntValue LOAD_RESCAN_DELAY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CONDUIT_PLACEMENT_FIX;
    public static final ForgeConfigSpec.IntValue CONDUIT_PLACEMENT_RESCAN_DELAY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LAZY_ENERGY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HASHSET_OPTIMIZE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SKIP_UNLOADED;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_NODE_SLEEP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_CRAFTING_BYPASS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CONNECTED_NODE_TICK_OPTIMIZE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENDERIO_RS_FIX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENDERIO_CONDUIT_TYPED_BACKUP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_REBORNSTORAGE_CRAFTER_FIX;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push(RSMixin.MODID);

        ENABLE_DEBUG_LOGGING = builder
                .comment("Enables extra debug logging (throttle ticks, active node counts, importer activation/sleep, unloaded skips, etc.). Useful for diagnosing issues but spammy in logs.")
                .define("enableDebugLogging", false);

        ENABLE_THROTTLE = builder
                .comment("""
                        Master switch for network update throttling.
                        When enabled, full RS network updates run only every throttleInterval ticks instead of every tick.
                        This reduces server load in large networks but slows down some operations if not bypassed.""")
                .define("enableThrottle", true);

        THROTTLE_INTERVAL = builder
                .comment("""
                        How often (in ticks) full network updates occur when throttling is enabled.
                        1 = no throttling (every tick), 20 = every second (default, ~1 operation/sec for non-bypassed nodes).
                        Only matters if enableThrottle is true.""")
                .defineInRange("throttleInterval", 20, 1, 1000);

        ENABLE_BYPASS_FAST_NODES = builder
                .comment("""
                        Allows specific nodes (importers, exporters, interfaces, etc.) to bypass throttling and update more frequently.
                        
                        - ENABLED (default): Listed nodes can update every tick (full speed when active; speed upgrades respected).
                          Higher performance cost when nodes are busy, but fast transfer rates.
                        
                        - DISABLED: All nodes strictly follow throttleInterval (~1 operation/sec at interval=20).
                          Speed upgrades are ignored (forced to 1) for consistent rate.
                        
                        Interacts with fastNodeClasses (defines which nodes can bypass) and enableDynamicNodeSleep (controls node sleep behaviour when bypassing).""")
                .define("enableBypassFastNodes", true);

        FAST_NODE_CLASSES = builder
                .comment("""
                        List of node class names that are allowed to bypass throttling when enableBypassFastNodes is true.
                        Defaults cover core RS nodes (importers, exporters, interfaces, crafters, etc.) and popular addons.
                        Adding extra classes is safe—even if the mod isn't installed (string matching only).
                        Only relevant when enableBypassFastNodes is enabled.""")
                .defineList("fastNodeClasses", java.util.Arrays.asList(
                        "com.refinedmods.refinedstorage.apiimpl.network.node.ImporterNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.ExporterNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.InterfaceNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.FluidInterfaceNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.CrafterNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.ConstructorNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.DestructorNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.diskmanipulator.DiskManipulatorNetworkNode",
                        // Cable Tiers
                        "com.ultramega.cabletiers.node.TieredConstructorNetworkNode",
                        "com.ultramega.cabletiers.node.TieredDestructorNetworkNode",
                        "com.ultramega.cabletiers.node.TieredExporterNetworkNode",
                        "com.ultramega.cabletiers.node.TieredImporterNetworkNode",
                        "com.ultramega.cabletiers.node.TieredInterfaceNetworkNode",
                        "com.ultramega.cabletiers.node.TieredRequesterNetworkNode",
                        "com.ultramega.cabletiers.node.diskmanipulatorz.TieredDiskManipulatorNetworkNode",
                        // Extra Storage
                        "edivad.extrastorage.nodes.AdvancedExporterNetworkNode",
                        "edivad.extrastorage.nodes.AdvancedImporterNetworkNode",
                        "edivad.extrastorage.nodes.AdvancedCrafterNetworkNode",
                        // Requestify
                        "com.buuz135.refinedstoragerequestify.proxy.block.network.NetworkNodeRequester",
                        // Reborn Storage
                        "net.gigabit101.rebornstorage.nodes.CraftingNode",
                        // Refined Crafter Proxy
                        "dev.stevendoesstuffs.refinedcrafterproxy.CrafterProxyNetworkNode"
                ), obj -> obj instanceof String);

        ENABLE_LOAD_RESCAN = builder
                .comment("""
                        On controller load/reload (world load, relog, chunk load), delay then force a full network rescan.
                        Fixes connection issues with capability-based cables (e.g., EnderIO conduits) that aren't detected immediately on load.
                        Safe and recommended for most setups.""")
                .define("enableLoadRescan", true);

        LOAD_RESCAN_DELAY = builder
                .comment("""
                        Ticks to wait after detecting controller load before rescanning.
                        Gives time for block entities and capabilities to initialize. Default 20 (~1 second).
                        Only matters if enableLoadRescan is true.""")
                .defineInRange("loadRescanDelay", 20, 0, 400);

        ENABLE_CONDUIT_PLACEMENT_FIX = builder
                .comment("""
                        When placing EnderIO conduits next to RS blocks, delay then force a network rescan.
                        Fixes runtime detection failures where immediate rescan is too early.
                        Safe even without EnderIO installed.""")
                .define("enableConduitPlacementFix", true);

        CONDUIT_PLACEMENT_RESCAN_DELAY = builder
                .comment("""
                        Ticks to wait after conduit placement before rescanning.
                        Default 10 (~0.5 seconds). Increase if connections still fail occasionally.
                        Only matters if enableConduitPlacementFix is true.""")
                .defineInRange("conduitPlacementRescanDelay", 10, 0, 200);

        ENABLE_LAZY_ENERGY = builder
                .comment("Recalculate network energy usage only when the graph changes instead of every tick. Minor performance improvement.")
                .define("enableLazyEnergy", true);

        ENABLE_HASHSET_OPTIMIZE = builder
                .comment("Replace ConcurrentHashMap/ConcurrentHashSet with regular HashMap/HashSet in single-threaded contexts. Small performance gain.")
                .define("enableHashSetOptimize", true);

        ENABLE_SKIP_UNLOADED = builder
                .comment("Skip processing unloaded positions during network graph updates. Improves performance and prevents rare deadlocks on chunk unload.")
                .define("enableSkipUnloaded", true);

        ENABLE_DYNAMIC_NODE_SLEEP = builder
                .comment("""
                        Makes fast nodes sleep when idle (only relevant when enableBypassFastNodes is true and node is in fastNodeClasses).
                        
                        - ENABLED (default): Nodes update every tick only when work is available.
                          When idle for a few cycles, they fall back to throttled updates → lower overhead.
                        
                        - DISABLED: Nodes always update every tick when bypassing is allowed (full speed even when idle).""")
                .define("enableDynamicNodeSleep", true);

        ENABLE_DYNAMIC_CRAFTING_BYPASS = builder
                .comment("""
                        Enables dynamic throttling bypass for the crafting manager.
                        
                        - ENABLED (default): When there are active crafting tasks, the crafting manager updates every tick → fast crafting while active.
                          Falls back to throttled updates when idle → lower overhead.
                        
                        - DISABLED: Crafting manager strictly follows throttleInterval.
                        
                        Only relevant when enableThrottle is true, throttleInterval > 1, and enableBypassFastNodes is true.
                        Fully respects crafter speed upgrades.""")
                .define("enableDynamicCraftingBypass", true);

        ENABLE_CONNECTED_NODE_TICK_OPTIMIZE = builder
                .comment("Optimizes ticking by only updating nodes that are connected to a network. Disconnected nodes are skipped, reducing unnecessary overhead.")
                .define("enableConnectedNodeTickOptimize", true);

        ENABLE_ENDERIO_RS_FIX = builder
                .comment("""
                        Registers a passthrough factory for EnderIO's RS conduits to suppress warnings and enable compatibility.
                        Disable if EnderIO adds their own registration in a future update.
                        Only applies if EnderIO is loaded.""")
                .define("enableEnderioRsFix", true);

        ENABLE_ENDERIO_CONDUIT_TYPED_BACKUP = builder
                .comment("""
                        Enables saving and applying a type-keyed NBT backup for EnderIO conduit data (filters, upgrades, autocrafting rows, etc.).
                        Prevents loss/corruption on world reload when the global graph restore or positional fallback fails.
                        Safe to disable if not needed, conflicting, or EnderIO fixes this upstream.
                        Default: true (recommended for RS + EnderIO setups).""")
                .define("enableEnderioConduitTypedBackup", true);

        ENABLE_REBORNSTORAGE_CRAFTER_FIX = builder
                .comment("""
                        Fixes RebornStorage multiblock crafter (v5.0.7):
                        - Immediate assembly/GUI access.
                        - Requires CPU or storage (no all-air).
                        - Allows air during construction.
                        - Clean validation messages.
                        - No pattern pages with CPU-only (clear message).
                        Safe to disable if newer versions fix these.""")
                .define("enableRebornstorageCrafterFix", true);

        SPEC = builder.build();
    }
}
