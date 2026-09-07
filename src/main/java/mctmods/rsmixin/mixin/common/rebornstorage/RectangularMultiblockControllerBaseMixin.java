package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.helper.rebornstorage.MultiblockFault;
import mctmods.rsmixin.helper.rebornstorage.MultiblockFaultHolder;
import mctmods.rsmixin.helper.rebornstorage.MultiblockRegion;
import mctmods.rsmixin.helper.rebornstorage.MultiblockReport;
import mctmods.rsmixin.helper.rebornstorage.MultiblockRules;

import net.gigabit101.rebornstorage.core.multiblock.MultiblockControllerBase;
import net.gigabit101.rebornstorage.init.ModBlocks;
import net.gigabit101.rebornstorage.core.multiblock.MultiblockValidationException;
import net.gigabit101.rebornstorage.core.multiblock.rectangular.RectangularMultiblockControllerBase;
import net.gigabit101.rebornstorage.core.multiblock.rectangular.RectangularMultiblockTileEntityBase;
import net.gigabit101.rebornstorage.multiblocks.MultiBlockCrafter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"ConstantValue", "ConstantConditions"}) @Mixin(RectangularMultiblockControllerBase.class) public abstract class RectangularMultiblockControllerBaseMixin implements MultiblockFaultHolder {
    @Unique private List<MultiblockFault> rsmixin$faults;
    @Unique private BlockPos.MutableBlockPos rsmixin$scratch;
    @Unique private int rsmixin$scanCpu;
    @Unique private int rsmixin$scanStorage;
    @Unique private int rsmixin$cpuCount;
    @Unique private int rsmixin$storageCount;

    @Override public List<MultiblockFault> rsmixin$faults() {
        return this.rsmixin$faults == null ? Collections.emptyList() : Collections.unmodifiableList(this.rsmixin$faults);
    }

    @Override public int rsmixin$cpuCount() {
        return this.rsmixin$cpuCount;
    }

    @Override public int rsmixin$storageCount() {
        return this.rsmixin$storageCount;
    }

    @Unique private void rsmixin$tally(BlockState state) {
        if (state.is(ModBlocks.BLOCK_MULTI_CPU.get())) { this.rsmixin$scanCpu++; }
        if (state.is(ModBlocks.BLOCK_MULTI_STORAGE.get())) { this.rsmixin$scanStorage++; }
    }

    @Unique private boolean rsmixin$inactive() {
        return !MultiBlockCrafter.class.isInstance(this) || !Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get();
    }

    @Unique private void rsmixin$record(MultiblockRegion region, BlockPos pos, BlockState state) {
        if (this.rsmixin$faults == null) { this.rsmixin$faults = new ArrayList<>(); }
        if (this.rsmixin$faults.size() < MultiblockReport.MAX_FAULTS) {
            this.rsmixin$faults.add(new MultiblockFault(pos, region, state));
        }
    }

    @Unique private void rsmixin$checkAt(MultiblockRegion region, Level world, int x, int y, int z) {
        if (this.rsmixin$scratch == null) { this.rsmixin$scratch = new BlockPos.MutableBlockPos(); }
        BlockPos pos = this.rsmixin$scratch.set(x, y, z);
        BlockState state = world.getBlockState(pos);
        rsmixin$tally(state);
        if (!MultiblockRules.allows(region, state)) { rsmixin$record(region, pos, state); }
    }

    @Unique private void rsmixin$checkPart(MultiblockRegion region, RectangularMultiblockTileEntityBase tile) {
        BlockPos pos = tile.getBlockPos();
        Level world = tile.getLevel();
        BlockState state = world == null ? tile.getBlockState() : world.getBlockState(pos);
        rsmixin$tally(state);
        if (!MultiblockRules.allows(region, state)) { rsmixin$record(region, pos, state); }
    }

    @Inject(method = "isMachineWhole", remap = false, at = @At("HEAD")) private void rsmixin$beginScan(CallbackInfo ci) {
        if (this.rsmixin$faults != null) { this.rsmixin$faults.clear(); }
        this.rsmixin$scanCpu = 0;
        this.rsmixin$scanStorage = 0;
        this.rsmixin$cpuCount = -1;
        this.rsmixin$storageCount = -1;
    }

    @Inject(method = "isMachineWhole", remap = false, at = @At("TAIL")) private void rsmixin$endScan(CallbackInfo ci) throws MultiblockValidationException {
        if (rsmixin$inactive()) { return; }
        MultiblockControllerBase self = (MultiblockControllerBase) (Object) this;
        this.rsmixin$cpuCount = this.rsmixin$scanCpu;
        this.rsmixin$storageCount = this.rsmixin$scanStorage;

        if (Config.ENABLE_DEBUG_LOGGING.get()) {
            RSMixin.LOGGER.info("RebornStorage scan ref={} box {} .. {}, cpu={}, storage={}, faults={}",
                    self.getReferenceCoord(), self.getMinimumCoord(), self.getMaximumCoord(),
                    this.rsmixin$scanCpu, this.rsmixin$scanStorage,
                    this.rsmixin$faults == null ? 0 : this.rsmixin$faults.size());
            if (this.rsmixin$faults != null) {
                for (MultiblockFault fault : this.rsmixin$faults) {
                    RSMixin.LOGGER.info("  fault {} {} found={}", fault.region(), fault.pos(),
                            ForgeRegistries.BLOCKS.getKey(fault.found().getBlock()));
                }
            }
        }

        List<MultiblockFault> faults = this.rsmixin$faults == null ? List.of() : this.rsmixin$faults;
        if (!MultiblockReport.incomplete(faults)) { return; }
        throw new MultiblockValidationException(MultiblockReport.plain(faults,
                self.getMinimumCoord(), self.getMaximumCoord(), self.getReferenceCoord(),
                this.rsmixin$scanCpu, this.rsmixin$scanStorage));
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;getMinimumNumberOfBlocksForAssembledMachine()I")) private int rsmixin$minBlocks(RectangularMultiblockControllerBase controller) {
        if (rsmixin$inactive()) { return ((IMultiblockControllerBaseMixin) controller).rsmixin$invokeMinBlocks(); }
        return 1;
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;isBlockGoodForFrame(Lnet/minecraft/world/level/Level;III)V")) private void rsmixin$checkFrame(RectangularMultiblockControllerBase controller, Level world, int x, int y, int z) throws MultiblockValidationException {
        if (rsmixin$inactive()) { ((IMultiblockControllerBaseMixin) controller).rsmixin$invokeGoodForFrame(world, x, y, z); return; }
        rsmixin$checkAt(MultiblockRegion.FRAME, world, x, y, z);
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;isBlockGoodForTop(Lnet/minecraft/world/level/Level;III)V")) private void rsmixin$checkTop(RectangularMultiblockControllerBase controller, Level world, int x, int y, int z) throws MultiblockValidationException {
        if (rsmixin$inactive()) { ((IMultiblockControllerBaseMixin) controller).rsmixin$invokeGoodForTop(world, x, y, z); return; }
        rsmixin$checkAt(MultiblockRegion.FACE, world, x, y, z);
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;isBlockGoodForBottom(Lnet/minecraft/world/level/Level;III)V")) private void rsmixin$checkBottom(RectangularMultiblockControllerBase controller, Level world, int x, int y, int z) throws MultiblockValidationException {
        if (rsmixin$inactive()) { ((IMultiblockControllerBaseMixin) controller).rsmixin$invokeGoodForBottom(world, x, y, z); return; }
        rsmixin$checkAt(MultiblockRegion.FACE, world, x, y, z);
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;isBlockGoodForSides(Lnet/minecraft/world/level/Level;III)V")) private void rsmixin$checkSides(RectangularMultiblockControllerBase controller, Level world, int x, int y, int z) throws MultiblockValidationException {
        if (rsmixin$inactive()) { ((IMultiblockControllerBaseMixin) controller).rsmixin$invokeGoodForSides(world, x, y, z); return; }
        rsmixin$checkAt(MultiblockRegion.FACE, world, x, y, z);
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockControllerBase;isBlockGoodForInterior(Lnet/minecraft/world/level/Level;III)V")) private void rsmixin$checkInterior(RectangularMultiblockControllerBase controller, Level world, int x, int y, int z) throws MultiblockValidationException {
        if (rsmixin$inactive()) { ((IMultiblockControllerBaseMixin) controller).rsmixin$invokeGoodForInterior(world, x, y, z); return; }
        rsmixin$checkAt(MultiblockRegion.INTERIOR, world, x, y, z);
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockTileEntityBase;isGoodForFrame()V")) private void rsmixin$partFrame(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        if (rsmixin$inactive()) { tile.isGoodForFrame(); return; }
        rsmixin$checkPart(MultiblockRegion.FRAME, tile);
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockTileEntityBase;isGoodForTop()V")) private void rsmixin$partTop(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        if (rsmixin$inactive()) { tile.isGoodForTop(); return; }
        rsmixin$checkPart(MultiblockRegion.FACE, tile);
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockTileEntityBase;isGoodForBottom()V")) private void rsmixin$partBottom(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        if (rsmixin$inactive()) { tile.isGoodForBottom(); return; }
        rsmixin$checkPart(MultiblockRegion.FACE, tile);
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockTileEntityBase;isGoodForSides()V")) private void rsmixin$partSides(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        if (rsmixin$inactive()) { tile.isGoodForSides(); return; }
        rsmixin$checkPart(MultiblockRegion.FACE, tile);
    }

    @Redirect(method = "isMachineWhole", remap = false, at = @At(value = "INVOKE", target = "Lnet/gigabit101/rebornstorage/core/multiblock/rectangular/RectangularMultiblockTileEntityBase;isGoodForInterior()V")) private void rsmixin$partInterior(RectangularMultiblockTileEntityBase tile) throws MultiblockValidationException {
        if (rsmixin$inactive()) { tile.isGoodForInterior(); return; }
        rsmixin$checkPart(MultiblockRegion.INTERIOR, tile);
    }
}
