package mctmods.rsmixin.mixin.client.rebornstorage;

import net.gigabit101.rebornstorage.blockentities.BlockEntityMultiCrafter;
import net.gigabit101.rebornstorage.blocks.BlockMultiCrafter;
import net.gigabit101.rebornstorage.core.multiblock.MultiblockControllerBase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockMultiCrafter.class)
public class BlockMultiCrafterMixin {

    @Inject(method = "use",
            at = @At("HEAD"),
            cancellable = true)
    private void rsmixin$handleClientValidation(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide()) return;

        if (!(level.getBlockEntity(pos) instanceof BlockEntityMultiCrafter tile)) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        MultiblockControllerBase controller = tile.getMultiblockController();
        if (controller == null) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        controller.checkIfMachineIsWhole();
        if (controller.getLastValidationException() != null) {
            String msg = controller.getLastValidationException().getMessage();
            if (!msg.isEmpty()) {
                Player clientPlayer = Minecraft.getInstance().player;
                if (clientPlayer != null) {
                    clientPlayer.displayClientMessage(Component.literal(msg), false);
                }
            }
            cir.setReturnValue(InteractionResult.CONSUME);
        } else {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
