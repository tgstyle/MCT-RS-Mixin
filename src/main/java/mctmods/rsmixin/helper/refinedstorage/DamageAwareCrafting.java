package mctmods.rsmixin.helper.refinedstorage;

import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DamageAwareCrafting {
    private DamageAwareCrafting() {}

    public static boolean isReusableMatch(ItemStack key, ItemStack candidate) {
        if (key.isEmpty() || candidate.isEmpty()) { return false; }
        if (!key.isDamageableItem() || key.getItem() != candidate.getItem()) { return false; }
        return nbtEqualIgnoringDamage(key, candidate);
    }

    private static boolean nbtEqualIgnoringDamage(ItemStack a, ItemStack b) {
        CompoundTag tagA = a.getTag() == null ? new CompoundTag() : a.getTag().copy();
        CompoundTag tagB = b.getTag() == null ? new CompoundTag() : b.getTag().copy();
        tagA.remove("Damage");
        tagB.remove("Damage");
        return tagA.equals(tagB);
    }

    public static ItemStack findInStackList(IStackList<ItemStack> list, ItemStack key) {
        ItemStack best = null;
        for (StackListEntry<ItemStack> entry : list.getStacks()) {
            ItemStack candidate = entry.getStack();
            if (!isReusableMatch(key, candidate)) { continue; }
            if (best == null || candidate.getDamageValue() > best.getDamageValue()) { best = candidate; }
        }
        return best;
    }

    public static List<ItemStack> collectVariants(Iterable<ItemStack> stacks, ItemStack key) {
        List<ItemStack> variants = new ArrayList<>();
        for (ItemStack candidate : stacks) {
            if (isReusableMatch(key, candidate)) { variants.add(candidate); }
        }
        variants.sort(Comparator.comparingInt(ItemStack::getDamageValue).reversed());
        return variants;
    }

    public static List<ItemStack> collectVariantsFromEntries(Iterable<StackListEntry<ItemStack>> entries, ItemStack key) {
        List<ItemStack> variants = new ArrayList<>();
        for (StackListEntry<ItemStack> entry : entries) {
            if (isReusableMatch(key, entry.getStack())) { variants.add(entry.getStack()); }
        }
        variants.sort(Comparator.comparingInt(ItemStack::getDamageValue).reversed());
        return variants;
    }
}
