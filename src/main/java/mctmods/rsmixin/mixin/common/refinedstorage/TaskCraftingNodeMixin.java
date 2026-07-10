package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.helper.refinedstorage.DamageAwareCrafting;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.IoUtil;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.node.CraftingNode;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = CraftingNode.class, remap = false) public abstract class TaskCraftingNodeMixin {
    @Unique private List<ItemStack> rsmixin$lastExtracted;

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/apiimpl/autocrafting/task/v6/IoUtil;extractFromInternalItemStorage(Ljava/util/List;Lcom/refinedmods/refinedstorage/api/storage/disk/IStorageDisk;Lcom/refinedmods/refinedstorage/api/util/Action;)Ljava/util/List;")) private List<ItemStack> rsmixin$captureExtracted(List<ItemStack> list, IStorageDisk<ItemStack> storage, Action action) {
        List<ItemStack> result = IoUtil.extractFromInternalItemStorage(list, storage, action);
        if (Config.ENABLE_DAMAGEABLE_INPUT_REUSE.get() && action == Action.PERFORM) { rsmixin$lastExtracted = result; }
        return result;
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingPattern;getByproducts(Lnet/minecraft/core/NonNullList;)Lnet/minecraft/core/NonNullList;")) private NonNullList<ItemStack> rsmixin$realByproducts(ICraftingPattern pattern, NonNullList<ItemStack> recipe) {
        if (!Config.ENABLE_DAMAGEABLE_INPUT_REUSE.get() || rsmixin$lastExtracted == null) { return pattern.getByproducts(recipe); }

        List<ItemStack> pool = new ArrayList<>();
        for (ItemStack extractedStack : rsmixin$lastExtracted) {
            if (extractedStack.isDamageableItem()) { pool.add(extractedStack.copy()); }
        }
        rsmixin$lastExtracted = null;
        if (pool.isEmpty()) { return pattern.getByproducts(recipe); }

        NonNullList<ItemStack> took = NonNullList.create();
        for (ItemStack slot : recipe) {
            ItemStack replacement = null;
            if (!slot.isEmpty() && slot.isDamageableItem()) {
                for (ItemStack poolStack : pool) {
                    if (poolStack.getCount() > 0 && DamageAwareCrafting.isReusableMatch(slot, poolStack)) {
                        replacement = poolStack.copy();
                        replacement.setCount(1);
                        poolStack.shrink(1);
                        break;
                    }
                }
            }
            took.add(replacement != null ? replacement : slot);
        }
        return pattern.getByproducts(took);
    }
}
