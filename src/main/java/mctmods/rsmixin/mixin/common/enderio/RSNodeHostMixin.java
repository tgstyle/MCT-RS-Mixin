package mctmods.rsmixin.mixin.common.enderio;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import com.enderio.conduits.common.integrations.refinedstorage.RSNetworkNode;
import com.enderio.conduits.common.integrations.refinedstorage.RSNodeHost;
import com.refinedmods.refinedstorage.api.network.node.INetworkNode;
import com.refinedmods.refinedstorage.api.network.node.INetworkNodeManager;
import com.refinedmods.refinedstorage.apiimpl.API;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RSNodeHost.class, remap = false) public abstract class RSNodeHostMixin {
    @Shadow @Final private RSNetworkNode node;
    @Shadow @Final private Level level;
    @Shadow @Final private BlockPos pos;

    @Inject(method = "getNode()Lcom/enderio/conduits/common/integrations/refinedstorage/RSNetworkNode;", at = @At("RETURN")) private void rsmixin$unifyManagerInstance(CallbackInfoReturnable<RSNetworkNode> cir) {
        if (!Config.ENABLE_ENDERIO_NODE_UNIFY.get()) { return; }
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) { return; }

        INetworkNodeManager manager = API.instance().getNetworkNodeManager(serverLevel);
        INetworkNode managerNode = manager.getNode(pos);
        if (managerNode == node) { return; }

        manager.setNode(pos, node);
        manager.markForSaving();
        if (Config.ENABLE_DEBUG_LOGGING.get()) { RSMixin.LOGGER.debug("RSMixin: Unified duplicate RS conduit node instance at {} (replaced {} with live host node)", pos, managerNode == null ? "null" : managerNode.getClass().getSimpleName()); }
    }
}
