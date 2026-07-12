package mctmods.rsmixin;

import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = RSMixin.MODID, name = RSMixin.NAME, dependencies = "required-after:refinedstorage", acceptableRemoteVersions = "*")
public class RSMixin {
    public static final String MODID = "rsmixin";
    public static final String NAME = "RS Mixin";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        ConfigManager.sync(MODID, Type.INSTANCE);
        LOGGER.info("RSMixin loaded: enableDebugLogging={}, enableThrottle={}, throttleInterval={}, enableBypassFastNodes={}, enableLoadRescan={}, loadRescanDelay={}, enableLazyEnergy={}, enableHashSetOptimize={}, enableSkipUnloaded={}, enableGraphRescanCoalesce={}, enableStorageCacheDebounce={}, enableCraftingRebuildDebounce={}",
                Config.enableDebugLogging,
                Config.enableThrottle,
                Config.throttleInterval,
                Config.enableBypassFastNodes,
                Config.enableLoadRescan,
                Config.loadRescanDelay,
                Config.enableLazyEnergy,
                Config.enableHashSetOptimize,
                Config.enableSkipUnloaded,
                Config.enableGraphRescanCoalesce,
                Config.enableStorageCacheDebounce,
                Config.enableCraftingRebuildDebounce);
    }

}
