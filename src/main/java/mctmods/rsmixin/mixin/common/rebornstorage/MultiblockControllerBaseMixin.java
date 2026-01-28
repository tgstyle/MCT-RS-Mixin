package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;

import net.gigabit101.rebornstorage.core.multiblock.IMultiblockPart;
import net.gigabit101.rebornstorage.core.multiblock.MultiblockControllerBase;
import net.gigabit101.rebornstorage.core.multiblock.MultiblockValidationException;
import net.gigabit101.rebornstorage.multiblocks.MultiBlockCrafter;

import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;

@SuppressWarnings({"ConstantConditions", "unused"})
@Mixin(MultiblockControllerBase.class)
abstract class MultiblockControllerBaseMixin {

    @Shadow protected Level worldObj;

    @Shadow public HashSet<IMultiblockPart> connectedParts;

    @Shadow public abstract boolean isAssembled();

    @Shadow private MultiblockValidationException lastValidationException;

    @Shadow public abstract void checkIfMachineIsWhole();

    @Inject(
            method = "updateMultiblockEntity",
            at = @At("HEAD"),
            remap = false
    )
    private void rsmixin$retryValidationIfCrafter(CallbackInfo ci) {
        if (MultiBlockCrafter.class.isInstance(this)
                && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()
                && this.lastValidationException != null) {
            this.checkIfMachineIsWhole();
        }
    }
}
