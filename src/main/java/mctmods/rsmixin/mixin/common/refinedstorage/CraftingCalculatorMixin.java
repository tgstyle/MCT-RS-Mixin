package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.helper.refinedstorage.DamageAwareCrafting;

import com.refinedmods.refinedstorage.api.util.IStackList;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.task.v6.calculator.CraftingCalculator;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftingCalculator.class, remap = false) public abstract class CraftingCalculatorMixin {
    @SuppressWarnings({"unchecked", "rawtypes"}) @Redirect(method = "calculateForItems", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/api/util/IStackList;get(Ljava/lang/Object;)Ljava/lang/Object;")) private Object rsmixin$damageAwareGet(IStackList list, Object stack) {
        Object exact = list.get(stack);
        if (exact != null) { return exact; }
        if (!Config.ENABLE_DAMAGEABLE_INPUT_REUSE.get()) { return null; }
        if (!(stack instanceof ItemStack key) || !key.isDamageableItem()) { return null; }
        return DamageAwareCrafting.findInStackList((IStackList<ItemStack>) list, key);
    }
}
