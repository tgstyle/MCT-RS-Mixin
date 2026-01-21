package mctmods.rsmixin.mixin.cabletiers;

import com.refinedmods.refinedstorage.api.network.node.INetworkNodeManager;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import com.refinedmods.refinedstorage.inventory.item.UpgradeItemHandler;
import com.ultramega.cabletiers.node.TieredConstructorNetworkNode;

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

@Mixin(TieredConstructorNetworkNode.class)
public abstract class TieredConstructorNetworkNodeMixin extends NetworkNode {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Unique private boolean rsmixin$didWork = false;
    @Unique private int rsmixin$idleCycles = 0;
    @Unique private boolean rsmixin$wasActive = false;
    @Unique private static final int IDLE_THRESHOLD = 5;
    @Unique private int rsmixin$lastLoggedSpeed = -1;

    protected TieredConstructorNetworkNodeMixin(Level level, BlockPos pos) {
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

    @Redirect(method = "update",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/inventory/item/UpgradeItemHandler;getSpeed(II)I"),
            remap = false)
    private int modifyGetSpeed(UpgradeItemHandler upgrades, int baseDivMultiplier, int increase) {
        int originalSpeed = upgrades.getSpeed(baseDivMultiplier, increase);
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
                rsmixin$LOGGER.debug("Forcing tick interval to 1 for idle tiered constructor at {} (original: {})", pos, originalSpeed);
            } else {
                rsmixin$LOGGER.debug("Using upgraded tick interval {} for active tiered constructor at {}", originalSpeed, pos);
            }
            rsmixin$lastLoggedSpeed = effectiveSpeed;
        }

        return effectiveSpeed;
    }

    @Inject(method = {"extractAndDropItem", "extractAndSpawnFireworks", "extractAndPlaceBlock"},
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/network/INetwork;extractItem(Lnet/minecraft/world/item/ItemStack;IILcom/refinedmods/refinedstorage/api/util/Action;)Lnet/minecraft/world/item/ItemStack;"),
            remap = false)
    private void onItemExtractAttempt(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @Inject(method = "extractAndPlaceFluid",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/network/INetwork;extractFluid(Lnet/minecraftforge/fluids/FluidStack;IILcom/refinedmods/refinedstorage/api/util/Action;)Lnet/minecraftforge/fluids/FluidStack;"),
            remap = false)
    private void onFluidExtractAttempt(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @Inject(method = {"extractAndDropItem", "extractAndPlaceBlock"},
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingManager;request(Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;I)Lcom/refinedmods/refinedstorage/api/autocrafting/task/ICraftingTask;"),
            remap = false)
    private void onItemCraftingRequest(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @Inject(method = "extractAndPlaceFluid",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingManager;request(Ljava/lang/Object;Lnet/minecraftforge/fluids/FluidStack;I)Lcom/refinedmods/refinedstorage/api/autocrafting/task/ICraftingTask;"),
            remap = false)
    private void onFluidCraftingRequest(CallbackInfo ci) {
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

        INetworkNodeManager manager = API.instance().getNetworkNodeManager((ServerLevel) level);
        ActiveFastNodesAccessor accessor = (ActiveFastNodesAccessor) manager;

        boolean newActive = rsmixin$didWork;

        if (newActive) {
            if (!rsmixin$wasActive) {
                rsmixin$idleCycles = 0;
                accessor.rsmixin$addActiveFastNode(this);
                rsmixin$wasActive = true;
                if (Config.ENABLE_DEBUG_LOGGING.get()) {
                    rsmixin$LOGGER.debug("Tiered Constructor at {} activated (items/fluids to place)", pos);
                }
            } else {
                rsmixin$idleCycles = 0;
            }
        } else {
            if (rsmixin$wasActive) {
                rsmixin$idleCycles++;
                if (Config.ENABLE_DEBUG_LOGGING.get()) {
                    if (rsmixin$idleCycles == 1) {
                        rsmixin$LOGGER.debug("Tiered Constructor at {} started idling", pos);
                    }
                    if (rsmixin$idleCycles > IDLE_THRESHOLD) {
                        rsmixin$LOGGER.debug("Tiered Constructor at {} deactivated (idle for {} cycles)", pos, IDLE_THRESHOLD);
                    }
                }
                if (rsmixin$idleCycles > IDLE_THRESHOLD) {
                    accessor.rsmixin$removeActiveFastNode(this);
                    rsmixin$wasActive = false;
                    rsmixin$idleCycles = 0;
                }
            }
        }
    }
}
