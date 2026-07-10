package mctmods.rsmixin.mixin.common.rebornstorage;

import net.gigabit101.rebornstorage.core.multiblock.MultiblockControllerBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiblockControllerBase.class) public interface IMultiblockControllerBaseMixin {
    @Invoker(value = "getMinimumNumberOfBlocksForAssembledMachine", remap = false) int rsmixin$invokeMinBlocks();
}
