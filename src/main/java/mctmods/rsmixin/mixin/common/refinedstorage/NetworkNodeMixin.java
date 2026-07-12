package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.IActiveFastNodesAccessor;
import mctmods.rsmixin.core.accessor.IConnectedNodesAccessor;
import mctmods.rsmixin.core.accessor.IEnergyDirtyAccessor;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.network.NetworkNodeManager;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

@Mixin(value = NetworkNode.class, remap = false) public abstract class NetworkNodeMixin {
    @Inject(method = "onConnected", at = @At("TAIL")) private void onConnectedInject(INetwork network, CallbackInfo ci) {
        NetworkNode thiz = (NetworkNode) (Object) this;
        if (thiz.getWorld() == null || thiz.getWorld().isRemote) { return; }
        NetworkNodeManager manager = (NetworkNodeManager) API.instance().getNetworkNodeManager(thiz.getWorld());
        if (Config.enableConnectedNodeTickOptimize) { ((IConnectedNodesAccessor) manager).rsmixin$addConnectedNode(thiz); }
        if (Config.enableBypassFastNodes && Arrays.asList(Config.fastNodeClasses).contains(thiz.getClass().getName())) { ((IActiveFastNodesAccessor) manager).rsmixin$addActiveFastNode(thiz); }
    }

    @Inject(method = "onDisconnected", at = @At("HEAD")) private void onDisconnectedInject(INetwork network, CallbackInfo ci) {
        NetworkNode thiz = (NetworkNode) (Object) this;
        if (thiz.getWorld() == null || thiz.getWorld().isRemote) { return; }
        NetworkNodeManager manager = (NetworkNodeManager) API.instance().getNetworkNodeManager(thiz.getWorld());
        ((IConnectedNodesAccessor) manager).rsmixin$removeConnectedNode(thiz);
        ((IActiveFastNodesAccessor) manager).rsmixin$removeActiveFastNode(thiz);
    }

    @Inject(method = "onConnectedStateChange", at = @At("TAIL")) private void onStateChangeInject(INetwork network, boolean state, CallbackInfo ci) {
        if (network instanceof IEnergyDirtyAccessor) { ((IEnergyDirtyAccessor) network).rsmixin$markEnergyDirty(); }
    }
}
