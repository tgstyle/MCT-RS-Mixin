package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.apiimpl.network.grid.handler.FluidGridHandlerPortable;
import com.raoulvdberge.refinedstorage.tile.grid.portable.IPortableGrid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FluidGridHandlerPortable.class, remap = false) public abstract class FluidGridHandlerPortableMixin {
    @Shadow private IPortableGrid portableGrid;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Redirect(method = "onExtract", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/capability/IFluidHandlerItem;fill(Lnet/minecraftforge/fluids/FluidStack;Z)I")) private int rsmixin$fillWithRefund(IFluidHandlerItem handler, FluidStack resource, boolean doFill) {
        if (!Config.enableFluidExtractionGuard) { return handler.fill(resource, doFill); }
        if (resource == null) { return 0; }

        int filled = handler.fill(resource, doFill);
        if (filled < resource.amount && portableGrid.getFluidStorage() != null) {
            portableGrid.getFluidStorage().insert(resource, resource.amount - filled, Action.PERFORM);
            if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Returned {} mB of {} to the portable grid after a failed bucket fill", resource.amount - filled, resource.getFluid().getName()); }
        }
        return filled;
    }
}
