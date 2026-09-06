package mctmods.rsmixin;

import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.RangeInt;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@net.minecraftforge.common.config.Config(modid = RSMixin.MODID, name = "mct_rsmixin") @EventBusSubscriber(modid = RSMixin.MODID) public class Config {
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
            "Defaults cover core RS nodes (importers, exporters, interfaces, crafters, etc.), the RS Requestify requester and the RebornStorage multiblock crafter.",
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
            "com.raoulvdberge.refinedstorage.apiimpl.network.node.diskmanipulator.NetworkNodeDiskManipulator",
            "com.buuz135.refinedstoragerequestify.proxy.block.network.NetworkNodeRequester",
            "me.modmuss50.rebornstorage.tiles.CraftingNode"
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

    @Comment({
            "Stops the fluid grid from destroying fluid when filling a bucket fails.",
            "Vanilla RS takes the fluid out of the network first and only then tries to fill the bucket;",
            "if the fill fails or comes up short (modded fluids the bucket won't take, storage races),",
            "that fluid is simply gone. With this on, whatever the bucket doesn't accept goes back into storage.",
            "Covers the regular, wireless, and portable fluid grids."})
    public static boolean enableFluidExtractionGuard = true;

    @Comment({
            "Fixes the Fluid Interface destroying whole stacks of fluid containers.",
            "Vanilla RS drains one container from the input slot and then replaces the entire stack with",
            "that single empty container - a stack of 64 filled cells or capsules becomes 1 empty one and",
            "the other 63 (plus their fluid) are destroyed. With this on, one container is processed at a",
            "time: the empty goes into the network and the rest of the stack stays in the slot."})
    public static boolean enableFluidInterfaceStackFix = true;

    @Comment({
            "Fixes wrong item counts with external storage on containers that hold more than 64 items per slot",
            "(Storage Drawers and similar). Vanilla RS asks such a slot for items only once, which by Forge's",
            "rules returns at most one vanilla stack, so large slots quietly hand out less than requested.",
            "Also stops extraction from blending different NBT variants from separate slots into one stack,",
            "which could duplicate NBT data when exact mode is off."})
    public static boolean enableOverstackExtractionFix = true;

    @Comment({
            "Stops exporters from destroying items and fluids the destination doesn't accept.",
            "Vanilla RS checks up front whether everything fits, but ignores the result of the actual",
            "transfer - if the destination takes less than promised (filtered slots, machines that changed",
            "state in between), the difference is destroyed. With this on, anything refused is put back",
            "into the network."})
    public static boolean enableExporterVoidGuard = true;

    @Comment({
            "Stops autocrafting from destroying inputs a machine doesn't accept (the 'remainder has been",
            "voided!' console message). Vanilla RS checks each input separately, so for example two fluids",
            "can both pass the check for a machine with a single tank; whatever the machine then refuses is",
            "destroyed. With this on, refused items and fluids are returned to the network instead.",
            "The craft still waits (cancelling it refunds the rest), but nothing is lost."})
    public static boolean enableProcessingVoidGuard = true;

    @Comment({
            "Prevents duplicate autocrafting tasks right after world load and within the same tick.",
            "Vanilla RS restores saved crafting tasks lazily and its duplicate check misses both tasks that",
            "aren't restored yet and tasks scheduled earlier in the same tick, so requesters and exporters",
            "with crafting upgrades can start the same craft twice. With this on, saved tasks are restored",
            "before any new request and pending tasks count toward the duplicate check."})
    public static boolean enableRestoredTaskDedup = true;

    @Comment({
            "Protects the Refined Storage save files (refinedstorage_disks.dat and refinedstorage_nodes.dat).",
            "Vanilla Minecraft overwrites these files in place, so a crash or power loss during a world save",
            "can truncate them - and with them every disk's contents - without any error message.",
            "With this on, they are written to a temporary file first and swapped in atomically, so a",
            "half-finished write can never eat the previous good data."})
    public static boolean enableSafeDataSaving = true;

    @Comment({
            "Prevents crashes when other mods make copies of RS blocks without a world, e.g. schematic and",
            "scan tools that build a preview of a block entity. Asking such a copy for its network node",
            "crashes vanilla RS. With this on, world-less copies get a detached placeholder node instead."})
    public static boolean enableTemplateNodeGuard = true;

    @Comment({
            "Fixes clients being kicked with 'Payload may not be larger than 1048576 bytes' when opening a",
            "grid on very large systems. The full item list is sent in a single packet with a 1 MB limit;",
            "with this on, lists that would exceed it are compressed before sending (NBT-heavy lists shrink",
            "several times over). Smaller lists are sent unchanged, so clients without this mod are only",
            "affected in the cases that would have kicked them anyway."})
    public static boolean enableCompressedGridSync = true;

    @Comment({
            "Fixes being kicked with 'Payload may not be larger than 32767 bytes' when using JEI's",
            "transfer (+) button. Vanilla RS sends every possible variant of every ingredient to the",
            "server, which for ore dictionary recipes in large packs easily exceeds the packet limit.",
            "With this on, the list is trimmed to at most 16 variants per slot (the displayed one first)",
            "and kept under the limit. Also validates the packet on the server side."})
    public static boolean enableJeiTransferLimit = true;

    @Comment({
            "Fixes pattern tooltips and icons rescanning the entire recipe registry over and over.",
            "Vanilla RS caches patterns by exact stack instance, so every GUI refresh or inventory sync",
            "creates a cache miss that walks every recipe in the game, and the old entries pile up forever",
            "(a slow memory leak). With this on, patterns are cached by their actual contents with a",
            "bounded, thread-safe cache."})
    public static boolean enablePatternCacheFix = true;

    @Comment({
            "Fixes Filter items nested inside other Filter items.",
            "Vanilla RS parses a nested filter as if it stood alone: its contents leak into the untabbed",
            "default view (or become their own top-level tab) instead of staying inside the parent filter's",
            "tab, which also scrambles the whitelist/blacklist outcome. With this on, a nested filter's",
            "entries belong to the filter that contains it, keeping their own compare and list settings."})
    public static boolean enableNestedFilterFix = true;

    @Comment({
            "Bounds how much a single autocrafting request may ask for.",
            "Vanilla RS only rejects amounts of zero or less, so a request for up to 2 billion is legal.",
            "The calculation runs one craft at a time on the server thread, so a huge request freezes the",
            "server until RS's calculation timeout (5 seconds minimum) while allocating a step object per",
            "craft. With this on, requests are clamped to maxCraftAmount, and the crafting preview also",
            "rejects non-positive amounts (vanilla lets 0 through and it errors in the preview thread)."})
    public static boolean enableCraftAmountLimit = true;

    @Comment({
            "Largest amount one autocrafting request may ask for.",
            "Only used when enableCraftAmountLimit is enabled. For fluids this is in mB."}) @RangeInt(min = 1)
    public static int maxCraftAmount = 1000000;

    @Comment({
            "Stops shift-clicked patterns from being inserted through the Crafter Manager while it is",
            "inactive (no network or no power). Vanilla accepts them into crafters of the dead network.",
            "Note: patterns shift-clicked into an active Crafter Manager still go to the first crafter",
            "with a free slot, which may be scrolled out of view - the server has no way of knowing what",
            "part of the list a player is looking at."})
    public static boolean enableCrafterManagerInsertGuard = true;

    @Comment({
            "Makes hollow covers placed directly against a Controller behave like they do everywhere else.",
            "Vanilla RS checks only whether a cover exists on the neighboring cable's side facing the",
            "controller, so a hollow cover (which is supposed to let the connection through) cuts the",
            "controller off. With this on, only full covers block the connection there."})
    public static boolean enableHollowCoverConnectionFix = true;

    @SubscribeEvent public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(RSMixin.MODID)) { ConfigManager.sync(RSMixin.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE); }
    }
}
