package mctmods.rsmixin;

import mctmods.rsmixin.helper.ConduitPlacementFix;
import mctmods.rsmixin.helper.FastNodeTicker;

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

        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
            System.err.println("Uncaught exception in thread " + thread.getName() + ":");
            t.printStackTrace(System.err);
        });
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Loaded config: enableDebugLogging={}, enableThrottle={}, throttleInterval={}, enableBypassFastNodes={}, enableLoadRescan={}, loadRescanDelay={}, enableConduitPlacementFix={}, conduitPlacementRescanDelay={}, enableLazyEnergy={}, enableHashSetOptimize={}, enableSkipUnloaded={}, enableDynamicImporterSleep={}",
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
                Config.ENABLE_DYNAMIC_IMPORTER_SLEEP.get());
    }
}
