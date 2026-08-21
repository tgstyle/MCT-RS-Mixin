package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.apiimpl.network.grid.handler.FluidGridHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidGridHandler.class, remap = false) public abstract class FluidGridHandlerMixin {
    @Shadow private INetwork network;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Redirect(method = "onExtract", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/capability/IFluidHandlerItem;fill(Lnet/minecraftforge/fluids/FluidStack;Z)I")) private int rsmixin$fillWithRefund(IFluidHandlerItem handler, FluidStack resource, boolean doFill) {
        if (!Config.enableFluidExtractionGuard) { return handler.fill(resource, doFill); }
        if (resource == null) { return 0; }

        int filled = handler.fill(resource, doFill);
        if (filled < resource.amount) {
            network.insertFluid(resource, resource.amount - filled, Action.PERFORM);
            if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Returned {} mB of {} to the network after a failed bucket fill", resource.amount - filled, resource.getFluid().getName()); }
        }
        return filled;
    }

    @Inject(method = "onCraftingPreviewRequested", at = @At("HEAD"), cancellable = true) private void rsmixin$rejectInvalidPreviewAmount(EntityPlayerMP player, int hash, int quantity, boolean noPreview, CallbackInfo ci) {
        if (Config.enableCraftAmountLimit && quantity <= 0) { ci.cancel(); }
    }

    @ModifyVariable(method = "onCraftingPreviewRequested", at = @At("HEAD"), argsOnly = true, ordinal = 1) private int rsmixin$clampPreviewAmount(int quantity) { return Config.enableCraftAmountLimit ? Math.min(quantity, Config.maxCraftAmount) : quantity; }

    @ModifyVariable(method = "onCraftingRequested", at = @At("HEAD"), argsOnly = true, ordinal = 1) private int rsmixin$clampCraftAmount(int quantity) { return Config.enableCraftAmountLimit ? Math.min(quantity, Config.maxCraftAmount) : quantity; }
}
