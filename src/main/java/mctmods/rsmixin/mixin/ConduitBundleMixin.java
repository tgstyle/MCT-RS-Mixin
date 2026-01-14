package mctmods.rsmixin.mixin;

import com.enderio.api.conduit.ConduitData;
import com.enderio.api.conduit.ConduitType;
import com.enderio.conduits.common.conduit.ConduitBundle;
import com.enderio.conduits.common.conduit.RightClickAction;
import mctmods.rsmixin.ConduitPlacementFix;
import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConduitBundle.class)
public abstract class ConduitBundleMixin {
    @Shadow @Final private BlockPos pos;

    @Inject(method = "addType(Lnet/minecraft/world/level/Level;Lcom/enderio/api/conduit/ConduitType;Lnet/minecraft/world/entity/player/Player;)Lcom/enderio/conduits/common/conduit/RightClickAction;", at = @At("RETURN"), remap = false)
    private <T extends ConduitData<T>> void rsmixin_afterAddType(Level level, ConduitType<T> type, Player player, CallbackInfoReturnable<RightClickAction> cir) {
        if (!Config.ENABLE_CONDUIT_PLACEMENT_FIX.get()) return;

        RightClickAction action = cir.getReturnValue();
        if (action instanceof RightClickAction.Blocked) return;

        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            RSMixin.LOGGER.info("RSMixin: Detected conduit addition at {}", pos);
        }

        ConduitPlacementFix.handleConduitUpdate(serverLevel, pos);
    }
}
