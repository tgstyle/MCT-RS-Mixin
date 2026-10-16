package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.IActiveFastNodesAccessor;
import mctmods.rsmixin.core.accessor.IConnectedNodesAccessor;
import mctmods.rsmixin.core.accessor.IEnergyDirtyAccessor;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.node.INetworkNodeManager;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.NetworkNodeManager;
import com.refinedmods.refinedstorage.apiimpl.network.node.ConnectivityStateChangeCause;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(NetworkNode.class) public abstract class NetworkNodeMixin {
    @Unique private static final int RSMIXIN$SETTLE_UPDATES = 40;
    @Shadow(remap = false) @Nullable protected INetwork network;
    @Shadow(remap = false) protected Level level;
    @Shadow(remap = false) protected BlockPos pos;
    @Shadow(remap = false) private boolean couldUpdate;
    @Unique private int rsmixin$settledUpdates;

    @Inject(method = "onConnected", at = @At("TAIL"), remap = false) private void onConnectedInject(INetwork network, CallbackInfo ci) {
        NetworkNode thiz = (NetworkNode) (Object) this;
        NetworkNodeManager manager = (NetworkNodeManager) API.instance().getNetworkNodeManager((ServerLevel) thiz.getLevel());
        if (Config.ENABLE_CONNECTED_NODE_TICK_OPTIMIZE.get()) { ((IConnectedNodesAccessor) manager).rsmixin$addConnectedNode(thiz); }
        if (Config.ENABLE_BYPASS_FAST_NODES.get() && Config.FAST_NODE_CLASSES.get().contains(thiz.getClass().getName())) { ((IActiveFastNodesAccessor) manager).rsmixin$addActiveFastNode(thiz); }
    }

    @Inject(method = "onDisconnected", at = @At("HEAD"), remap = false) private void onDisconnectedInject(INetwork network, CallbackInfo ci) {
        NetworkNode thiz = (NetworkNode) (Object) this;
        NetworkNodeManager manager = (NetworkNodeManager) API.instance().getNetworkNodeManager((ServerLevel) thiz.getLevel());
        ((IActiveFastNodesAccessor) manager).rsmixin$removeActiveFastNode(thiz);
        rsmixin$wake();
    }

    @Inject(method = "markDirty", at = @At("HEAD"), remap = false) private void markDirtyInject(CallbackInfo ci) { rsmixin$wake(); }

    @Inject(method = "setRedstonePowered", at = @At("HEAD"), remap = false) private void setRedstonePoweredInject(boolean redstonePowered, CallbackInfo ci) { rsmixin$wake(); }

    @Inject(method = "update", at = @At("TAIL"), remap = false) private void settleInject(CallbackInfo ci) {
        if (!Config.ENABLE_CONNECTED_NODE_TICK_OPTIMIZE.get() || network != null || couldUpdate) { rsmixin$settledUpdates = 0; }
        else if (++rsmixin$settledUpdates > RSMIXIN$SETTLE_UPDATES && level instanceof ServerLevel serverLevel) { ((IConnectedNodesAccessor) API.instance().getNetworkNodeManager(serverLevel)).rsmixin$removeConnectedNode((NetworkNode) (Object) this); }
    }

    @Unique private void rsmixin$wake() {
        if (!Config.ENABLE_CONNECTED_NODE_TICK_OPTIMIZE.get() || !(level instanceof ServerLevel serverLevel)) { return; }
        NetworkNode thiz = (NetworkNode) (Object) this;
        INetworkNodeManager manager = API.instance().getNetworkNodeManager(serverLevel);
        if (manager.getNode(pos) != thiz) { return; }
        rsmixin$settledUpdates = 0;
        ((IConnectedNodesAccessor) manager).rsmixin$addConnectedNode(thiz);
    }

    @Inject(method = "onConnectedStateChange", at = @At("TAIL"), remap = false) private void onStateChangeInject(INetwork network, boolean state, ConnectivityStateChangeCause cause, CallbackInfo ci) {
        if (network instanceof IEnergyDirtyAccessor accessor) { accessor.rsmixin$markEnergyDirty(); }
    }
}
