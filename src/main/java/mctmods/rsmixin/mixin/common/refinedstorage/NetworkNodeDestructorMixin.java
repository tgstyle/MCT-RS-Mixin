package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.interfaces.IActiveFastNodes;

import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeManager;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeDestructor;
import com.raoulvdberge.refinedstorage.inventory.item.ItemHandlerUpgrade;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkNodeDestructor.class, remap = false) public abstract class NetworkNodeDestructorMixin extends NetworkNode {
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);
    @Unique private boolean rsmixin$didWork = false;
    @Unique private int rsmixin$idleCycles = 0;
    @Unique private boolean rsmixin$wasActive = false;
    @Unique private static final int IDLE_THRESHOLD = 5;
    @Unique private int rsmixin$lastLoggedSpeed = -1;
    @Unique private int rsmixin$lastEffectiveSpeed = 1;

    protected NetworkNodeDestructorMixin(World world, BlockPos pos) { super(world, pos); }

    @Inject(method = "update", at = @At("HEAD")) private void resetDidWorkAndEnsureActive(CallbackInfo ci) {
        if (world == null || world.isRemote) { return; }
        if (Config.enableDynamicNodeSleep) { rsmixin$didWork = false; }
        else if (!rsmixin$wasActive && Config.enableBypassFastNodes) {
            INetworkNodeManager manager = API.instance().getNetworkNodeManager(world);
            ((IActiveFastNodes) manager).rsmixin$addActiveFastNode(this);
            rsmixin$wasActive = true;
        }
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/inventory/item/ItemHandlerUpgrade;getSpeed(II)I")) private int modifyGetSpeed(ItemHandlerUpgrade upgrades, int speed, int speedIncrease) {
        int originalSpeed = upgrades.getSpeed(speed, speedIncrease);
        int effectiveSpeed;

        if (!Config.enableBypassFastNodes) { effectiveSpeed = 1; }
        else if (Config.enableDynamicNodeSleep && !rsmixin$wasActive) { effectiveSpeed = 1; }
        else { effectiveSpeed = originalSpeed; }

        if (Config.enableDebugLogging && effectiveSpeed != rsmixin$lastLoggedSpeed) {
            if (effectiveSpeed == 1) { rsmixin$LOGGER.debug("Forcing tick interval to 1 for idle destructor at {} (original: {})", pos, originalSpeed); }
            else { rsmixin$LOGGER.debug("Using upgraded tick interval {} for active destructor at {}", originalSpeed, pos); }
            rsmixin$lastLoggedSpeed = effectiveSpeed;
        }

        rsmixin$lastEffectiveSpeed = effectiveSpeed;
        return effectiveSpeed;
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/api/network/INetwork;insertItem(Lnet/minecraft/item/ItemStack;ILcom/raoulvdberge/refinedstorage/api/util/Action;)Lnet/minecraft/item/ItemStack;")) private void onItemInsertAttempt(CallbackInfo ci) {
        if (Config.enableDynamicNodeSleep) { rsmixin$didWork = true; }
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/api/network/INetwork;insertFluid(Lnet/minecraftforge/fluids/FluidStack;ILcom/raoulvdberge/refinedstorage/api/util/Action;)Lnet/minecraftforge/fluids/FluidStack;")) private void onFluidInsertAttempt(CallbackInfo ci) {
        if (Config.enableDynamicNodeSleep) { rsmixin$didWork = true; }
    }


    @Inject(method = "update", at = @At("TAIL")) private void manageActivation(CallbackInfo ci) {
        if (!Config.enableDynamicNodeSleep) { return; }
        if (world == null || world.isRemote) { return; }

        int effectiveSpeed = rsmixin$wasActive ? rsmixin$lastEffectiveSpeed : 1;
        if (effectiveSpeed < 1) { effectiveSpeed = 1; }
        if (this.ticks % effectiveSpeed != 0) { return; }

        INetworkNodeManager manager = API.instance().getNetworkNodeManager(world);
        IActiveFastNodes accessor = (IActiveFastNodes) manager;

        boolean newActive = rsmixin$didWork;

        if (newActive && !rsmixin$wasActive) {
            rsmixin$idleCycles = 0;
            accessor.rsmixin$addActiveFastNode(this);
            rsmixin$wasActive = true;
            if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("Destructor at {} activated", pos); }
        }
        else if (!newActive && rsmixin$wasActive) {
            rsmixin$idleCycles++;
            if (rsmixin$idleCycles > IDLE_THRESHOLD) {
                accessor.rsmixin$removeActiveFastNode(this);
                rsmixin$wasActive = false;
                if (Config.enableDebugLogging) { rsmixin$LOGGER.debug("Destructor at {} deactivated (idle for {} cycles)", pos, IDLE_THRESHOLD); }
            }
        }
        else if (newActive) { rsmixin$idleCycles = 0; }
        else { rsmixin$idleCycles++; }
    }
}
