package mctmods.rsmixin.core.interfaces;

import com.raoulvdberge.refinedstorage.api.util.IStackList;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface IProcessing {
    boolean rsmixin$isExtractedAll();

    void rsmixin$setProcessed();

    boolean rsmixin$isRoot();

    IStackList<ItemStack> rsmixin$itemsToReceive();

    IStackList<FluidStack> rsmixin$fluidsToReceive();
}
