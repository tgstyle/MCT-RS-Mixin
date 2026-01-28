package mctmods.rsmixin.mixin.client.rebornstorage;

import mctmods.rsmixin.Config;

import net.gigabit101.rebornstorage.client.screens.ScreenMultiCrafter;
import net.gigabit101.rebornstorage.multiblocks.MultiBlockCrafter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenMultiCrafter.class)
public abstract class ScreenMultiCrafterMixin {

    @Shadow MultiBlockCrafter crafter;
    @Shadow Button buttonNext;
    @Shadow Button buttonBack;

    @Inject(method = "render",
            at = @At("HEAD"))
    private void rsmixin$liveButtonUpdate(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() || this.crafter == null) return;

        boolean hasStorage = !this.crafter.invs.isEmpty();

        this.buttonNext.visible = hasStorage;
        this.buttonBack.visible = hasStorage;
        this.buttonNext.active = hasStorage && this.crafter.currentPage < this.crafter.invs.size();
        this.buttonBack.active = hasStorage && this.crafter.currentPage > 1;
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"))
    private int rsmixin$noStorageMessage(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, Component text, int x, int y, int color, boolean shadow) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()
                && this.crafter != null
                && this.crafter.invs.isEmpty()
                && text.getString().startsWith("Page ")) {
            Component msg = Component.literal("No Storage Blocks!");
            return guiGraphics.drawString(font, msg, x, y, 0xFF5555, false);
        }
        return guiGraphics.drawString(font, text, x, y, color, shadow);
    }
}
