package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;

import net.gigabit101.rebornstorage.core.multiblock.MultiblockControllerBase;
import net.gigabit101.rebornstorage.core.multiblock.MultiblockValidationException;
import net.gigabit101.rebornstorage.multiblocks.MultiBlockCrafter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"ConstantConditions", "unused"}) @Mixin(value = MultiblockControllerBase.class, remap = false) abstract class MultiblockControllerBaseMixin {
    @Unique private static final int RSMIXIN_MIN_RETRY = 20;
    @Unique private static final int RSMIXIN_MAX_RETRY = 200;
    @Unique private static final double RSMIXIN_PLAYER_RANGE = 48.0D;

    @Shadow protected Level worldObj;
    @Shadow private MultiblockValidationException lastValidationException;

    @Shadow public abstract void checkIfMachineIsWhole();

    @Unique private long rsmixin$nextRetryTick;
    @Unique private int rsmixin$retryDelay;

    @Inject(method = "updateMultiblockEntity", at = @At("HEAD")) private void rsmixin$retryValidationIfCrafter(CallbackInfo ci) {
        if (!MultiBlockCrafter.class.isInstance(this) || !Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) { return; }
        if (this.worldObj == null || this.worldObj.isClientSide()) { return; }

        if (this.lastValidationException == null) {
            this.rsmixin$retryDelay = RSMIXIN_MIN_RETRY;
            return;
        }

        long now = this.worldObj.getGameTime();
        if (now < this.rsmixin$nextRetryTick) { return; }

        BlockPos ref = ((MultiblockControllerBase) (Object) this).getReferenceCoord();
        if (ref == null) {
            this.rsmixin$nextRetryTick = now + RSMIXIN_MAX_RETRY;
            return;
        }
        if (!this.worldObj.hasNearbyAlivePlayer(ref.getX() + 0.5D, ref.getY() + 0.5D, ref.getZ() + 0.5D, RSMIXIN_PLAYER_RANGE)) {
            this.rsmixin$nextRetryTick = now + RSMIXIN_MAX_RETRY;
            return;
        }

        String before = this.lastValidationException.getMessage();
        this.checkIfMachineIsWhole();
        String after = this.lastValidationException == null ? null : this.lastValidationException.getMessage();

        if (after == null || !after.equals(before)) {
            this.rsmixin$retryDelay = RSMIXIN_MIN_RETRY;
        } else {
            this.rsmixin$retryDelay = Math.min(Math.max(this.rsmixin$retryDelay, RSMIXIN_MIN_RETRY) * 2, RSMIXIN_MAX_RETRY);
        }
        this.rsmixin$nextRetryTick = now + this.rsmixin$retryDelay;
    }
}
