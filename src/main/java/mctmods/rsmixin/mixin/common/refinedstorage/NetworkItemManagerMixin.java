package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.apiimpl.network.item.NetworkItemManager;
import com.refinedmods.refinedstorage.inventory.player.PlayerSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkItemManager.class, priority = 1500, remap = false) public abstract class NetworkItemManagerMixin {
    @Shadow @Final private INetwork network;

    @Inject(method = "open", at = @At("HEAD"), cancellable = true) private void rsmixin$dimensionLock(Player player, ItemStack stack, PlayerSlot slot, CallbackInfo ci) {
        if (!Config.ENABLE_WIRELESS_DIMENSION_LOCK.get()) { return; }
        if (network.getLevel() == null) { return; }

        ResourceKey<Level> networkDimension = network.getLevel().dimension();
        if (player.getCommandSenderWorld().dimension() == networkDimension) { return; }

        player.displayClientMessage(Component.translatable("misc.rsmixin.wireless_dimension_lock", networkDimension.location().toString()).withStyle(ChatFormatting.RED), true);
        ci.cancel();
    }
}
