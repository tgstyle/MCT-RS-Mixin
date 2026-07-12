package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.raoulvdberge.refinedstorage.api.network.INetworkNodeVisitor;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeNetworkTransmitter;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = NetworkNodeNetworkTransmitter.class, remap = false) public abstract class NetworkNodeNetworkTransmitterMixin {
    @Redirect(method = "visit", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/api/network/INetworkNodeVisitor$Operator;apply(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)V", ordinal = 0)) private void rsmixin$blockCrossDimensionLink(INetworkNodeVisitor.Operator instance, World world, BlockPos pos, EnumFacing side) {
        if (Config.enableWirelessDimensionLock) { return; }
        instance.apply(world, pos, side);
    }
}
