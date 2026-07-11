package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.apiimpl.util.RSSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.File;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(RSSavedData.class) public abstract class RSSavedDataMixin {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Shadow public abstract CompoundTag save(CompoundTag compound);

    @Inject(method = "save(Ljava/io/File;)V", at = @At("HEAD"), cancellable = true) private void rsmixin$atomicSave(File file, CallbackInfo ci) {
        if (!Config.ENABLE_SAFE_DATA_SAVING.get()) { return; }
        ci.cancel();

        SavedData self = (SavedData) (Object) this;
        if (!self.isDirty()) { return; }

        CompoundTag tag = new CompoundTag();
        tag.put("data", this.save(new CompoundTag()));
        NbtUtils.addCurrentDataVersion(tag);

        Path target = file.toPath();
        Path temp = target.getParent().resolve(file.getName() + ".temp");

        try {
            NbtIo.writeCompressed(tag, temp.toFile());
            try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            self.setDirty(false);
        } catch (Exception e) {
            rsmixin$LOGGER.error("RSMixin: Failed to save {} atomically, retrying with direct write. The previous file is untouched.", file.getName(), e);
            try {
                NbtIo.writeCompressed(tag, file);
                self.setDirty(false);
            } catch (Exception e2) {
                rsmixin$LOGGER.error("RSMixin: Direct write of {} also failed. Data is kept in memory and will be retried on the next save. If the server stops before a save succeeds, recover from {}", file.getName(), temp.getFileName(), e2);
            }
        }
    }
}
