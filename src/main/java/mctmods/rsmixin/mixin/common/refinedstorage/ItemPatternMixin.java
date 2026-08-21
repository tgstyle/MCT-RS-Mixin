package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.helper.refinedstorage.PatternCache;

import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingPattern;
import com.raoulvdberge.refinedstorage.item.ItemPattern;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemPattern.class, remap = false) public abstract class ItemPatternMixin {
    @Inject(method = "getPatternFromCache", at = @At("HEAD"), cancellable = true) private static void rsmixin$valueKeyedCache(World world, ItemStack stack, CallbackInfoReturnable<CraftingPattern> cir) {
        if (!Config.enablePatternCacheFix) { return; }
        cir.setReturnValue(PatternCache.get(world, stack));
    }
}
