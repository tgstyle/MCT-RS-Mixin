package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.core.accessor.IProcessingAccessor;
import mctmods.rsmixin.helper.refinedstorage.ProcessingStateAccess;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDisk;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.task.CraftingTask;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = CraftingTask.class, remap = false) public abstract class CraftingTaskMixin {
    @Shadow private List<?> processing;
    @Shadow private IStorageDisk<ItemStack> internalStorage;
    @Shadow private IStorageDisk<FluidStack> internalFluidStorage;
    @Shadow private INetwork network;
    @Unique private Map<Item, List<IProcessingAccessor>> rsmixin$itemIndex;
    @Unique private Map<Fluid, List<IProcessingAccessor>> rsmixin$fluidIndex;

    @Unique private void rsmixin$buildIndex() {
        rsmixin$itemIndex = new HashMap<>();
        rsmixin$fluidIndex = new HashMap<>();
        for (Object o : processing) {
            IProcessingAccessor p = (IProcessingAccessor) o;
            for (ItemStack stack : p.rsmixin$itemsToReceive().getStacks()) {
                rsmixin$itemIndex.computeIfAbsent(stack.getItem(), k -> new ArrayList<>()).add(p);
            }
            for (FluidStack stack : p.rsmixin$fluidsToReceive().getStacks()) {
                rsmixin$fluidIndex.computeIfAbsent(stack.getFluid(), k -> new ArrayList<>()).add(p);
            }
        }
    }

    @Inject(method = "onTrackedInsert(Lnet/minecraft/item/ItemStack;I)I", at = @At("HEAD"), cancellable = true) private void rsmixin$indexedItemInsert(ItemStack stack, int size, CallbackInfoReturnable<Integer> cir) {
        if (!Config.enableTrackedInsertIndex || ProcessingStateAccess.isUnAvailable()) { return; }
        if (rsmixin$itemIndex == null) { rsmixin$buildIndex(); }

        List<IProcessingAccessor> candidates = rsmixin$itemIndex.get(stack.getItem());
        if (candidates == null) {
            cir.setReturnValue(size);
            return;
        }

        for (IProcessingAccessor p : candidates) {
            if (p.rsmixin$isExtractedAll()) {
                ItemStack content = p.rsmixin$itemsToReceive().get(stack);
                if (content == null) { continue; }

                int needed = content.getCount();
                if (needed > size) { needed = size; }

                p.rsmixin$itemsToReceive().remove(stack, needed);
                size -= needed;

                if (p.rsmixin$itemsToReceive().isEmpty() && p.rsmixin$fluidsToReceive().isEmpty()) { p.rsmixin$setProcessed(); }

                if (p.rsmixin$isRoot()) {
                    ItemStack remainder = network.insertItem(stack, needed, Action.PERFORM);
                    if (remainder != null) { internalStorage.insert(stack, needed, Action.PERFORM); }
                }
                else { internalStorage.insert(stack, needed, Action.PERFORM); }

                if (size == 0) {
                    cir.setReturnValue(0);
                    return;
                }
            }
        }

        cir.setReturnValue(size);
    }

    @Inject(method = "onTrackedInsert(Lnet/minecraftforge/fluids/FluidStack;I)I", at = @At("HEAD"), cancellable = true) private void rsmixin$indexedFluidInsert(FluidStack stack, int size, CallbackInfoReturnable<Integer> cir) {
        if (!Config.enableTrackedInsertIndex || ProcessingStateAccess.isUnAvailable()) { return; }
        if (rsmixin$fluidIndex == null) { rsmixin$buildIndex(); }

        List<IProcessingAccessor> candidates = rsmixin$fluidIndex.get(stack.getFluid());
        if (candidates == null) {
            cir.setReturnValue(size);
            return;
        }

        for (IProcessingAccessor p : candidates) {
            if (p.rsmixin$isExtractedAll()) {
                FluidStack content = p.rsmixin$fluidsToReceive().get(stack);
                if (content == null) { continue; }

                int needed = content.amount;
                if (needed > size) { needed = size; }

                p.rsmixin$fluidsToReceive().remove(stack, needed);
                size -= needed;

                if (p.rsmixin$itemsToReceive().isEmpty() && p.rsmixin$fluidsToReceive().isEmpty()) { p.rsmixin$setProcessed(); }

                if (p.rsmixin$isRoot()) {
                    FluidStack remainder = network.insertFluid(stack, needed, Action.PERFORM);
                    if (remainder != null) { internalFluidStorage.insert(stack, needed, Action.PERFORM); }
                }
                else { internalFluidStorage.insert(stack, needed, Action.PERFORM); }

                if (size == 0) {
                    cir.setReturnValue(0);
                    return;
                }
            }
        }

        cir.setReturnValue(size);
    }
}
