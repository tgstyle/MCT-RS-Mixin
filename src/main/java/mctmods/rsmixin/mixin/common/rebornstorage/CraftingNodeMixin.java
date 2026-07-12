package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;

import me.modmuss50.rebornstorage.tiles.CraftingNode;
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
    @Unique private int rsmixin$rebuildCounter = 0;

    @Shadow public abstract void rebuildPatterns(String reason);

    @Inject(method = "update", at = @At("HEAD")) private void rsmixin$deterministicRebuildTimer(CallbackInfo ci) {
        if (!Config.enableRebornstorageCrafterFix) { return; }
        if (needsRebuild) {
            rsmixin$rebuildCounter++;
            if (rsmixin$rebuildCounter >= CraftingNode.invUpdateTime * 20) {
                rebuildPatterns("rsmixin scheduled rebuild");
                needsRebuild = false;
                rsmixin$rebuildCounter = 0;
            }
        }
        else { rsmixin$rebuildCounter = 0; }
    }

    @Inject(method = "rebuildPatterns", at = @At("HEAD")) private void rsmixin$resetChanged(String reason, CallbackInfo ci) {
        if (Config.enableRebornstorageCrafterFix) { this.rsmixin$patternChanged = false; }
    }

    @Inject(method = "rebuildPatterns", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/api/autocrafting/ICraftingPatternProvider;create(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;Lcom/raoulvdberge/refinedstorage/api/autocrafting/ICraftingPatternContainer;)Lcom/raoulvdberge/refinedstorage/api/autocrafting/ICraftingPattern;")) private void rsmixin$markCreated(String reason, CallbackInfo ci) {
        if (Config.enableRebornstorageCrafterFix) { this.rsmixin$patternChanged = true; }
    }

    @Redirect(method = "rebuildPatterns", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;remove(Ljava/lang/Object;)Ljava/lang/Object;")) private Object rsmixin$trackRemove(HashMap<?, ?> map, Object key) {
        Object removed = map.remove(key);
        if (Config.enableRebornstorageCrafterFix && removed != null) { this.rsmixin$patternChanged = true; }
        return removed;
    }

    @Redirect(method = "rebuildPatterns", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;clear()V")) private void rsmixin$trackClear(HashMap<?, ?> map) {
        if (Config.enableRebornstorageCrafterFix && !map.isEmpty()) { this.rsmixin$patternChanged = true; }
        map.clear();
    }

    @Inject(method = "rebuildPatterns", at = @At(value = "INVOKE", target = "Lme/modmuss50/rebornstorage/RebornStorageEventHandler;queue(Lcom/raoulvdberge/refinedstorage/api/autocrafting/ICraftingManager;Lme/modmuss50/rebornstorage/tiles/CraftingNode;Ljava/lang/String;)V"), cancellable = true) private void rsmixin$cancelUnnecessaryQueue(String reason, CallbackInfo ci) {
        if (Config.enableRebornstorageCrafterFix && !this.rsmixin$patternChanged) { ci.cancel(); }
    }
}
