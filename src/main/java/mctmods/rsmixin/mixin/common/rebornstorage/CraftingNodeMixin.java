package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;

import net.gigabit101.rebornstorage.nodes.CraftingNode;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingNode.class)
public abstract class CraftingNodeMixin {

    @Shadow private boolean needsRebuild;

    @Unique private boolean rsmixin$patternChanged;

    @Inject(method = "rebuildPatterns",
            at = @At("HEAD"),
            remap = false)
    private void rsmixin$resetChanged(String reason, CallbackInfo ci) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) {
            this.rsmixin$patternChanged = false;
        }
    }

    @Inject(method = "rebuildPatterns",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingPatternProvider;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingPatternContainer;)Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingPattern;"),
            remap = false)
    private void rsmixin$markCreated(String reason, CallbackInfo ci) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) {
            this.rsmixin$patternChanged = true;
        }
    }

    @Inject(method = "rebuildPatterns",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/HashMap;remove(Ljava/lang/Object;)Ljava/lang/Object;"),
            remap = false)
    private void rsmixin$markRemoved(String reason, CallbackInfo ci) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) {
            this.rsmixin$patternChanged = true;
        }
    }

    @Inject(method = "rebuildPatterns",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/network/INetwork;getCraftingManager()Lcom/refinedmods/refinedstorage/api/autocrafting/ICraftingManager;"),
            cancellable = true,
            remap = false)
    private void rsmixin$cancelUnnecessaryQueue(String reason, CallbackInfo ci) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() && !this.rsmixin$patternChanged) {
            ci.cancel();
        }
    }

    @Inject(method = "update",
            at = @At("RETURN"),
            remap = false)
    private void rsmixin$clearNeedsRebuildIfUnchanged(CallbackInfo ci) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() && this.needsRebuild && !this.rsmixin$patternChanged) {
            this.needsRebuild = false;
        }
    }
}
