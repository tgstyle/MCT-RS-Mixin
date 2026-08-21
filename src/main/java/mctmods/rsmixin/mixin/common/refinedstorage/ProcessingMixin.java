package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.core.interfaces.IProcessing;
import mctmods.rsmixin.helper.refinedstorage.ProcessingStateAccess;

import com.raoulvdberge.refinedstorage.api.util.IStackList;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.raoulvdberge.refinedstorage.apiimpl.autocrafting.task.Processing", remap = false) public abstract class ProcessingMixin implements IProcessing {
    @Shadow private boolean root;
    @Shadow private IStackList<ItemStack> itemsToReceive;
    @Shadow private IStackList<FluidStack> fluidsToReceive;

    @Override public boolean rsmixin$isExtractedAll() { return ProcessingStateAccess.isExtractedAll(this); }

    @Override public void rsmixin$setProcessed() { ProcessingStateAccess.setProcessed(this); }

    @Override public boolean rsmixin$isRoot() { return root; }

    @Override public IStackList<ItemStack> rsmixin$itemsToReceive() { return itemsToReceive; }

    @Override public IStackList<FluidStack> rsmixin$fluidsToReceive() { return fluidsToReceive; }
}
