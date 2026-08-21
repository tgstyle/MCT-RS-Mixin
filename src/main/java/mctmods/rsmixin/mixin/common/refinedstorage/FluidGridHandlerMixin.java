package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.apiimpl.network.grid.handler.FluidGridHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(value = FluidGridHandler.class, remap = false) public abstract class FluidGridHandlerMixin {
    @Shadow @Final private INetwork network;

    @Inject(method = "onExtract", at = @At("HEAD"), cancellable = true) private void rsmixin$guardUnfillableFluid(ServerPlayer player, UUID id, boolean shift, CallbackInfo ci) {
        if (!Config.ENABLE_FLUID_EXTRACTION_GUARD.get()) { return; }
        FluidStack stack = network.getFluidStorageCache().getList().get(id);
        if (stack == null) { return; }
        FluidStack simulated = network.extractFluid(stack, FluidType.BUCKET_VOLUME, Action.SIMULATE);
        if (simulated.isEmpty() || simulated.getAmount() < FluidType.BUCKET_VOLUME) { ci.cancel(); return; }
        int filled = new ItemStack(Items.BUCKET).getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().map(handler -> handler.fill(simulated, IFluidHandler.FluidAction.SIMULATE)).orElse(0);
        if (filled < FluidType.BUCKET_VOLUME) { ci.cancel(); }
    }
}
