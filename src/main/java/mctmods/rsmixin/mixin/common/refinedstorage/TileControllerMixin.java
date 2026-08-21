package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.interfaces.IEnergyDirty;

import com.raoulvdberge.refinedstorage.api.network.INetworkNodeGraph;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.cover.Cover;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.cover.CoverManager;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.cover.CoverType;
import com.raoulvdberge.refinedstorage.tile.TileController;
import com.raoulvdberge.refinedstorage.tile.config.RedstoneMode;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileController.class, remap = false) public abstract class TileControllerMixin implements IEnergyDirty {
    @Unique private boolean rsmixin$dirtyEnergyUsage = true;
    @Unique private int rsmixin$cachedEnergyUsage = -1;
    @Unique private boolean rsmixin$wasLoaded = false;
    @Unique private long rsmixin$rescanAtGameTime = -1L;
    @Unique private int rsmixin$energyRefreshTicks = 0;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Shadow public abstract INetworkNodeGraph getNodeGraph();

    @Override public void rsmixin$markEnergyDirty() { rsmixin$dirtyEnergyUsage = true; }

    @Inject(method = "<init>", at = @At("RETURN")) private void initFlags(CallbackInfo ci) {
        rsmixin$dirtyEnergyUsage = true;
        rsmixin$cachedEnergyUsage = -1;
        rsmixin$wasLoaded = false;
        rsmixin$rescanAtGameTime = -1L;
        getNodeGraph().addListener(() -> rsmixin$dirtyEnergyUsage = true);
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = true) private void updateWithLoadRescan(CallbackInfo ci) {
        TileController self = (TileController) (Object) this;
        World world = self.getWorld();
        BlockPos pos = self.getPos();
        if (world == null || world.isRemote) { return; }

        if (Config.enableSkipUnloaded && !world.isBlockLoaded(pos)) {
            if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Skipping network update for pos {} during chunk unload (chunk not present)", pos); }
            ci.cancel();
            return;
        }

        if (Config.enableLazyEnergy && ++rsmixin$energyRefreshTicks >= 20) {
            rsmixin$energyRefreshTicks = 0;
            rsmixin$dirtyEnergyUsage = true;
        }

        if (Config.enableLoadRescan) {
            boolean currentlyLoaded = world.isBlockLoaded(pos);
            long gameTime = world.getTotalWorldTime();
            if (currentlyLoaded && !rsmixin$wasLoaded) { rsmixin$rescanAtGameTime = gameTime + Config.loadRescanDelay; }
            if (rsmixin$rescanAtGameTime >= 0L && currentlyLoaded && gameTime >= rsmixin$rescanAtGameTime) {
                rsmixin$rescanAtGameTime = -1L;
                getNodeGraph().invalidate(Action.PERFORM, world, pos);
                if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("RSMixin: Forced network graph rescan on load for controller at {}", pos); }
            }
            rsmixin$wasLoaded = currentlyLoaded;
        }
    }

    @Inject(method = "getEnergyUsage", at = @At("HEAD"), cancellable = true) private void lazyEnergyPre(CallbackInfoReturnable<Integer> cir) {
        if (Config.enableLazyEnergy && !rsmixin$dirtyEnergyUsage && rsmixin$cachedEnergyUsage >= 0) { cir.setReturnValue(rsmixin$cachedEnergyUsage); }
    }

    @Inject(method = "getEnergyUsage", at = @At("RETURN")) private void lazyEnergyPost(CallbackInfoReturnable<Integer> cir) {
        if (Config.enableLazyEnergy) {
            rsmixin$cachedEnergyUsage = cir.getReturnValue();
            rsmixin$dirtyEnergyUsage = false;
        }
    }

    @Inject(method = "setRedstoneMode", at = @At("HEAD")) private void dirtyOnRedstoneMode(RedstoneMode mode, CallbackInfo ci) { rsmixin$dirtyEnergyUsage = true; }

    @Redirect(method = "visit", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/apiimpl/network/node/cover/CoverManager;hasCover(Lnet/minecraft/util/EnumFacing;)Z")) private boolean rsmixin$nonHollowCoversOnly(CoverManager manager, EnumFacing facing) {
        if (!Config.enableHollowCoverConnectionFix) { return manager.hasCover(facing); }
        Cover cover = manager.getCover(facing);
        return cover != null && cover.getType() != CoverType.HOLLOW;
    }
}
