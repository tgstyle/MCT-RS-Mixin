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

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        ENABLE_DEBUG_LOGGING = builder
                .comment("Enable debug logging for unloaded position skips and throttle ticks")
                .define("enableDebugLogging", false);

        ENABLE_THROTTLE = builder
                .comment("Enable throttling of network updates")
                .define("enableThrottle", true);

        THROTTLE_INTERVAL = builder
                .comment("Throttle interval in ticks (1 = no throttle, 20 = every second)")
                .defineInRange("throttleInterval", 20, 1, 1000);

        ENABLE_BYPASS_FAST_NODES = builder
                .comment("When throttling is enabled, still update import/export buses and interfaces every tick (if disabled it will only do an operation per the throttleInterval value, i.e. 1 in 20 by default)")
                .define("enableBypassFastNodes", true);

        FAST_NODE_CLASSES = builder
                .comment("List of full class names for network nodes that should bypass throttling and update every tick. Defaults include core RS nodes and common addons like Cable Tiers, Extra Storage, Requestify, Reborn Storage, and Refined Crafter Proxy. Adding classes from uninstalled mods causes no issues, as it's just string matching.")
                .defineList("fastNodeClasses", java.util.Arrays.asList(
                        "com.refinedmods.refinedstorage.apiimpl.network.node.ImporterNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.ExporterNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.InterfaceNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.FluidInterfaceNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.CrafterNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.ConstructorNetworkNode",
                        "com.refinedmods.refinedstorage.apiimpl.network.node.DestructorNetworkNode",
                        // Cable Tiers
                        "com.ultramega.cabletiers.node.TieredConstructorNetworkNode",
                        "com.ultramega.cabletiers.node.TieredDestructorNetworkNode",
                        "com.ultramega.cabletiers.node.TieredExporterNetworkNode",
                        "com.ultramega.cabletiers.node.TieredImporterNetworkNode",
                        "com.ultramega.cabletiers.node.TieredInterfaceNetworkNode",
                        "com.ultramega.cabletiers.node.TieredRequesterNetworkNode",
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
                .comment("When the controller becomes loaded (world load, relog, chunk load, etc.), delay then force a full network graph rescan. Fixes EnderIO RS conduits and other capability-based cables not detecting properly on load—even in single-chunk networks—without manual break/replace.")
                .define("enableLoadRescan", true);

        LOAD_RESCAN_DELAY = builder
                .comment("Ticks to wait after detecting load before rescanning (gives time for block entities/capabilities to initialize). Default 20 (~1 second at 20 TPS). Set 0 for immediate.")
                .defineInRange("loadRescanDelay", 20, 0, 400);

        ENABLE_CONDUIT_PLACEMENT_FIX = builder
                .comment("Delay and force network graph rescan when placing EnderIO conduits adjacent to RS blocks. Fixes runtime placement detection failures for EnderIO RS conduits where immediate rescan was too early (capability not yet registered). Safe even if EnderIO not installed.")
                .define("enableConduitPlacementFix", true);

        CONDUIT_PLACEMENT_RESCAN_DELAY = builder
                .comment("Ticks to wait after conduit placement before rescanning (allows EnderIO capability registration). Default 10 (~0.5 seconds). Increase if detection still fails in some cases.")
                .defineInRange("conduitPlacementRescanDelay", 10, 0, 200);

        ENABLE_LAZY_ENERGY = builder
                .comment("Enable lazy recalculation of energy usage (only on graph changes)")
                .define("enableLazyEnergy", true);

        ENABLE_HASHSET_OPTIMIZE = builder
                .comment("Enable switching concurrent sets to regular HashSets in single-threaded contexts")
                .define("enableHashSetOptimize", true);

        ENABLE_SKIP_UNLOADED = builder
                .comment("Skip network graph processing entirely for unloaded positions (performance improvement plus fixes deadlock on chunk unload)")
                .define("enableSkipUnloaded", true);

        SPEC = builder.build();
    }
}
