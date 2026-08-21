package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import com.raoulvdberge.refinedstorage.tile.TileNode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileNode.class, remap = false) public abstract class TileNodeMixin {
    @Unique private NetworkNode rsmixin$detachedNode;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Shadow public abstract NetworkNode createNode(World world, BlockPos pos);

    @Inject(method = "getNode()Lcom/raoulvdberge/refinedstorage/apiimpl/network/node/NetworkNode;", at = @At("HEAD"), cancellable = true) private void rsmixin$detachedNodeGuard(CallbackInfoReturnable<NetworkNode> cir) {
        if (!Config.enableTemplateNodeGuard) { return; }

        TileNode<?> self = (TileNode<?>) (Object) this;
        World world = self.getWorld();
        if (world != null && (world.isRemote || world instanceof WorldServer)) { return; }

        if (rsmixin$detachedNode == null) {
            rsmixin$detachedNode = createNode(world, self.getPos());
            if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Handed out a detached placeholder node for a {} copy without a server world", self.getClass().getSimpleName()); }
        }
        cir.setReturnValue(rsmixin$detachedNode);
    }
}
