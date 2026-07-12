package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;

import me.modmuss50.rebornstorage.blocks.BlockMultiCrafter;
import me.modmuss50.rebornstorage.multiblocks.MultiBlockCrafter;
import me.modmuss50.rebornstorage.tiles.TileMultiCrafter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockMultiCrafter.class, remap = false) public abstract class BlockMultiCrafterMixin {
    @Inject(method = "onBlockActivated", at = @At("HEAD"), remap = true) private void rsmixin$refreshBeforeOpen(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.enableRebornstorageCrafterFix || worldIn.isRemote) { return; }

        TileEntity tile = worldIn.getTileEntity(pos);
        if (!(tile instanceof TileMultiCrafter)) { return; }

        TileMultiCrafter crafterTile = (TileMultiCrafter) tile;
        if (crafterTile.getMultiblockController() instanceof MultiBlockCrafter && crafterTile.getMultiblockController().isAssembled()) { ((MultiBlockCrafter) crafterTile.getMultiblockController()).updateInfo("rsmixin force on open"); }
    }
}
