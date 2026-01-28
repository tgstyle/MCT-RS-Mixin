package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;
import net.gigabit101.rebornstorage.blockentities.BlockEntityMultiCrafter;
import net.gigabit101.rebornstorage.blocks.BlockMultiCrafter;
import net.gigabit101.rebornstorage.core.multiblock.MultiblockControllerBase;
import net.gigabit101.rebornstorage.core.multiblock.MultiblockRegistry;
import net.gigabit101.rebornstorage.multiblocks.MultiBlockCrafter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockMultiCrafter.class)
public class BlockMultiCrafterMixin {

    @Inject(method = "use",
            at = @At("HEAD"),
            cancellable = true)
    private void rsmixin$handleServerInteraction(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()) return;

        if (!(level.getBlockEntity(pos) instanceof BlockEntityMultiCrafter tile)) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        MultiblockControllerBase controller = tile.getMultiblockController();
        if (controller == null) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        if (!Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }

        MultiblockRegistry.tickStart(level);
        if (controller instanceof MultiBlockCrafter multiController && multiController.isAssembled()) {
            multiController.updateInfo("rsmixin force on open");
            BlockPos ref = multiController.getReferenceCoord();
            if (ref != null) {
                level.blockEntityChanged(ref);
                BlockState bs = level.getBlockState(ref);
                level.sendBlockUpdated(ref, bs, bs, 3);
            }
            NetworkHooks.openScreen((ServerPlayer) player, tile, pos);
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
