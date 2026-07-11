package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.helper.refinedstorage.DamageAwareCrafting;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.CraftingPatternInputs;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.calculator.CraftingCalculator;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.node.CraftingNode;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.node.Node;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Mixin(value = CraftingCalculator.class, remap = false) public abstract class CraftingCalculatorMixin {
    @Unique private final Deque<List<ItemStack>> rsmixin$pendingDebits = new ArrayDeque<>();

    @SuppressWarnings({"unchecked", "rawtypes"}) @Redirect(method = "calculateForItems", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/api/util/IStackList;get(Ljava/lang/Object;)Ljava/lang/Object;")) private Object rsmixin$damageAwareGet(IStackList list, Object stack) {
        Object exact = list.get(stack);
        if (exact != null) { return exact; }
        if (!Config.ENABLE_DAMAGEABLE_INPUT_REUSE.get()) { return null; }
        if (!(stack instanceof ItemStack key) || !key.isDamageableItem()) { return null; }
        return DamageAwareCrafting.findInStackList((IStackList<ItemStack>) list, key);
    }

    @Inject(method = "calculateForItems", at = @At("HEAD")) private void rsmixin$applyReuseCredit(int qty, IStackList<ItemStack> storageSource, IStackList<FluidStack> fluidStorageSource, IStackList<ItemStack> results, IStackList<FluidStack> fluidResults, IStackList<ItemStack> itemsToExtract, CraftingPatternInputs inputs, Node node, CallbackInfo ci) {
        List<ItemStack> debits = new ArrayList<>();
        rsmixin$pendingDebits.push(debits);

        if (!Config.ENABLE_DAMAGEABLE_INPUT_REUSE.get() || !(node instanceof CraftingNode)) { return; }

        for (CraftingPatternInputs.Ingredient<ItemStack> ingredient : inputs.getItemIngredients()) {
            for (ItemStack input : ingredient.getInputs()) {
                int credit = DamageAwareCrafting.computeReuseCredit(input, ingredient.getCount(), qty, node.getPattern(), inputs.getRecipe(), results, storageSource);
                if (credit <= 0) { continue; }
                results.add(input, credit);
                ItemStack debit = input.copy();
                debit.setCount(credit);
                debits.add(debit);
                break;
            }
        }
    }

    @Inject(method = "calculateInternal", at = @At("RETURN")) private void rsmixin$settleReuseCredit(int qty, IStackList<ItemStack> storageSource, IStackList<FluidStack> fluidStorageSource, IStackList<ItemStack> results, IStackList<FluidStack> fluidResults, ICraftingPattern pattern, boolean root, CallbackInfo ci) {
        List<ItemStack> debits = rsmixin$pendingDebits.poll();
        if (debits == null) { return; }
        for (ItemStack debit : debits) { DamageAwareCrafting.debitDamageAwareVariants(results, debit, debit.getCount()); }
    }
}
