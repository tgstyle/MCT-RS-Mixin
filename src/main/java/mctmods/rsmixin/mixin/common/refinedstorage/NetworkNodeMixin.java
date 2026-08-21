package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.interfaces.IActiveFastNodes;
import mctmods.rsmixin.core.interfaces.IConnectedNodes;
import mctmods.rsmixin.core.interfaces.IEnergyDirty;

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
        if (Config.enableConnectedNodeTickOptimize) { ((IConnectedNodes) manager).rsmixin$addConnectedNode(thiz); }
        if (Config.enableBypassFastNodes && Arrays.asList(Config.fastNodeClasses).contains(thiz.getClass().getName())) { ((IActiveFastNodes) manager).rsmixin$addActiveFastNode(thiz); }
    }

    @Inject(method = "onDisconnected", at = @At("HEAD")) private void onDisconnectedInject(INetwork network, CallbackInfo ci) {
        NetworkNode thiz = (NetworkNode) (Object) this;
        if (thiz.getWorld() == null || thiz.getWorld().isRemote) { return; }
        NetworkNodeManager manager = (NetworkNodeManager) API.instance().getNetworkNodeManager(thiz.getWorld());
        ((IConnectedNodes) manager).rsmixin$removeConnectedNode(thiz);
        ((IActiveFastNodes) manager).rsmixin$removeActiveFastNode(thiz);
    }

    @Inject(method = "onConnectedStateChange", at = @At("TAIL")) private void onStateChangeInject(INetwork network, boolean state, CallbackInfo ci) {
        if (network instanceof IEnergyDirty) { ((IEnergyDirty) network).rsmixin$markEnergyDirty(); }
    }
}
