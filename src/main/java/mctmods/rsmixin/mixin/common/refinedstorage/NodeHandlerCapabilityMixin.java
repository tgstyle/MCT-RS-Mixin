package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.blockentity.CrafterBlockEntity;
import com.refinedmods.refinedstorage.blockentity.DiskDriveBlockEntity;
import com.refinedmods.refinedstorage.blockentity.DiskManipulatorBlockEntity;
import com.refinedmods.refinedstorage.blockentity.FluidInterfaceBlockEntity;
import com.refinedmods.refinedstorage.blockentity.InterfaceBlockEntity;
import com.refinedmods.refinedstorage.blockentity.NetworkTransmitterBlockEntity;
import com.refinedmods.refinedstorage.blockentity.grid.GridBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {CrafterBlockEntity.class, DiskDriveBlockEntity.class, DiskManipulatorBlockEntity.class, FluidInterfaceBlockEntity.class, GridBlockEntity.class, InterfaceBlockEntity.class, NetworkTransmitterBlockEntity.class}, remap = false) public abstract class NodeHandlerCapabilityMixin {
    @Inject(method = "getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;", at = @At("HEAD"), cancellable = true) private <T> void rsmixin$skipLevelLessHandler(Capability<T> cap, Direction direction, CallbackInfoReturnable<LazyOptional<T>> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (!Config.ENABLE_TEMPLATE_NODE_GUARD.get() || self.getLevel() != null) { return; }
        if (cap == ForgeCapabilities.ITEM_HANDLER || cap == ForgeCapabilities.FLUID_HANDLER) { cir.setReturnValue(LazyOptional.empty()); }
    }
}
