package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;

import net.gigabit101.rebornstorage.RebornStorage;
import net.gigabit101.rebornstorage.containers.ContainerMultiCrafter;
import net.gigabit101.rebornstorage.core.multiblock.IMultiblockPart;
import net.gigabit101.rebornstorage.core.multiblock.MultiblockRegistry;
import net.gigabit101.rebornstorage.init.ModBlocks;
import net.gigabit101.rebornstorage.multiblocks.MultiBlockCrafter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ContainerMultiCrafter.class) public abstract class ContainerMultiCrafterMixin {
    @Shadow(remap = false) public MultiBlockCrafter crafter;

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true) private void rsmixin$closeWhenBroken(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() || this.crafter == null) { return; }
        cir.setReturnValue(this.crafter.isAssembled());
    }

    @Redirect(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/gigabit101/rebornstorage/blockentities/BlockEntityMultiCrafter;)V", at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/RebornStorage;getMultiBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/gigabit101/rebornstorage/multiblocks/MultiBlockCrafter;"), remap = false) private MultiBlockCrafter rsmixin$forceClientClearAndPurgeStaleParts(Level world, BlockPos pos) {
        MultiBlockCrafter controller = RebornStorage.getMultiBlock(world, pos);

        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() && world.isClientSide() && controller != null) {
            controller.invs.clear();

            Set<IMultiblockPart> toRemove = new HashSet<>();
            for (IMultiblockPart part : controller.connectedParts) {
                BlockPos partPos = part.getBlockPos();
                if (!world.isLoaded(partPos) ||
                        world.getBlockEntity(partPos) != part ||
                        part.isInvalid()) {
                    toRemove.add(part);
                    continue;
                }

                if (part.getBlockState().getBlock() == ModBlocks.BLOCK_MULTI_STORAGE.get() &&
                        world.getBlockState(partPos).getBlock() != ModBlocks.BLOCK_MULTI_STORAGE.get()) {
                    toRemove.add(part);
                }
            }

            for (IMultiblockPart badPart : toRemove) { controller.detachBlock(badPart, false); }

            MultiblockRegistry.tickStart(world);
            controller.checkForDisconnections();
            controller.updateInfo("rsmixin client force clear + purge + rebuild");
        }

        return controller;
    }

    @Redirect(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/gigabit101/rebornstorage/blockentities/BlockEntityMultiCrafter;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;isEmpty()Z", ordinal = 0), require = 1, remap = false) private boolean rsmixin$allowPlayerSlotsWhenEmpty(Map<?, ?> map) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) { return false; }
        return map.isEmpty();
    }
}
