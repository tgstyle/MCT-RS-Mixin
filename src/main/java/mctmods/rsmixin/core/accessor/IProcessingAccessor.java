package mctmods.rsmixin.core.accessor;

import com.raoulvdberge.refinedstorage.api.util.IStackList;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public interface IProcessingAccessor {
    boolean rsmixin$isExtractedAll();

    void rsmixin$setProcessed();

    boolean rsmixin$isRoot();

    IStackList<ItemStack> rsmixin$itemsToReceive();

    IStackList<FluidStack> rsmixin$fluidsToReceive();
}
