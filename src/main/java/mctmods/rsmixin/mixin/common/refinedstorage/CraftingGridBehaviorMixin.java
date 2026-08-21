package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.refinedmods.refinedstorage.apiimpl.network.grid.CraftingGridBehavior;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;

@Mixin(value = CraftingGridBehavior.class, remap = false) public abstract class CraftingGridBehaviorMixin {
    @Inject(method = "filterDuplicateStacks", at = @At("RETURN")) private void rsmixin$clampToExtractable(INetwork network, CraftingContainer matrix, IStackList<ItemStack> availableItems, CallbackInfo ci) {
        if (!Config.ENABLE_OVERSTACK_EXTRACTION_FIX.get()) { return; }
        for (StackListEntry<ItemStack> entry : new ArrayList<>(availableItems.getStacks())) {
            ItemStack stack = entry.getStack();
            ItemStack extractable = network.extractItem(stack, stack.getCount(), Action.SIMULATE);
            int shortfall = stack.getCount() - extractable.getCount();
            if (shortfall > 0) { availableItems.remove(stack, shortfall); }
        }
    }
}
