package mctmods.rsmixin.helper.rebornstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record MultiblockFault(BlockPos pos, MultiblockRegion region, BlockState found) {

    public MultiblockFault(BlockPos pos, MultiblockRegion region, BlockState found) {
        this.pos = pos.immutable();
        this.region = region;
        this.found = found;
    }

    public boolean missing() {
        return found.isAir();
    }
}
