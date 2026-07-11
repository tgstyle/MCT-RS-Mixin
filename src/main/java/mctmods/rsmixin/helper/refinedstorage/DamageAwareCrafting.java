package mctmods.rsmixin.helper.refinedstorage;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import net.minecraft.core.NonNullList;
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

    public static int computeReuseCredit(ItemStack input, int perCraft, int qty, ICraftingPattern pattern, NonNullList<ItemStack> recipe, IStackList<ItemStack> results, IStackList<ItemStack> storage) {
        if (qty <= 1 || perCraft <= 0 || !input.isDamageableItem() || input.getMaxDamage() <= 1) { return 0; }

        long demand = (long) perCraft * (long) qty;
        if (demand > Integer.MAX_VALUE) { return 0; }

        int damagePerCraft = measureDamagePerCraft(input, perCraft, pattern, recipe);
        if (damagePerCraft < 0) { return 0; }

        List<ItemStack> owned = new ArrayList<>();
        for (StackListEntry<ItemStack> entry : results.getStacks()) {
            if (isReusableMatch(input, entry.getStack())) { owned.add(entry.getStack()); }
        }
        for (StackListEntry<ItemStack> entry : storage.getStacks()) {
            if (isReusableMatch(input, entry.getStack())) { owned.add(entry.getStack()); }
        }
        owned.sort(Comparator.comparingInt(ItemStack::getDamageValue).reversed());

        long uses = 0;
        long toolsTaken = 0;
        for (ItemStack tool : owned) {
            long usesEach = usableCrafts(tool.getMaxDamage(), tool.getDamageValue(), damagePerCraft, demand);
            for (int i = 0; i < tool.getCount() && uses < demand; i++) {
                uses += usesEach;
                toolsTaken++;
            }
            if (uses >= demand) { break; }
        }
        long freshUses = usableCrafts(input.getMaxDamage(), input.getDamageValue(), damagePerCraft, demand);
        while (uses < demand) {
            uses += freshUses;
            toolsTaken++;
        }

        long externalNeed = Math.max(toolsTaken, perCraft);
        long credit = demand - externalNeed;
        if (credit <= 0) { return 0; }
        return (int) credit;
    }

    private static long usableCrafts(int maxDamage, int currentDamage, int damagePerCraft, long cap) {
        if (damagePerCraft <= 0) { return cap; }
        long durabilityLeft = maxDamage - currentDamage;
        if (durabilityLeft <= 0) { return 1; }
        return Math.max(1, (durabilityLeft + damagePerCraft - 1) / damagePerCraft);
    }

    private static int measureDamagePerCraft(ItemStack input, int perCraft, ICraftingPattern pattern, NonNullList<ItemStack> recipe) {
        int inputDamage = input.getDamageValue();
        int worstObserved = -1;
        for (int sample = 0; sample < 3; sample++) {
            int count = 0;
            int worst = inputDamage;
            for (ItemStack byproduct : pattern.getByproducts(recipe)) {
                if (!isReusableMatch(input, byproduct)) { continue; }
                count += byproduct.getCount();
                if (byproduct.getDamageValue() > worst) { worst = byproduct.getDamageValue(); }
            }
            if (count < perCraft) { return -1; }
            int observed = worst - inputDamage;
            if (observed > worstObserved) { worstObserved = observed; }
        }
        return Math.max(0, worstObserved);
    }

    public static void debitDamageAwareVariants(IStackList<ItemStack> list, ItemStack template, int amount) {
        int remaining = amount;
        List<ItemStack> variants = collectVariantsFromEntries(new ArrayList<>(list.getStacks()), template);
        for (ItemStack variant : variants) {
            if (remaining <= 0) { break; }
            int take = Math.min(remaining, variant.getCount());
            list.remove(variant, take);
            remaining -= take;
        }
    }
}
