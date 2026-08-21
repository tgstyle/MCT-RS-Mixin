package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;
import mctmods.rsmixin.core.interfaces.IProcessing;
import mctmods.rsmixin.helper.refinedstorage.ProcessingStateAccess;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDisk;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.task.CraftingTask;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = CraftingTask.class, remap = false) public abstract class CraftingTaskMixin {
    @Shadow private List<?> processing;
    @Shadow private IStorageDisk<ItemStack> internalStorage;
    @Shadow private IStorageDisk<FluidStack> internalFluidStorage;
    @Shadow private INetwork network;
    @Unique private Map<Item, List<IProcessing>> rsmixin$itemIndex;
    @Unique private Map<Fluid, List<IProcessing>> rsmixin$fluidIndex;
    @Unique private static final Logger rsmixin$LOGGER = LogManager.getLogger(RSMixin.MODID);

    @Shadow private static boolean insertIntoInventory(IItemHandler dest, Deque<ItemStack> stacks, Action action) { throw new AssertionError(); }

    @Redirect(method = "updateProcessing", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/apiimpl/autocrafting/task/CraftingTask;insertIntoInventory(Lnet/minecraftforge/items/IItemHandler;Ljava/util/Deque;Lcom/raoulvdberge/refinedstorage/api/util/Action;)Z", ordinal = 1)) private boolean rsmixin$insertWithRefund(IItemHandler dest, Deque<ItemStack> stacks, Action action) {
        if (!Config.enableProcessingVoidGuard) { return insertIntoInventory(dest, stacks, action); }

        List<ItemStack> leftovers = new ArrayList<>();
        rsmixin$insertCollectingLeftovers(dest, stacks, leftovers);
        if (leftovers.isEmpty()) { return true; }

        for (ItemStack leftover : leftovers) {
            ItemStack remainder = network.insertItem(leftover, leftover.getCount(), Action.PERFORM);
            if (remainder != null && !remainder.isEmpty()) { internalStorage.insert(remainder, remainder.getCount(), Action.PERFORM); }
        }
        rsmixin$LOGGER.warn("RSMixin: {} did not accept all items for a processing craft; the rest was returned to the network instead of being voided", dest);
        return true;
    }

    @Unique private static void rsmixin$insertCollectingLeftovers(IItemHandler dest, Deque<ItemStack> stacks, List<ItemStack> leftovers) {
        ItemStack current = stacks.poll();

        if (dest == null) {
            while (current != null) {
                leftovers.add(current);
                current = stacks.poll();
            }
            return;
        }

        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < dest.getSlots(); ++i) { availableSlots.add(i); }

        while (current != null && !availableSlots.isEmpty()) {
            ItemStack remainder = ItemStack.EMPTY;

            for (int i = 0; i < availableSlots.size(); ++i) {
                int slot = availableSlots.get(i);
                remainder = dest.insertItem(slot, current.copy(), false);
                if (remainder.isEmpty() || current.getCount() != remainder.getCount()) {
                    availableSlots.remove(i);
                    break;
                }
            }

            if (remainder.isEmpty()) { current = stacks.poll(); }
            else if (current.getCount() == remainder.getCount()) { break; }
            else { current = remainder; }
        }

        while (current != null) {
            if (!current.isEmpty()) { leftovers.add(current); }
            current = stacks.poll();
        }
    }

    @Redirect(method = "updateProcessing", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/capability/IFluidHandler;fill(Lnet/minecraftforge/fluids/FluidStack;Z)I", ordinal = 1)) private int rsmixin$fillWithRefund(IFluidHandler handler, FluidStack resource, boolean doFill) {
        if (!Config.enableProcessingVoidGuard) { return handler.fill(resource, doFill); }
        if (resource == null) { return 0; }

        int filled = handler.fill(resource, doFill);
        if (filled < resource.amount) {
            FluidStack remainder = network.insertFluid(resource, resource.amount - filled, Action.PERFORM);
            if (remainder != null && remainder.amount > 0) { internalFluidStorage.insert(remainder, remainder.amount, Action.PERFORM); }
            rsmixin$LOGGER.warn("RSMixin: {} did not accept {} mB of {} for a processing craft; it was returned to the network instead of being voided", handler, resource.amount - filled, resource.getFluid().getName());
            return resource.amount;
        }
        return filled;
    }

    @Unique private void rsmixin$buildIndex() {
        rsmixin$itemIndex = new HashMap<>();
        rsmixin$fluidIndex = new HashMap<>();
        for (Object o : processing) {
            IProcessing p = (IProcessing) o;
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

        List<IProcessing> candidates = rsmixin$itemIndex.get(stack.getItem());
        if (candidates == null) {
            cir.setReturnValue(size);
            return;
        }

        for (IProcessing p : candidates) {
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

        List<IProcessing> candidates = rsmixin$fluidIndex.get(stack.getFluid());
        if (candidates == null) {
            cir.setReturnValue(size);
            return;
        }

        for (IProcessing p : candidates) {
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
