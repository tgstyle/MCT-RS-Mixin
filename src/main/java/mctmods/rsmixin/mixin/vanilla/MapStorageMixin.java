package mctmods.rsmixin.mixin.vanilla;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Mixin(MapStorage.class) public abstract class MapStorageMixin {
    @Shadow(aliases = "field_75751_a") @Final private ISaveHandler saveHandler;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Inject(method = "saveData", at = @At("HEAD"), cancellable = true) private void rsmixin$atomicRsSave(WorldSavedData data, CallbackInfo ci) {
        if (!Config.enableSafeDataSaving || saveHandler == null) { return; }
        if (data == null || !data.mapName.startsWith("refinedstorage")) { return; }

        try {
            File file = saveHandler.getMapFileFromName(data.mapName);

            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("data", data.writeToNBT(new NBTTagCompound()));

            File temp = new File(file.getParentFile(), file.getName() + ".rstmp");
            FileOutputStream out = new FileOutputStream(temp);
            CompressedStreamTools.writeCompressed(tag, out);
            out.close();

            try { Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING); }

            if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Atomically saved {}", file.getName()); }
            ci.cancel();
        }
        catch (Exception e) { rsmixin$LOGGER.warn("RSMixin: Atomic save of {} failed, falling back to the vanilla save", data.mapName, e); }
    }
}
