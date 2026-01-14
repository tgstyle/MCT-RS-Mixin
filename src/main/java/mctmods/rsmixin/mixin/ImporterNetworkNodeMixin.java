package mctmods.rsmixin.mixin;

import com.refinedmods.refinedstorage.api.network.node.INetworkNodeManager;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.node.ImporterNetworkNode;
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

@Mixin(ImporterNetworkNode.class)
public abstract class ImporterNetworkNodeMixin extends NetworkNode {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Unique private boolean rsmixin$didWork = false;
    @Unique private int rsmixin$idleCycles = 0;
    @Unique private boolean rsmixin$wasActive = false;
    @Unique private static final int IDLE_THRESHOLD = 5;

    protected ImporterNetworkNodeMixin(Level level, BlockPos pos) {
        super(level, pos);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onInit(Level level, BlockPos pos, CallbackInfo ci) {
        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            rsmixin$LOGGER.debug("Created Importer at {}", pos);
        }
    }

    @Inject(method = "update", at = @At("HEAD"), remap = false)
    private void resetDidWorkAndEnsureActive(CallbackInfo ci) {
        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            rsmixin$LOGGER.debug("Importer update called at {}", pos);
        }
        if (Config.ENABLE_DYNAMIC_IMPORTER_SLEEP.get()) {
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
        if (!Config.ENABLE_BYPASS_FAST_NODES.get()) {
            if (Config.ENABLE_DEBUG_LOGGING.get()) {
                rsmixin$LOGGER.debug("Forcing speed to 1 for importer at {} due to bypass disabled (original: {})", pos, originalSpeed);
            }
            return 1;
        } else if (Config.ENABLE_DYNAMIC_IMPORTER_SLEEP.get() && !rsmixin$wasActive) {
            if (Config.ENABLE_DEBUG_LOGGING.get()) {
                rsmixin$LOGGER.debug("Forcing speed to 1 for idle importer at {} (original: {})", pos, originalSpeed);
            }
            return 1;
        }
        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            rsmixin$LOGGER.debug("Using original speed {} for importer at {}", originalSpeed, pos);
        }
        return originalSpeed;
    }

    @Inject(method = "update",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/network/INetwork;insertItem(Lnet/minecraft/world/item/ItemStack;ILcom/refinedmods/refinedstorage/api/util/Action;)Lnet/minecraft/world/item/ItemStack;",
                    ordinal = 0),
            remap = false)
    private void onBeforeItemSimulateInsert(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_IMPORTER_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @Inject(method = "update",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/network/INetwork;insertFluid(Lnet/minecraftforge/fluids/FluidStack;ILcom/refinedmods/refinedstorage/api/util/Action;)Lnet/minecraftforge/fluids/FluidStack;",
                    ordinal = 0),
            remap = false)
    private void onBeforeFluidSimulateInsert(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_IMPORTER_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "update", at = @At("TAIL"), remap = false)
    private void manageActivation(CallbackInfo ci) {
        if (!Config.ENABLE_DYNAMIC_IMPORTER_SLEEP.get()) {
            return;
        }

        int effectiveSpeed = rsmixin$wasActive ? ((UpgradeItemHandler) ((ImporterNetworkNode) (Object) this).getUpgrades()).getSpeed() : 1;
        if (this.ticks % effectiveSpeed != 0) {
            return;
        }

        INetworkNodeManager manager = API.instance().getNetworkNodeManager((ServerLevel) level);
        ActiveFastNodesAccessor accessor = (ActiveFastNodesAccessor) manager;

        boolean newActive = rsmixin$didWork;

        if (newActive && !rsmixin$wasActive) {
            rsmixin$idleCycles = 0;
            accessor.rsmixin$addActiveFastNode(this);
            if (Config.ENABLE_DEBUG_LOGGING.get()) {
                rsmixin$LOGGER.debug("Importer at {} activated (stuff available to import)", pos);
            }
        } else if (!newActive && rsmixin$wasActive) {
            rsmixin$idleCycles++;
            if (Config.ENABLE_DEBUG_LOGGING.get() && rsmixin$idleCycles == 1) {
                rsmixin$LOGGER.debug("Importer at {} started idling", pos);
            }
            if (rsmixin$idleCycles > IDLE_THRESHOLD) {
                accessor.rsmixin$removeActiveFastNode(this);
                if (Config.ENABLE_DEBUG_LOGGING.get()) {
                    rsmixin$LOGGER.debug("Importer at {} deactivated (nothing to import for {} cycles)", pos, IDLE_THRESHOLD);
                }
            }
        } else if (newActive) {
            rsmixin$idleCycles = 0;
        } else {
            rsmixin$idleCycles++;
        }

        rsmixin$wasActive = newActive;
    }
}
