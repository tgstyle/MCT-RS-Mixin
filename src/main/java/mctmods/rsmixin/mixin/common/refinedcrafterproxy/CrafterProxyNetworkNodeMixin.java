package mctmods.rsmixin.mixin.common.refinedcrafterproxy;

import com.refinedmods.refinedstorage.api.network.node.INetworkNodeManager;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import dev.stevendoesstuffs.refinedcrafterproxy.crafterproxy.CrafterProxyNetworkNode;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.ActiveFastNodesAccessor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(CrafterProxyNetworkNode.class)
public abstract class CrafterProxyNetworkNodeMixin extends NetworkNode {

    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Unique private boolean rsmixin$forcedActive = false;

    protected CrafterProxyNetworkNodeMixin(Level level, net.minecraft.core.BlockPos pos) {
        super(level, pos);
    }

    @Inject(method = "update", at = @At("HEAD"), remap = false)
    private void ensureFastTick(CallbackInfo ci) {
        if (!(level instanceof ServerLevel)) {
            return;
        }

        INetworkNodeManager manager = API.instance().getNetworkNodeManager((ServerLevel) level);
        ActiveFastNodesAccessor accessor = (ActiveFastNodesAccessor) manager;

        if (Config.ENABLE_BYPASS_FAST_NODES.get()) {
            if (!rsmixin$forcedActive) {
                accessor.rsmixin$addActiveFastNode(this);
                rsmixin$forcedActive = true;
                if (Config.ENABLE_DEBUG_LOGGING.get()) {
                    rsmixin$LOGGER.debug("Forcing Crafter Proxy at {} into fast nodes (permanent for responsiveness)", pos);
                }
            }
        } else {
            if (rsmixin$forcedActive) {
                accessor.rsmixin$removeActiveFastNode(this);
                rsmixin$forcedActive = false;
            }
        }
    }
}
