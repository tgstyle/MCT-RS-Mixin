package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.IActiveFastNodesAccessor;
import mctmods.rsmixin.core.accessor.IConnectedNodesAccessor;
import mctmods.rsmixin.core.accessor.IEnergyDirtyAccessor;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeManager;
import com.refinedmods.refinedstorage.apiimpl.network.node.ConnectivityStateChangeCause;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkNode.class) public abstract class NetworkNodeMixin {
    @Inject(method = "onConnected", at = @At("TAIL"), remap = false) private void onConnectedInject(INetwork network, CallbackInfo ci) {
        NetworkNode thiz = (NetworkNode) (Object) this;
        NetworkNodeManager manager = (NetworkNodeManager) API.instance().getNetworkNodeManager((ServerLevel) thiz.getLevel());
        if (Config.ENABLE_CONNECTED_NODE_TICK_OPTIMIZE.get()) { ((IConnectedNodesAccessor) manager).rsmixin$addConnectedNode(thiz); }
        if (Config.ENABLE_BYPASS_FAST_NODES.get() && Config.FAST_NODE_CLASSES.get().contains(thiz.getClass().getName())) { ((IActiveFastNodesAccessor) manager).rsmixin$addActiveFastNode(thiz); }
    }

    @Inject(method = "onDisconnected", at = @At("HEAD"), remap = false) private void onDisconnectedInject(INetwork network, CallbackInfo ci) {
        NetworkNode thiz = (NetworkNode) (Object) this;
        NetworkNodeManager manager = (NetworkNodeManager) API.instance().getNetworkNodeManager((ServerLevel) thiz.getLevel());
        ((IConnectedNodesAccessor) manager).rsmixin$removeConnectedNode(thiz);
        ((IActiveFastNodesAccessor) manager).rsmixin$removeActiveFastNode(thiz);
    }

    @Inject(method = "onConnectedStateChange", at = @At("TAIL"), remap = false) private void onStateChangeInject(INetwork network, boolean state, ConnectivityStateChangeCause cause, CallbackInfo ci) {
        if (network instanceof IEnergyDirtyAccessor accessor) { accessor.rsmixin$markEnergyDirty(); }
    }
}
