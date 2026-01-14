package mctmods.rsmixin.mixin;

import com.refinedmods.refinedstorage.api.network.node.INetworkNodeManager;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.network.node.ExporterNetworkNode;
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

@Mixin(ExporterNetworkNode.class)
public abstract class ExporterNetworkNodeMixin extends NetworkNode {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(MODID);

    @Unique private boolean rsmixin$didWork = false;
    @Unique private int rsmixin$idleCycles = 0;
    @Unique private boolean rsmixin$wasActive = false;
    @Unique private static final int IDLE_THRESHOLD = 5;
    @Unique private int rsmixin$lastLoggedSpeed = -1;

    protected ExporterNetworkNodeMixin(Level level, BlockPos pos) {
        super(level, pos);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void onInit(Level level, BlockPos pos, CallbackInfo ci) {
        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            rsmixin$LOGGER.debug("Created Exporter at {}", pos);
        }
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
        int effectiveSpeed = originalSpeed;
        String logMessage;
        if (!Config.ENABLE_BYPASS_FAST_NODES.get()) {
            effectiveSpeed = 1;
            logMessage = "Forcing speed to 1 for exporter at " + pos + " due to bypass disabled (original: " + originalSpeed + ")";
        } else if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get() && !rsmixin$wasActive) {
            effectiveSpeed = 1;
            logMessage = "Forcing speed to 1 for idle exporter at " + pos + " (original: " + originalSpeed + ")";
        } else {
            logMessage = "Using original speed " + originalSpeed + " for exporter at " + pos;
        }
        if (Config.ENABLE_DEBUG_LOGGING.get() && (rsmixin$lastLoggedSpeed == -1 || effectiveSpeed != rsmixin$lastLoggedSpeed)) {
            rsmixin$LOGGER.debug(logMessage);
            rsmixin$lastLoggedSpeed = effectiveSpeed;
        }
        return effectiveSpeed;
    }

    @Inject(method = "update",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/items/ItemHandlerHelper;insertItem(Lnet/minecraftforge/items/IItemHandler;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
                    ordinal = 0),
            remap = false)
    private void onItemTryExport(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @Inject(method = "update",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingManager;request(Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;I)Lcom/refinedmods/refinedstorage/api/autocrafting/task/ICraftingTask;",
                    ordinal = 0),
            remap = false)
    private void onItemCraftingRequest(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @Inject(method = "update",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/capability/IFluidHandler;fill(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I",
                    ordinal = 0),
            remap = false)
    private void onFluidTryExport(CallbackInfo ci) {
        if (Config.ENABLE_DYNAMIC_NODE_SLEEP.get()) {
            rsmixin$didWork = true;
        }
    }

    @Inject(method = "update",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingManager;request(Ljava/lang/Object;Lnet/minecraftforge/fluids/FluidStack;I)Lcom/refinedmods/refinedstorage/api/autocrafting/task/ICraftingTask;",
                    ordinal = 0),
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

        int effectiveSpeed = rsmixin$wasActive ? ((ExporterNetworkNode) (Object) this).getUpgrades().getSpeed() : 1;
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
                rsmixin$LOGGER.debug("Exporter at {} activated (stuff available to export)", pos);
            }
        } else if (!newActive && rsmixin$wasActive) {
            rsmixin$idleCycles++;
            if (Config.ENABLE_DEBUG_LOGGING.get() && rsmixin$idleCycles == 1) {
                rsmixin$LOGGER.debug("Exporter at {} started idling", pos);
            }
            if (rsmixin$idleCycles > IDLE_THRESHOLD) {
                accessor.rsmixin$removeActiveFastNode(this);
                if (Config.ENABLE_DEBUG_LOGGING.get()) {
                    rsmixin$LOGGER.debug("Exporter at {} deactivated (nothing to export for {} cycles)", pos, IDLE_THRESHOLD);
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
