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
    public static final ForgeConfigSpec.BooleanValue ENABLE_STORAGE_CACHE_DEBOUNCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CRAFTING_REBUILD_DEBOUNCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GRAPH_RESCAN_COALESCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENDERIO_NODE_UNIFY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TRACKED_INSERT_INDEX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DAMAGEABLE_INPUT_REUSE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CRAFTING_CRASH_GUARD;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GRID_RESYNC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SAFE_DATA_SAVING;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WIRELESS_DIMENSION_LOCK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MODEL_REGISTRATION_FIX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FLUID_EXTRACTION_GUARD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push(RSMixin.MODID);

        ENABLE_DEBUG_LOGGING = builder
                .comment("Turns on extra log messages for troubleshooting. Can get spammy, leave off unless you're diagnosing a problem.")
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

        ENABLE_STORAGE_CACHE_DEBOUNCE = builder
                .comment("""
                        Stops your item/fluid list from rebuilding itself over and over when things reconnect.
                        Normally, every storage block on your network can trigger a full list rebuild at the same time,
                        which causes lag spikes when your world loads or when you place/break a lot of storage at once.
                        This makes it rebuild the list just once instead.""")
                .define("enableStorageCacheDebounce", true);

        ENABLE_CRAFTING_REBUILD_DEBOUNCE = builder
                .comment("""
                        Stops autocrafting patterns from being re-scanned over and over when crafters reconnect.
                        Normally, every crafter reconnecting (like on world load) can trigger its own full pattern re-scan,
                        which gets very slow with lots of crafters. This waits until everything has reconnected, then scans once.
                        Works for regular RS crafters, Extra Storage crafters, and RebornStorage crafters.""")
                .define("enableCraftingRebuildDebounce", true);

        ENABLE_GRAPH_RESCAN_COALESCE = builder
                .comment("""
                        Stops network rescans from piling up when you place or break lots of network blocks at once.
                        The first block still connects instantly, just like normal.
                        Any extra rescans triggered in that same instant are merged into one, run a moment later.
                        Great for builders, schematics, or quarries that place many cables/blocks in one go.
                        Worst case, a block takes one extra tick to connect (not noticeable in normal play).""")
                .define("enableGraphRescanCoalesce", true);

        ENABLE_ENDERIO_NODE_UNIFY = builder
                .comment("""
                        Fixes a bug where EnderIO's RS conduits can end up tracked as two different objects at once
                        after a world reload, causing weird behavior like settings not sticking or conduits acting laggy/broken.
                        Makes sure there's only ever one real conduit object being tracked.""")
                .define("enableEnderIONodeUnify", true);

        ENABLE_TRACKED_INSERT_INDEX = builder
                .comment("""
                        Speeds up autocrafting for big crafting trees.
                        Normally, every item that comes back into a crafting job gets checked against every single step
                        of that job to see where it belongs, which gets slow with big/multi-step crafts.
                        This looks it up directly instead, so it only checks the steps that actually need that item.""")
                .define("enableTrackedInsertIndex", true);

        ENABLE_DAMAGEABLE_INPUT_REUSE = builder
                .comment("""
                Lets autocrafting properly re-use damageable tools (hammers, saws, etc.) in recipes.
                Normally RS treats a damaged tool as a completely different item, so it grabs or crafts
                a brand new tool every single time and the damaged ones pile up unused in storage.
                With this on, damaged tools count as valid ingredients (most-damaged used first),
                a single tool is re-used across an entire batch craft until its durability runs out
                (only crafting a replacement when one actually breaks), and broken tools are properly
                consumed. The actual durability cost of each recipe is measured from the recipe itself,
                and recipes with random durability loss are estimated conservatively.
                Enchanted tools are never mixed up with plain ones.""")
                .define("enableDamageableInputReuse", true);

        ENABLE_CRAFTING_CRASH_GUARD = builder
                .comment("""
                Stops a broken autocrafting task from crashing the whole server.
                Vanilla RS can hit an internal inventory mismatch during processing crafts and hard-crashes
                the server, and because the broken task is saved with the world, it crashes again on every reboot.
                With this on, the broken task is safely cancelled instead: its items are refunded to storage,
                an error is written to the log, and the server keeps running.
                Fixes Refined Storage issues #3751 and #3727, and the reboot crash loops behind #3755 and #3753.""")
                .define("enableCraftingCrashGuard", true);

        ENABLE_GRID_RESYNC = builder
                .comment("""
                Fixes grids randomly showing missing/wrong/empty item lists until reopened (RS bug #3693).
                Whenever the network rebuilds its item list (any block placed/broken on the network, chunks
                loading, etc.) while a grid is open, vanilla RS forgets to tell the player's screen about it,
                leaving it permanently out of sync until the grid is reopened.
                With this on, open grids are refreshed automatically whenever that happens.""")
                .define("enableGridResync", true);

        ENABLE_SAFE_DATA_SAVING = builder
                .comment("""
                Fixes disks/storage randomly wiping to 0/0 after a server restart (RS bugs #3740, #3714).
                Vanilla RS saves its disk data by deleting the real file and then renaming a temp file over it -
                if that rename fails (common on Windows with antivirus or backup software), your disk data file
                is destroyed and everything shows as empty on next boot.
                With this on, saves are atomic (the old file is never deleted first), failures are retried on the
                next autosave instead of silently dropped, and a clear error is logged.
                Tip: if you were already hit by this bug, look for a refinedstorage_disks.dat.temp file in your
                world's data folder - renaming it to refinedstorage_disks.dat usually recovers everything.""")
                .define("enableSafeDataSaving", true);

        ENABLE_WIRELESS_DIMENSION_LOCK = builder
                .comment("""
                When enabled, wireless items (Wireless Grid, Fluid Grid, Crafting Monitor, and addon wireless items)
                can only be used in the same dimension as the network they are linked to.
                Trying to use one from another dimension shows an on-screen message instead of opening,
                even if an addon's infinite/cross-dimension wireless transmitter would normally allow it.
                Disabled by default (vanilla behavior).""")
                .define("enableWirelessDimensionLock", false);

        ENABLE_MODEL_REGISTRATION_FIX = builder
                .comment("""
                Fixes covers crafting up purple/black and turning invisible when placed, and patterns
                not previewing their output while Shift is held (RS bug #3747).
                RS registers its custom cover and pattern models during mod setup, which the game runs
                at the same time as its first model bake. If the bake finishes first (more likely on
                slower PCs), the custom models are never swapped in and stay broken for the whole session.
                With this on, the models are registered right before every bake, so the race can't be lost.
                If a bake fires before this config file has loaded, the fix is applied regardless and this
                setting takes effect from the next bake onward.""")
                .define("enableModelRegistrationFix", true);

        ENABLE_FLUID_EXTRACTION_GUARD = builder
                .comment("""
                Stops the fluid grid from destroying fluids that can't go into a bucket (RS bugs #3732, #3169).
                Vanilla RS takes the fluid out of the network first and only then tries to fill the bucket -
                if the fluid has no bucket form (Create potions, many modded fluids), the fill silently fails
                and the fluid is gone, leaving the player holding an empty bucket.
                With this on, the extraction is checked first: if a bucket can't actually hold that fluid,
                or the network can't supply a full bucket's worth, the click simply does nothing and
                no fluid or bucket is lost.""")
                .define("enableFluidExtractionGuard", true);

        SPEC = builder.build();
    }
}
