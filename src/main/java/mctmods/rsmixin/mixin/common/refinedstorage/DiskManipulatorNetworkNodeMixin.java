package mctmods.rsmixin.mixin.common.refinedstorage;

import com.refinedmods.refinedstorage.api.network.node.INetworkNodeManager;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.node.diskmanipulator.DiskManipulatorNetworkNode;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import com.refinedmods.refinedstorage.inventory.item.UpgradeItemHandler;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.ActiveFastNodesAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mctmods.rsmixin.RSMixin.MODID;

@Mixin(DiskManipulatorNetworkNode.class)
public abstract class DiskManipulatorNetworkNodeMixin extends NetworkNode {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Unique private boolean rsmixin$didWork = false;
    @Unique private int rsmixin$idleCycles = 0;
    @Unique private boolean rsmixin$wasActive = false;
    @Unique private static final int IDLE_THRESHOLD = 5;
    @Unique private int rsmixin$lastLoggedSpeed = -1;

    protected DiskManipulatorNetworkNodeMixin(Level level, BlockPos pos) {
        super(level, pos);
    }

    @Inject(method = "update", at = @At("HEAD"), remap = false)
    private void resetDidWorkAndEnsureActive(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = false;
        } else if (!rsmixin$wasActive && level instanceof ServerLevel && Config.ENABLE_BYPASS_FAST_NODES.get()) {
            INetworkNodeManager manager = API.instance().getNetworkNodeManager((ServerLevel) level);
            ActiveFastNodesAccessor accessor = (ActiveFastNodesAccessor) manager;
            accessor.rsmixin$addActiveFastNode(this);
            rsmixin$wasActive = true;
        }
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/inventory/item/UpgradeItemHandler;getSpeed()I"), remap = false)
    private int modifyGetSpeed(UpgradeItemHandler upgrades) {
        int originalSpeed = upgrades.getSpeed();
        int effectiveSpeed;

        if (!Config.ENABLE_BYPASS_FAST_NODES.get()) {
            effectiveSpeed = 1;
        } else if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get() && !rsmixin$wasActive) {
            effectiveSpeed = 1;
        } else {
            effectiveSpeed = originalSpeed;
        }

        if (Config.ENABLE_DEBUG_LOGGING.get() && effectiveSpeed != rsmixin$lastLoggedSpeed) {
            if (effectiveSpeed == 1) {
                rsmixin$LOGGER.debug("Forcing tick interval to 1 for idle disk manipulator at {} (original: {})", pos, originalSpeed);
            } else {
                rsmixin$LOGGER.debug("Using upgraded tick interval {} for active disk manipulator at {}", originalSpeed, pos);
            }
            rsmixin$lastLoggedSpeed = effectiveSpeed;
        }

        return effectiveSpeed;
    }

    @Inject(method = "insertItemIntoNetwork", at = @At("HEAD"), remap = false)
    private void onInsertItemHead(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @Inject(method = "extractItemFromNetwork", at = @At("HEAD"), remap = false)
    private void onExtractItemHead(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @Inject(method = "insertFluidIntoNetwork", at = @At("HEAD"), remap = false)
    private void onInsertFluidHead(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @Inject(method = "extractFluidFromNetwork", at = @At("HEAD"), remap = false)
    private void onExtractFluidHead(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "update", at = @At("TAIL"), remap = false)
    private void manageActivation(CallbackInfo ci) {
        if (!Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            return;
        }

        int effectiveSpeed = rsmixin$wasActive ? ((UpgradeItemHandler) ((DiskManipulatorNetworkNode) (Object) this).getUpgrades()).getSpeed() : 1;
        if (this.ticks % effectiveSpeed != 0) {
            return;
        }

        INetworkNodeManager manager = API.instance().getNetworkNodeManager((ServerLevel) level);
        ActiveFastNodesAccessor accessor = (ActiveFastNodesAccessor) manager;

        boolean newActive = rsmixin$didWork;

        if (newActive && !rsmixin$wasActive) {
            rsmixin$idleCycles = 0;
            accessor.rsmixin$addActiveFastNode(this);
            rsmixin$wasActive = true;
            if (Config.ENABLE_DEBUG_LOGGING.get()) {
                rsmixin$LOGGER.debug("Disk Manipulator at {} activated (drive processing active)", pos);
            }
        } else if (!newActive && rsmixin$wasActive) {
            rsmixin$idleCycles++;
            if (Config.ENABLE_DEBUG_LOGGING.get()) {
                if (rsmixin$idleCycles == 1) {
                    rsmixin$LOGGER.debug("Disk Manipulator at {} started idling", pos);
                }
                if (rsmixin$idleCycles > IDLE_THRESHOLD) {
                    rsmixin$LOGGER.debug("Disk Manipulator at {} deactivated (idle for {} cycles)", pos, IDLE_THRESHOLD);
                }
            }
            if (rsmixin$idleCycles > IDLE_THRESHOLD) {
                accessor.rsmixin$removeActiveFastNode(this);
                rsmixin$wasActive = false;
                rsmixin$idleCycles = 0;
            }
        } else if (newActive) {
            rsmixin$idleCycles = 0;
        } else {
            rsmixin$idleCycles++;
        }
    }
}
