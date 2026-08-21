package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.CraftingPatternFactory;
import com.refinedmods.refinedstorage.item.PatternItem;
import com.refinedmods.refinedstorage.util.ItemStackKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

@Mixin(value = PatternItem.class, remap = false) public abstract class PatternItemMixin {
    @Shadow @Final private static Map<ItemStackKey, ICraftingPattern> CACHE;
    @Unique private static final Object rsmixin$CACHE_LOCK = new Object();

    @Inject(method = "fromCache", at = @At("HEAD"), cancellable = true) private static void rsmixin$threadSafeFromCache(Level level, ItemStack stack, CallbackInfoReturnable<ICraftingPattern> cir) {
        if (!Config.ENABLE_PATTERN_CACHE_THREAD_SAFETY.get()) { return; }
        synchronized (rsmixin$CACHE_LOCK) {
            ICraftingPattern pattern = CACHE.computeIfAbsent(new ItemStackKey(stack), s -> CraftingPatternFactory.INSTANCE.create(level, null, s.getStack()));
            if (CACHE.size() > 16384) { CACHE.clear(); }
            cir.setReturnValue(pattern);
        }
    }
}
