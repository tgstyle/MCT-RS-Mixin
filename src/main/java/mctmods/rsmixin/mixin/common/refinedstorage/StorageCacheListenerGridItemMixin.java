package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.raoulvdberge.refinedstorage.apiimpl.storage.StorageCacheListenerGridItem;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StorageCacheListenerGridItem.class, remap = false) public abstract class StorageCacheListenerGridItemMixin {
    @Shadow private EntityPlayerMP player;
    @Unique private long rsmixin$lastResync = Long.MIN_VALUE;

    @Shadow public abstract void onAttached();

    @Inject(method = "onInvalidated", at = @At("HEAD")) private void rsmixin$resyncOnInvalidate(CallbackInfo ci) {
        if (!Config.enableGridResync) { return; }
        long gameTime = player.world.getTotalWorldTime();
        if (gameTime == rsmixin$lastResync) { return; }
        rsmixin$lastResync = gameTime;
        this.onAttached();
    }
}
