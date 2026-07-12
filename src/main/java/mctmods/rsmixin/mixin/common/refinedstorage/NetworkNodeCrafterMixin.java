package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.accessor.IActiveFastNodesAccessor;

import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeManager;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeCrafter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkNodeCrafter.class, remap = false) public abstract class NetworkNodeCrafterMixin {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);
    @Unique private boolean rsmixin$forcedActive = false;

    @Inject(method = "update", at = @At("HEAD")) private void ensureFastTick(CallbackInfo ci) {
        NetworkNodeCrafter thiz = (NetworkNodeCrafter) (Object) this;
        if (thiz.getWorld() == null || thiz.getWorld().isRemote) { return; }

        INetworkNodeManager manager = API.instance().getNetworkNodeManager(thiz.getWorld());
        IActiveFastNodesAccessor accessor = (IActiveFastNodesAccessor) manager;

        if (Config.enableBypassFastNodes) {
            if (!rsmixin$forcedActive) {
                accessor.rsmixin$addActiveFastNode(thiz);
                rsmixin$forcedActive = true;
                if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("Forcing core Crafter at {} into fast nodes (permanent for redstone responsiveness)", thiz.getPos()); }
            }
        }
        else {
            if (rsmixin$forcedActive) {
                accessor.rsmixin$removeActiveFastNode(thiz);
                rsmixin$forcedActive = false;
            }
        }
    }
}
