package mctmods.rsmixin.mixin.common.enderio;

import com.enderio.api.conduit.ConduitData;
import com.enderio.api.conduit.ConduitType;
import com.enderio.conduits.common.conduit.ConduitBundle;
import com.enderio.conduits.common.conduit.RightClickAction;

import mctmods.rsmixin.helper.enderio.ConduitPlacementFix;
import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;

import net.minecraftforge.registries.ForgeRegistries;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConduitBundle.class)
public abstract class ConduitBundleMixin {
    @Shadow @Final private BlockPos pos;

    @Unique
    private static final ResourceLocation RS_CONDUIT_ITEM_ID = ResourceLocation.fromNamespaceAndPath("enderio", "refined_storage_conduit");

    @Inject(method = "addType(Lnet/minecraft/world/level/Level;Lcom/enderio/api/conduit/ConduitType;Lnet/minecraft/world/entity/player/Player;)Lcom/enderio/conduits/common/conduit/RightClickAction;", at = @At("RETURN"), remap = false)
    private <T extends ConduitData<T>> void rsmixin_afterAddType(Level level, ConduitType<T> type, Player player, CallbackInfoReturnable<RightClickAction> cir) {
        if (!Config.ENABLE_CONDUIT_PLACEMENT_FIX.get()) return;

        RightClickAction action = cir.getReturnValue();
        if (action instanceof RightClickAction.Blocked) return;

        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            RSMixin.LOGGER.debug("RSMixin: Detected conduit addition at {}", pos);
        }

        ConduitPlacementFix.handleConduitUpdate(serverLevel, pos);
    }

    @Inject(method = "removeType(Lnet/minecraft/world/level/Level;Lcom/enderio/api/conduit/ConduitType;)Z", at = @At("RETURN"), remap = false)
    private void rsmixin_afterRemoveType(Level level, ConduitType<?> type, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.ENABLE_CONDUIT_PLACEMENT_FIX.get()) return;
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!cir.getReturnValue()) return;

        ItemLike conduitItem = type.getConduitItem();

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(conduitItem.asItem());
        if (!RS_CONDUIT_ITEM_ID.equals(itemId)) return;

        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            RSMixin.LOGGER.debug("RSMixin: Detected RS conduit type removal at {}", pos);
        }

        ConduitPlacementFix.handleConduitUpdate(serverLevel, pos);
    }
}
