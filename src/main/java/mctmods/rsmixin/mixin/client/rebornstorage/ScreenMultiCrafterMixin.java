package mctmods.rsmixin.mixin.client.rebornstorage;

import mctmods.rsmixin.Config;

import net.gigabit101.rebornstorage.client.screens.ScreenMultiCrafter;
import net.gigabit101.rebornstorage.multiblocks.MultiBlockCrafter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenMultiCrafter.class) public abstract class ScreenMultiCrafterMixin {
    @Shadow(remap = false) MultiBlockCrafter crafter;
    @Shadow(remap = false) Button buttonNext;
    @Shadow(remap = false) Button buttonBack;

    @Unique private int rsmixin$noticeX = -1;
    @Unique private int rsmixin$noticeY = -1;
    @Unique private int rsmixin$noticeWidth;

    @Inject(method = "render", at = @At("HEAD")) private void rsmixin$liveButtonUpdate(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() || this.crafter == null) { return; }

        boolean hasStorage = !this.crafter.invs.isEmpty();
        this.buttonNext.visible = hasStorage;
        this.buttonBack.visible = hasStorage;
        this.buttonNext.active = hasStorage && this.crafter.currentPage < this.crafter.invs.size();
        this.buttonBack.active = hasStorage && this.crafter.currentPage > 1;
        if (!hasStorage) { return; }
        this.rsmixin$noticeX = -1;
        this.rsmixin$noticeY = -1;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I")) private int rsmixin$noStorageMessage(GuiGraphics guiGraphics, Font pFont, Component pText, int pX, int pY, int pColor, boolean pDropShadow) {
        if (Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get()
                && this.crafter != null
                && this.crafter.invs.isEmpty()
                && pText.getString().startsWith("Page ")) {
            Component msg = Component.translatable("misc.rsmixin.rebornstorage.no_storage");
            this.rsmixin$noticeX = pX;
            this.rsmixin$noticeY = pY;
            this.rsmixin$noticeWidth = pFont.width(msg);
            return guiGraphics.drawString(pFont, msg, pX, pY, 0xFF5555, false);
        }
        return guiGraphics.drawString(pFont, pText, pX, pY, pColor, pDropShadow);
    }

    @Inject(method = "render", at = @At("RETURN")) private void rsmixin$noStorageTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!Config.ENABLE_REBORNSTORAGE_CRAFTER_FIX.get() || this.rsmixin$noticeX < 0) { return; }
        if (mouseX < this.rsmixin$noticeX || mouseX > this.rsmixin$noticeX + this.rsmixin$noticeWidth) { return; }
        if (mouseY < this.rsmixin$noticeY || mouseY > this.rsmixin$noticeY + 9) { return; }

        Font font = Minecraft.getInstance().font;
        guiGraphics.renderTooltip(font, Component.translatable("misc.rsmixin.rebornstorage.no_storage_tip"), mouseX, mouseY);
    }
}
