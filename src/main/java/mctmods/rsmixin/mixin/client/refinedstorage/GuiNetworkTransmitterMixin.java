package mctmods.rsmixin.mixin.client.refinedstorage;

import mctmods.rsmixin.Config;

import com.raoulvdberge.refinedstorage.gui.GuiNetworkTransmitter;
import com.raoulvdberge.refinedstorage.tile.TileNetworkTransmitter;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GuiNetworkTransmitter.class, remap = false) public abstract class GuiNetworkTransmitterMixin {
    @Shadow private TileNetworkTransmitter networkTransmitter;

    @Redirect(method = "drawForeground", at = @At(value = "INVOKE", target = "Lcom/raoulvdberge/refinedstorage/gui/GuiNetworkTransmitter;drawString(IILjava/lang/String;)V", ordinal = 1)) private void rsmixin$renderBlockedStatus(GuiNetworkTransmitter screen, int x, int y, String message) {
        boolean crossDim = !networkTransmitter.getNode().getNetworkCard().getStackInSlot(0).isEmpty()
                && TileNetworkTransmitter.RECEIVER_DIMENSION.getValue() != networkTransmitter.getWorld().provider.getDimension();

        if (Config.enableWirelessDimensionLock && crossDim) {
            screen.drawString(x, y, I18n.format("gui.rsmixin.network_transmitter.dimension_blocked"), 0xB02E26);
            return;
        }
        screen.drawString(x, y, message);
    }
}
