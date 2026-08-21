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
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(value = PatternItem.class, remap = false) public abstract class PatternItemMixin {
    @Shadow @Final private static Map<ItemStackKey, ICraftingPattern> CACHE;
    @Unique private static final Object rsmixin$CACHE_LOCK = new Object();
    @Unique private static final Map<ItemStack, ICraftingPattern> rsmixin$RENDER_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    @Inject(method = "fromCache", at = @At("HEAD"), cancellable = true) private static void rsmixin$threadSafeFromCache(Level level, ItemStack stack, CallbackInfoReturnable<ICraftingPattern> cir) {
        boolean renderCache = Config.ENABLE_PATTERN_RENDER_CACHE.get() && level != null && level.isClientSide;
        if (renderCache) {
            ICraftingPattern cached = rsmixin$RENDER_CACHE.get(stack);
            if (cached != null) {
                cir.setReturnValue(cached);
                return;
            }
        }
        if (!Config.ENABLE_PATTERN_CACHE_THREAD_SAFETY.get() && !renderCache) { return; }
        synchronized (rsmixin$CACHE_LOCK) {
            ICraftingPattern pattern = CACHE.computeIfAbsent(new ItemStackKey(stack), s -> CraftingPatternFactory.INSTANCE.create(level, null, s.getStack()));
            if (CACHE.size() > 16384) {
                CACHE.clear();
                rsmixin$RENDER_CACHE.clear();
            }
            if (renderCache) { rsmixin$RENDER_CACHE.put(stack, pattern); }
            cir.setReturnValue(pattern);
        }
    }
}
