package mctmods.rsmixin.mixin.common.rebornstorage;

import net.gigabit101.rebornstorage.core.multiblock.MultiblockControllerBase;
import net.gigabit101.rebornstorage.core.multiblock.MultiblockValidationException;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiblockControllerBase.class) public interface IMultiblockControllerBaseMixin {
    @Invoker(value = "getMinimumNumberOfBlocksForAssembledMachine", remap = false) int rsmixin$invokeMinBlocks();

    @Invoker(value = "isBlockGoodForFrame", remap = false) void rsmixin$invokeGoodForFrame(Level world, int x, int y, int z) throws MultiblockValidationException;

    @Invoker(value = "isBlockGoodForTop", remap = false) void rsmixin$invokeGoodForTop(Level world, int x, int y, int z) throws MultiblockValidationException;

    @Invoker(value = "isBlockGoodForBottom", remap = false) void rsmixin$invokeGoodForBottom(Level world, int x, int y, int z) throws MultiblockValidationException;

    @Invoker(value = "isBlockGoodForSides", remap = false) void rsmixin$invokeGoodForSides(Level world, int x, int y, int z) throws MultiblockValidationException;

    @Invoker(value = "isBlockGoodForInterior", remap = false) void rsmixin$invokeGoodForInterior(Level world, int x, int y, int z) throws MultiblockValidationException;
}
