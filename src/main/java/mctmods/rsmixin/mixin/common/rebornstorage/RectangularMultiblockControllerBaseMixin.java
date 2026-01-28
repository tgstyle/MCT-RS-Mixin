package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;

import net.gigabit101.rebornstorage.core.multiblock.MultiblockValidationException;
import net.gigabit101.rebornstorage.core.multiblock.rectangular.RectangularMultiblockControllerBase;
import net.gigabit101.rebornstorage.core.multiblock.rectangular.RectangularMultiblockTileEntityBase;
import net.gigabit101.rebornstorage.init.ModBlocks;
import net.gigabit101.rebornstorage.multiblocks.MultiBlockCrafter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RectangularMultiblockControllerBase.class)
public abstract class RectangularMultiblockControllerBaseMixin {

    @Unique
    private String rsmixin$getBlockDisplayName(BlockState state) {
        if (state.isAir()) {
            return "Air";
        }
        return state.getBlock().getName().getString();
    }

    @Unique
    private String rsmixin$getExpectedFrame() {
        String frame = ModBlocks.BLOCK_MULTI_FRAME.get().getName().getString();
        String storage = ModBlocks.BLOCK_MULTI_STORAGE.get().getName().getString();
        String cpu = ModBlocks.BLOCK_MULTI_CPU.get().getName().getString();
        return frame + ", " + storage + ", or " + cpu;
    }

    @Unique
    private String rsmixin$getExpectedHeat() {
        return ModBlocks.BLOCK_MULTI_HEAT.get().getName().getString();
    }

    @Unique
    private String rsmixin$getExpectedInterior() {
        String storage = ModBlocks.BLOCK_MULTI_STORAGE.get().getName().getString();
        String cpu = ModBlocks.BLOCK_MULTI_CPU.get().getName().getString();
        return "Air, " + storage + ", or " + cpu;
    }

    @Unique
    private MultiblockValidationException rsmixin$createException(String name, BlockPos pos, String message) {
        String full = name + " " + message + " at " + pos.toShortString();
        return new MultiblockValidationException(full);
    }

    @Unique
    private void rsmixin$checkPartFrame(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        BlockState state = tile.getBlockState();
        BlockPos pos = tile.getBlockPos();
        String name = rsmixin$getBlockDisplayName(state);
        if (state.getBlock() == ModBlocks.BLOCK_MULTI_STORAGE.get()
                || state.getBlock() == ModBlocks.BLOCK_MULTI_CPU.get()
                || state.getBlock() == ModBlocks.BLOCK_MULTI_FRAME.get()) {
            return;
        }
        String expected = rsmixin$getExpectedFrame();
        throw rsmixin$createException(name, pos, "is invalid on corners/edges (expected " + expected + ")");
    }

    @Unique
    private void rsmixin$checkPartFace(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        BlockState state = tile.getBlockState();
        BlockPos pos = tile.getBlockPos();
        String name = rsmixin$getBlockDisplayName(state);
        if (state.getBlock() == ModBlocks.BLOCK_MULTI_HEAT.get()) {
            return;
        }
        String expected = rsmixin$getExpectedHeat();
        throw rsmixin$createException(name, pos, "is invalid on face surface (expected " + expected + ")");
    }

