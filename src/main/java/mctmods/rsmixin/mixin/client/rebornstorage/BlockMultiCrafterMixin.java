package mctmods.rsmixin.mixin.client.rebornstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.client.CrafterBuildHints;
import mctmods.rsmixin.helper.rebornstorage.MultiblockFault;
import mctmods.rsmixin.helper.rebornstorage.MultiblockFaultHolder;

import net.gigabit101.rebornstorage.blockentities.BlockEntityMultiCrafter;
import net.gigabit101.rebornstorage.blocks.BlockMultiCrafter;
import net.gigabit101.rebornstorage.core.multiblock.MultiblockControllerBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockMultiCrafter.class)
public class BlockMultiCrafterMixin {

    @Inject(method = "use",
            at = @At("HEAD"),
            cancellable = true)
    private void rsmixin$handleClientValidation(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide()) return;
        if (!Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) return;
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) return;

        if (!(level.getBlockEntity(blockPos) instanceof BlockEntityMultiCrafter tile)) return;

        MultiblockControllerBase controller = tile.getMultiblockController();
        if (controller == null) return;

        controller.checkIfMachineIsWhole();

        int cpuCount = -1;
        int storageCount = -1;
        List<MultiblockFault> faults = List.of();
        if (controller instanceof MultiblockFaultHolder holder) {
            faults = List.copyOf(holder.rsmixin$faults());
            cpuCount = holder.rsmixin$cpuCount();
            storageCount = holder.rsmixin$storageCount();
        }

        if (controller.getLastValidationException() == null) {
            // Formed. Only speak up when it is formed but cannot work yet; otherwise stay
            // quiet and let the server open the screen.
            if (cpuCount == 0 || storageCount == 0) {
                CrafterBuildHints.showFormed(player, controller.getMinimumCoord(), controller.getMaximumCoord(),
                        cpuCount, storageCount);
            } else {
                CrafterBuildHints.clear();
                CrafterBuildHints.forgetAdvice();
            }
            return;
        }

        if (faults.isEmpty()) {
            // A vanilla-side rejection we do not model (too small, too large, foreign part).
            CrafterBuildHints.clear();
            String message = controller.getLastValidationException().getMessage();
            if (message != null && !message.isEmpty()) {
                player.displayClientMessage(Component.literal(message), false);
            }
        } else {
            CrafterBuildHints.show(player, faults, controller.getMinimumCoord(), controller.getMaximumCoord(),
                    blockPos, cpuCount, storageCount);
        }

        cir.setReturnValue(InteractionResult.PASS);
    }
}
