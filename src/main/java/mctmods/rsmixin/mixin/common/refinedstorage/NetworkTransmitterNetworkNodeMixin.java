package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.api.network.INetworkNodeVisitor;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkTransmitterNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = NetworkTransmitterNetworkNode.class, remap = false) public abstract class NetworkTransmitterNetworkNodeMixin {
    @Redirect(method = "visit", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/api/network/INetworkNodeVisitor$Operator;apply(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V", ordinal = 0)) private void rsmixin$blockCrossDimensionLink(INetworkNodeVisitor.Operator operator, Level dimensionLevel, BlockPos receiverPos, Direction side) {
        if (Config.ENABLE_WIRELESS_DIMENSION_LOCK.get()) { return; }
        operator.apply(dimensionLevel, receiverPos, side);
    }
}
