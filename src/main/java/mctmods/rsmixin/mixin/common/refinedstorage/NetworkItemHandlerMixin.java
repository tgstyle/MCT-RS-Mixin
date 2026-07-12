package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.apiimpl.network.item.NetworkItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkItemHandler.class, priority = 1500, remap = false) public abstract class NetworkItemHandlerMixin {
    @Shadow private INetwork network;

    @Inject(method = "open", at = @At("HEAD"), cancellable = true) private void rsmixin$dimensionLock(EntityPlayer player, ItemStack stack, int slotId, CallbackInfo ci) {
        if (!Config.enableWirelessDimensionLock) { return; }
        if (network.world() == null) { return; }

        int networkDimension = network.world().provider.getDimension();
        if (player.dimension == networkDimension) { return; }

        String dimensionLabel = network.world().provider.getDimensionType().getName() + " (dim " + networkDimension + ")";
        player.sendStatusMessage(new TextComponentTranslation("misc.rsmixin.wireless_dimension_lock", dimensionLabel).setStyle(new Style().setColor(TextFormatting.RED)), true);
        ci.cancel();
    }
}
