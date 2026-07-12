package mctmods.rsmixin.mixin.client.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.blockentity.NetworkTransmitterBlockEntity;
import com.refinedmods.refinedstorage.screen.NetworkTransmitterScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

@Mixin(value = NetworkTransmitterScreen.class, remap = false) public abstract class NetworkTransmitterScreenMixin {
    @Redirect(method = "renderForeground", at = @At(value = "INVOKE", target = "Lcom/refinedmods/refinedstorage/screen/NetworkTransmitterScreen;renderString(Lnet/minecraft/client/gui/GuiGraphics;IILjava/lang/String;)V", ordinal = 1)) private void rsmixin$renderBlockedStatus(NetworkTransmitterScreen screen, GuiGraphics graphics, int x, int y, String text) {
        Optional<ResourceLocation> receiverDim = NetworkTransmitterBlockEntity.RECEIVER_DIMENSION.getValue();
        int distance = NetworkTransmitterBlockEntity.DISTANCE.getValue();
        boolean crossDim = receiverDim.isPresent() && distance == -1;

        if (Config.ENABLE_WIRELESS_DIMENSION_LOCK.get() && crossDim) {
            graphics.drawString(Minecraft.getInstance().font, I18n.get("gui.rsmixin.network_transmitter.dimension_blocked"), x, y, 0xB02E26, false);
            return;
        }
        screen.renderString(graphics, x, y, text);
    }
}