    @Unique
    private void rsmixin$checkPartInterior(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        BlockState state = tile.getBlockState();
        BlockPos pos = tile.getBlockPos();
        String name = rsmixin$getBlockDisplayName(state);
        if (state.getBlock() == ModBlocks.BLOCK_MULTI_STORAGE.get() || state.getBlock() == ModBlocks.BLOCK_MULTI_CPU.get()) {
            return;
        }
        String expected = rsmixin$getExpectedInterior();
        throw rsmixin$createException(name, pos, "is invalid in interior (expected " + expected + ")");
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;getMinimumNumberOfBlocksForAssembledMachine()I"
            )
    )
    private int rsmixin$minBlocks(RectangularMultiblockControllerBase controller) {
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            return 27;
        }
        return 1;
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;isBlockGoodForFrame(Lnet/minecraft/world/level/Level;III)V"
            )
    )
    private void rsmixin$checkFrame(RectangularMultiblockControllerBase controller, Level world, int x, int y, int z) throws MultiblockValidationException {
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        String name = rsmixin$getBlockDisplayName(state);
        if (state.getBlock() == ModBlocks.BLOCK_MULTI_STORAGE.get()
                || state.getBlock() == ModBlocks.BLOCK_MULTI_CPU.get()
                || state.getBlock() == ModBlocks.BLOCK_MULTI_FRAME.get()) {
            return;
        }
        String expected = rsmixin$getExpectedFrame();
        throw rsmixin$createException(name, pos, "is invalid on corners/edges (expected " + expected + ")");
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;isBlockGoodForTop(Lnet/minecraft/world/level/Level;III)V"
            )
    )
    private void rsmixin$checkTop(RectangularMultiblockControllerBase controller, Level world, int x, int y, int z) throws MultiblockValidationException {
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        String name = rsmixin$getBlockDisplayName(state);
        if (state.getBlock() == ModBlocks.BLOCK_MULTI_HEAT.get()) {
            return;
        }
        String expected = rsmixin$getExpectedHeat();
        throw rsmixin$createException(name, pos, "is invalid on face surface (expected " + expected + ")");
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;isBlockGoodForBottom(Lnet/minecraft/world/level/Level;III)V"
            )
    )
    private void rsmixin$checkBottom(RectangularMultiblockControllerBase controller, Level world, int x, int y, int z) throws MultiblockValidationException {
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        String name = rsmixin$getBlockDisplayName(state);
        if (state.getBlock() == ModBlocks.BLOCK_MULTI_HEAT.get()) {
            return;
        }
        String expected = rsmixin$getExpectedHeat();
        throw rsmixin$createException(name, pos, "is invalid on face surface (expected " + expected + ")");
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;isBlockGoodForSides(Lnet/minecraft/world/level/Level;III)V"
            )
    )
    private void rsmixin$checkSides(RectangularMultiblockControllerBase controller, Level world, int x, int y, int z) throws MultiblockValidationException {
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        String name = rsmixin$getBlockDisplayName(state);
        if (state.getBlock() == ModBlocks.BLOCK_MULTI_HEAT.get()) {
            return;
        }
        String expected = rsmixin$getExpectedHeat();
        throw rsmixin$createException(name, pos, "is invalid on face surface (expected " + expected + ")");
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;isBlockGoodForInterior(Lnet/minecraft/world/level/Level;III)V"
            )
    )
    private void rsmixin$checkInterior(RectangularMultiblockControllerBase controller, Level world, int x, int y, int z) throws MultiblockValidationException {
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        String name = rsmixin$getBlockDisplayName(state);
        if (state.isAir()
                || state.getBlock() == ModBlocks.BLOCK_MULTI_STORAGE.get()
                || state.getBlock() == ModBlocks.BLOCK_MULTI_CPU.get()) {
            return;
        }
        String expected = rsmixin$getExpectedInterior();
        throw rsmixin$createException(name, pos, "is invalid in interior (expected " + expected + ")");
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockTileEntityBase;isGoodForFrame()V"
            )
    )
    private void rsmixin$partFrame(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        RectangularMultiblockControllerBase controller = (RectangularMultiblockControllerBase) (Object) this;
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            tile.isGoodForFrame();
            return;
        }
        rsmixin$checkPartFrame(tile);
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockTileEntityBase;isGoodForTop()V"
            )
    )
    private void rsmixin$partTop(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        RectangularMultiblockControllerBase controller = (RectangularMultiblockControllerBase) (Object) this;
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            tile.isGoodForTop();
            return;
        }
        rsmixin$checkPartFace(tile);
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockTileEntityBase;isGoodForBottom()V"
            )
    )
    private void rsmixin$partBottom(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        RectangularMultiblockControllerBase controller = (RectangularMultiblockControllerBase) (Object) this;
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            tile.isGoodForBottom();
            return;
        }
        rsmixin$checkPartFace(tile);
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockTileEntityBase;isGoodForSides()V"
            )
    )
    private void rsmixin$partSides(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        RectangularMultiblockControllerBase controller = (RectangularMultiblockControllerBase) (Object) this;
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            tile.isGoodForSides();
            return;
        }
        rsmixin$checkPartFace(tile);
    }

    @Redirect(
            method = "isMachineWhole",
            remap = false,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockTileEntityBase;isGoodForInterior()V"
            )
    )
    private void rsmixin$partInterior(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        RectangularMultiblockControllerBase controller = (RectangularMultiblockControllerBase) (Object) this;
        if (!(controller instanceof MultiBlockCrafter && Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get())) {
            tile.isGoodForInterior();
            return;
        }
        rsmixin$checkPartInterior(tile);
    }
}
