package mctmods.rsmixin.mixin.common.rebornstorage;

import mctmods.rsmixin.Config;

import net.gigabit101.rebornstorage.multiblocks.MultiBlockCrafter;

import org.apache.logging.log4j.Logger;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiBlockCrafter.class)
public abstract class MultiBlockCrafterMixin {

    @Inject(method = "updateInfo",
            at = @At("HEAD"),
            remap = false)
    private void rsmixin$clearStaleBefore(String reason, CallbackInfo ci) {
        MultiBlockCrafter self = (MultiBlockCrafter)(Object)this;
        if (!Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) return;

        self.invs.clear();
    }

    @Inject(method = "updateInfo",
            at = @At("TAIL"),
            remap = false)
    private void rsmixin$cleanPhantomKeysClampAfter(String reason, CallbackInfo ci) {
        MultiBlockCrafter self = (MultiBlockCrafter)(Object)this;
        if (!Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) return;

        int pageCount = self.invs.size();
        self.invs.keySet().removeIf(key -> key > pageCount);
        self.pages = pageCount;

        if (self.invs.isEmpty()) {
            self.currentPage = 1;
        } else {
            self.currentPage = Math.max(1, Math.min(self.currentPage, pageCount));
        }
    }

    @Inject(method = "updateServer",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void rsmixin$disableUnnecessaryTicking(CallbackInfoReturnable<Boolean> cir) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
            method = "updateInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;)V"
            ),
            remap = false
    )
    private void rsmixin$serverOnlyLog(Logger logger, String message) {
        MultiBlockCrafter self = (MultiBlockCrafter)(Object)this;
        if (!self.level.isClientSide() && !message.contains("rsmixin")) {
            logger.info(message);
        }
    }
}
