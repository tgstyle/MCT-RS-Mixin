package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.core.interfaces.IFluidTrackerTouch;
import mctmods.rsmixin.helper.refinedstorage.TrackerTouch;

import com.raoulvdberge.refinedstorage.api.storage.IStorageTracker.IStorageTrackerEntry;
import com.raoulvdberge.refinedstorage.apiimpl.storage.StorageTrackerFluid;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(value = StorageTrackerFluid.class, remap = false) public abstract class StorageTrackerFluidMixin implements IFluidTrackerTouch {
    @Shadow private Map<FluidStack, IStorageTrackerEntry> changes;
    @Shadow private Runnable listener;

    @Override public void rsmixin$touchFluid(FluidStack stack) { TrackerTouch.touch(changes, listener, stack, FluidStack::copy); }
}
