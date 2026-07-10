package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;

import net.gigabit101.rebornstorage.nodes.CraftingNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashMap;

@Mixin(value = CraftingNode.class, remap = false) public abstract class CraftingNodeMixin {
    @Shadow private boolean needsRebuild;
    @Unique private boolean rsmixin$patternChanged;

    @Inject(method = "rebuildPatterns", at = @At("HEAD")) private void rsmixin$resetChanged(String reason, CallbackInfo ci) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) { this.rsmixin$patternChanged = false; }
    }

    @Inject(method = "rebuildPatterns", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingPatternProvider;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingPatternContainer;)Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingPattern;")) private void rsmixin$markCreated(String reason, CallbackInfo ci) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) { this.rsmixin$patternChanged = true; }
    }

    @Redirect(method = "rebuildPatterns", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;remove(Ljava/lang/Object;)Ljava/lang/Object;")) private Object rsmixin$trackRemove(HashMap<?, ?> map, Object key) {
        Object removed = map.remove(key);
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() && removed != null) { this.rsmixin$patternChanged = true; }
        return removed;
    }

    @Redirect(method = "rebuildPatterns", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;clear()V")) private void rsmixin$trackClear(HashMap<?, ?> map) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() && !map.isEmpty()) { this.rsmixin$patternChanged = true; }
        map.clear();
    }

    @Inject(method = "rebuildPatterns", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/api/network/INetwork;getCraftingManager()Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingManager;"), cancellable = true) private void rsmixin$cancelUnnecessaryQueue(String reason, CallbackInfo ci) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() && !this.rsmixin$patternChanged) { ci.cancel(); }
    }

    @Inject(method = "update", at = @At("RETURN")) private void rsmixin$clearNeedsRebuildIfUnchanged(CallbackInfo ci) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() && this.needsRebuild && !this.rsmixin$patternChanged) { this.needsRebuild = false; }
    }
}
