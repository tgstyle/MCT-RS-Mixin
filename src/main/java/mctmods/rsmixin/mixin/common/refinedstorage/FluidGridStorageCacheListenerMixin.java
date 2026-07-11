package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.RS;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.apiimpl.storage.cache.listener.FluidGridStorageCacheListener;
import com.refinedmods.refinedstorage.network.grid.GridFluidUpdateMessage;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidGridStorageCacheListener.class, remap = false) public abstract class FluidGridStorageCacheListenerMixin {
    @Shadow @Final private ServerPlayer player;
    @Shadow @Final private INetwork network;
    @Unique private long rsmixin$lastResync = Long.MIN_VALUE;

    @Inject(method = "onInvalidated", at = @At("HEAD")) private void rsmixin$resyncOnInvalidate(CallbackInfo ci) {
        if (!Config.ENABLE_GRID_RESYNC.get()) { return; }
        long gameTime = player.level().getGameTime();
        if (gameTime == rsmixin$lastResync) { return; }
        rsmixin$lastResync = gameTime;
        RS.NETWORK_HANDLER.sendTo(player, new GridFluidUpdateMessage(network, network.getSecurityManager().hasPermission(Permission.AUTOCRAFTING, player)));
    }
}
