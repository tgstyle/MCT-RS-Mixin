package mctmods.rsmixin.mixin.client.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.integration.jei.GridRecipeTransferHandler;
import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.refinedmods.refinedstorage.screen.grid.stack.IGridStack;
import com.refinedmods.refinedstorage.screen.grid.stack.ItemGridStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(value = GridRecipeTransferHandler.class, remap = false) public abstract class GridRecipeTransferHandlerMixin {
    @Unique private static final int rsmixin$MAX_POSSIBILITIES_PER_SLOT = 16;

    @ModifyArg(method = "move", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/network/grid/GridTransferMessage;<init>(Ljava/util/List;)V")) private List<List<ItemStack>> rsmixin$capPossibilities(List<List<ItemStack>> inputs) {
        if (!Config.ENABLE_JEI_TRANSFER_LIMIT.get()) { return inputs; }
        Set<Item> available = new HashSet<>();
        if (Minecraft.getInstance().player != null) {
            for (ItemStack stack : Minecraft.getInstance().player.getInventory().items) {
                if (!stack.isEmpty()) { available.add(stack.getItem()); }
            }
        }
        if (Minecraft.getInstance().screen instanceof GridScreen gridScreen) {
            for (IGridStack gridStack : gridScreen.getView().getAllStacks()) {
                if (gridStack instanceof ItemGridStack itemGridStack) { available.add(itemGridStack.getStack().getItem()); }
            }
        }
        List<List<ItemStack>> capped = new ArrayList<>(inputs.size());
        for (List<ItemStack> possibilities : inputs) {
            if (possibilities.size() <= rsmixin$MAX_POSSIBILITIES_PER_SLOT) {
                capped.add(possibilities);
                continue;
            }
            List<ItemStack> kept = new ArrayList<>(rsmixin$MAX_POSSIBILITIES_PER_SLOT);
            for (ItemStack possibility : possibilities) {
                if (kept.size() >= rsmixin$MAX_POSSIBILITIES_PER_SLOT) { break; }
                if (kept.isEmpty() || available.contains(possibility.getItem())) { kept.add(possibility); }
            }
            for (ItemStack possibility : possibilities) {
                if (kept.size() >= rsmixin$MAX_POSSIBILITIES_PER_SLOT) { break; }
                if (!kept.contains(possibility)) { kept.add(possibility); }
            }
            capped.add(kept);
        }
        return capped;
    }
}
