package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import com.refinedmods.refinedstorage.blockentity.NetworkNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetworkNodeBlockEntity.class, remap = false) public abstract class NetworkNodeBlockEntityMixin {
    @Shadow private NetworkNode clientNode;

    @Shadow public abstract NetworkNode createNode(Level level, BlockPos pos);

    @Inject(method = "getNode()Lcom/refinedmods/refinedstorage/apiimpl/network/node/NetworkNode;", at = @At("HEAD"), cancellable = true) private void rsmixin$guardLevelLessNode(CallbackInfoReturnable<NetworkNode> cir) {
        if (!Config.ENABLE_TEMPLATE_NODE_GUARD.get()) { return; }
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level != null && (level.isClientSide || level instanceof ServerLevel)) { return; }
        if (clientNode == null) { clientNode = createNode(level, self.getBlockPos()); }
        cir.setReturnValue(clientNode);
    }
}
