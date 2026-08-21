package mctmods.rsmixin;

import mctmods.rsmixin.helper.enderio.ConduitPlacementFix;
import mctmods.rsmixin.helper.refinedstorage.ChunkNodeDiscovery;
import mctmods.rsmixin.helper.refinedstorage.CraftingTicker;
import mctmods.rsmixin.helper.refinedstorage.FastNodeTicker;

import mctmods.rsmixin.helper.refinedstorage.GraphRescanScheduler;
import mctmods.rsmixin.helper.refinedstorage.SaveDataRecovery;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("removal")
@Mod("rsmixin")
public class RSMixin {
    public static final String MODID = "rsmixin";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public RSMixin() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        MinecraftForge.EVENT_BUS.register(new FastNodeTicker());
        MinecraftForge.EVENT_BUS.register(new ConduitPlacementFix());
        MinecraftForge.EVENT_BUS.register(CraftingTicker.class);
        MinecraftForge.EVENT_BUS.register(GraphRescanScheduler.class);
        MinecraftForge.EVENT_BUS.register(ChunkNodeDiscovery.class);
        MinecraftForge.EVENT_BUS.register(SaveDataRecovery.class);

        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
            System.err.println("Uncaught exception in thread " + thread.getName() + ":");
            t.printStackTrace(System.err);
        });
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Loaded config: enableDebugLogging={}, enableThrottle={}, throttleInterval={}, enableBypassFastNodes={}, enableLoadRescan={}, loadRescanDelay={}, enableConduitPlacementFix={}, conduitPlacementRescanDelay={}, enableLazyEnergy={}, enableHashSetOptimize={}, enableSkipUnloaded={}, enableDynamicNodeSleep={}, enableDynamicCraftingBypass={}, enableConnectedNodeTickOptimize={}, enableEnderioRsFix={}, enableEnderioConduitTypedBackup={}, enableRebornstorageCrafterFix={}, enableStorageCacheDebounce={}, enableCraftingRebuildDebounce={}, enableGraphRescanCoalesce={}, enableEnderIONodeUnify={}, enableTrackedInsertIndex={}, enableDamageableInputReuse={}, enableCraftingCrashGuard={}, enableGridResync={}, enableSafeDataSaving={}, enableWirelessDimensionLock={}, enableModelRegistrationFix={}, enableFluidExtractionGuard={}, enableRaisedPacketLimit={}, maxSplitPackets={}, enablePatternCacheThreadSafety={}, enableOverstackExtractionFix={}, enableChunkLoadDiscovery={}, enableRestoredTaskDedup={}, enableJeiTransferLimit={}, enablePatternRenderCache={}, enableTemplateNodeGuard={}, enableJeiTrackerRefresh={}, enableExporterVoidGuard={}, enableProcessingVoidGuard={}, enableCoverParticleFix={}, enableHotkeyModifierFix={}",
                Config.ENABLE_DEBUG_LOGGING.get(),
                Config.ENABLE_THROTTLE.get(),
                Config.THROTTLE_INTERVAL.get(),
                Config.ENABLE_BYPASS_FAST_NODES.get(),
                Config.ENABLE_LOAD_RESCAN.get(),
                Config.LOAD_RESCAN_DELAY.get(),
                Config.ENABLE_CONDUIT_PLACEMENT_FIX.get(),
                Config.CONDUIT_PLACEMENT_RESCAN_DELAY.get(),
                Config.ENABLE_LAZY_ENERGY.get(),
                Config.ENABLE_HASHSET_OPTIMIZE.get(),
                Config.ENABLE_SKIP_UNLOADED.get(),
                Config.ENABLE_DYNAMIC_NODE_SLEEP.get(),
                Config.ENABLE_DYNAMIC_CRAFTING_BYPASS.get(),
                Config.ENABLE_CONNECTED_NODE_TICK_OPTIMIZE.get(),
                Config.ENABLE_ENDERIO_RS_FIX.get(),
                Config.ENABLE_ENDERIO_CONDUIT_TYPED_BACKUP.get(),
                Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get(),
                Config.ENABLE_STORAGE_CACHE_DEBOUNCE.get(),
                Config.ENABLE_CRAFTING_REBUILD_DEBOUNCE.get(),
                Config.ENABLE_GRAPH_RESCAN_COALESCE.get(),
                Config.ENABLE_ENDERIO_NODE_UNIFY.get(),
                Config.ENABLE_TRACKED_INSERT_INDEX.get(),
                Config.ENABLE_DAMAGEABLE_INPUT_REUSE.get(),
                Config.ENABLE_CRAFTING_CRASH_GUARD.get(),
                Config.ENABLE_GRID_RESYNC.get(),
                Config.ENABLE_SAFE_DATA_SAVING.get(),
                Config.ENABLE_WIRELESS_DIMENSION_LOCK.get(),
                Config.ENABLE_MODEL_REGISTRATION_FIX.get(),
                Config.ENABLE_FLUID_EXTRACTION_GUARD.get(),
                Config.ENABLE_RAISED_PACKET_LIMIT.get(),
                Config.MAX_SPLIT_PACKETS.get(),
                Config.ENABLE_PATTERN_CACHE_THREAD_SAFETY.get(),
                Config.ENABLE_OVERSTACK_EXTRACTION_FIX.get(),
                Config.ENABLE_CHUNK_LOAD_DISCOVERY.get(),
                Config.ENABLE_RESTORED_TASK_DEDUP.get(),
                Config.ENABLE_JEI_TRANSFER_LIMIT.get(),
                Config.ENABLE_PATTERN_RENDER_CACHE.get(),
                Config.ENABLE_TEMPLATE_NODE_GUARD.get(),
                Config.ENABLE_JEI_TRACKER_REFRESH.get(),
                Config.ENABLE_EXPORTER_VOID_GUARD.get(),
                Config.ENABLE_PROCESSING_VOID_GUARD.get(),
                Config.ENABLE_COVER_PARTICLE_FIX.get(),
                Config.ENABLE_HOTKEY_MODIFIER_FIX.get());
    }
}
