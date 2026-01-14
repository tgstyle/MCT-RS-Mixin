package mctmods.rsmixin.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(targets = "com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeGraph$Operator", remap = false)
public class NetworkNodeGraphOperatorMixin {
    @Unique
    private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void skipDuringUnload(Level level, BlockPos pos, @Nullable Direction side, CallbackInfo ci) {
        if (mctmods.rsmixin.Config.ENABLE_SKIP_UNLOADED.get()) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            if (!level.hasChunk(chunkX, chunkZ)) {
                if (mctmods.rsmixin.Config.ENABLE_DEBUG_LOGGING.get()) {
                    rsmixin$LOGGER.debug("RSMixin: Skipping apply() for pos {} during chunk unload (chunk not present)", pos);
                }
                ci.cancel();
            }
        }
    }
}
