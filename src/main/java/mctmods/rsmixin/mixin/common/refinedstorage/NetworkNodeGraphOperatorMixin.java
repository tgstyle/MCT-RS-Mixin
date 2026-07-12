package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import javax.annotation.Nullable;

@Mixin(targets = "com.raoulvdberge.refinedstorage.apiimpl.network.NetworkNodeGraph$Operator", remap = false) public class NetworkNodeGraphOperatorMixin {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true) private void skipDuringUnload(World world, BlockPos pos, @Nullable EnumFacing side, CallbackInfo ci) {
        if (Config.enableSkipUnloaded) {
            if (!world.isBlockLoaded(pos)) {
                if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Skipping apply() for pos {} during chunk unload (chunk not present)", pos); }
                ci.cancel();
            }
        }
    }
}
