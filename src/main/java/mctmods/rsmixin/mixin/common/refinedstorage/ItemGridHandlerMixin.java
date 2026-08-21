package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.raoulvdberge.refinedstorage.apiimpl.network.grid.handler.ItemGridHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemGridHandler.class, remap = false) public abstract class ItemGridHandlerMixin {
    @Inject(method = "onCraftingPreviewRequested", at = @At("HEAD"), cancellable = true) private void rsmixin$rejectInvalidPreviewAmount(EntityPlayerMP player, int hash, int quantity, boolean noPreview, CallbackInfo ci) {
        if (Config.enableCraftAmountLimit && quantity <= 0) { ci.cancel(); }
    }

    @ModifyVariable(method = "onCraftingPreviewRequested", at = @At("HEAD"), argsOnly = true, ordinal = 1) private int rsmixin$clampPreviewAmount(int quantity) { return Config.enableCraftAmountLimit ? Math.min(quantity, Config.maxCraftAmount) : quantity; }

    @ModifyVariable(method = "onCraftingRequested", at = @At("HEAD"), argsOnly = true, ordinal = 1) private int rsmixin$clampCraftAmount(int quantity) { return Config.enableCraftAmountLimit ? Math.min(quantity, Config.maxCraftAmount) : quantity; }
}
