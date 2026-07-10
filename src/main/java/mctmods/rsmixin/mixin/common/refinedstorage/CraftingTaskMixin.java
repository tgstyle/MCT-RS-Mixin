package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.CraftingTask;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.node.Node;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.node.NodeList;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.node.ProcessingNode;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
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
    @Shadow @Final private NodeList nodes;
    @Shadow @Final private IStorageDisk<ItemStack> internalStorage;
    @Shadow @Final private IStorageDisk<FluidStack> internalFluidStorage;
    @Shadow @Final private INetwork network;
    @Unique private Map<Item, List<ProcessingNode>> rsmixin$itemIndex;
    @Unique private Map<Fluid, List<ProcessingNode>> rsmixin$fluidIndex;

    @Unique private void rsmixin$buildIndex() {
        rsmixin$itemIndex = new HashMap<>();
        rsmixin$fluidIndex = new HashMap<>();
        for (Node node : nodes.all()) {
            if (!(node instanceof ProcessingNode processing)) { continue; }
            for (StackListEntry<ItemStack> entry : processing.getSingleItemSetToReceive().getStacks()) {
                rsmixin$itemIndex.computeIfAbsent(entry.getStack().getItem(), k -> new ArrayList<>()).add(processing);
            }
            for (StackListEntry<FluidStack> entry : processing.getSingleFluidSetToReceive().getStacks()) {
                rsmixin$fluidIndex.computeIfAbsent(entry.getStack().getFluid(), k -> new ArrayList<>()).add(processing);
            }
        }
    }

    @Inject(method = "onTrackedInsert(Lnet/minecraft/world/item/ItemStack;I)I", at = @At("HEAD"), cancellable = true) private void rsmixin$indexedItemInsert(ItemStack stack, int size, CallbackInfoReturnable<Integer> cir) {
        if (!Config.ENABLE_TRACKED_INSERT_INDEX.get()) { return; }
        if (rsmixin$itemIndex == null) { rsmixin$buildIndex(); }

        List<ProcessingNode> candidates = rsmixin$itemIndex.get(stack.getItem());
        if (candidates == null) {
            cir.setReturnValue(size);
            return;
        }

        for (ProcessingNode processing : candidates) {
            int needed = processing.getNeeded(stack);
            if (needed <= 0) { continue; }
            if (needed > size) { needed = size; }

            processing.markReceived(stack, needed);
            size -= needed;

            if (!processing.isRoot()) { internalStorage.insert(stack, needed, Action.PERFORM); }
            else {
                ItemStack remainder = network.insertItem(stack, needed, Action.PERFORM);
                internalStorage.insert(remainder, remainder.getCount(), Action.PERFORM);
            }

            network.getCraftingManager().onTaskChanged();

            if (size == 0) {
                cir.setReturnValue(0);
                return;
            }
        }

        cir.setReturnValue(size);
    }

    @Inject(method = "onTrackedInsert(Lnet/minecraftforge/fluids/FluidStack;I)I", at = @At("HEAD"), cancellable = true) private void rsmixin$indexedFluidInsert(FluidStack stack, int size, CallbackInfoReturnable<Integer> cir) {
        if (!Config.ENABLE_TRACKED_INSERT_INDEX.get()) { return; }
        if (rsmixin$fluidIndex == null) { rsmixin$buildIndex(); }

        List<ProcessingNode> candidates = rsmixin$fluidIndex.get(stack.getFluid());
        if (candidates == null) {
            cir.setReturnValue(size);
            return;
        }

        for (ProcessingNode processing : candidates) {
            int needed = processing.getNeeded(stack);
            if (needed <= 0) { continue; }
            if (needed > size) { needed = size; }

            processing.markReceived(stack, needed);
            size -= needed;

            if (!processing.isRoot()) { internalFluidStorage.insert(stack, needed, Action.PERFORM); }
            else {
                FluidStack remainder = network.insertFluid(stack, needed, Action.PERFORM);
                internalFluidStorage.insert(remainder, remainder.getAmount(), Action.PERFORM);
            }

            network.getCraftingManager().onTaskChanged();

            if (size == 0) {
                cir.setReturnValue(0);
                return;
            }
        }

        cir.setReturnValue(size);
    }
}
