package mctmods.rsmixin.mixin.client.refinedstorage;

import mctmods.rsmixin.Config;

import com.mojang.blaze3d.platform.InputConstants;
import com.refinedmods.refinedstorage.RSItems;
import com.refinedmods.refinedstorage.RSKeyBindings;
import com.refinedmods.refinedstorage.screen.KeyInputListener;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyInputListener.class, remap = false) public abstract class KeyInputListenerMixin {
    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true) private void rsmixin$modifierAwareHotkeys(InputEvent.Key e, CallbackInfo ci) {
        if (!Config.ENABLE_HOTKEY_MODIFIER_FIX.get()) { return; }
        ci.cancel();
        if (Minecraft.getInstance().player == null || e.getAction() != InputConstants.PRESS) { return; }
        InputConstants.Key key = InputConstants.getKey(e.getKey(), e.getScanCode());
        if (RSKeyBindings.OPEN_WIRELESS_GRID.isActiveAndMatches(key)) { KeyInputListener.findAndOpen(RSItems.WIRELESS_GRID.get(), RSItems.CREATIVE_WIRELESS_GRID.get()); }
        else if (RSKeyBindings.OPEN_WIRELESS_FLUID_GRID.isActiveAndMatches(key)) { KeyInputListener.findAndOpen(RSItems.WIRELESS_FLUID_GRID.get(), RSItems.CREATIVE_WIRELESS_FLUID_GRID.get()); }
        else if (RSKeyBindings.OPEN_PORTABLE_GRID.isActiveAndMatches(key)) { KeyInputListener.findAndOpen(RSItems.PORTABLE_GRID.get(), RSItems.CREATIVE_PORTABLE_GRID.get()); }
        else if (RSKeyBindings.OPEN_WIRELESS_CRAFTING_MONITOR.isActiveAndMatches(key)) { KeyInputListener.findAndOpen(RSItems.WIRELESS_CRAFTING_MONITOR.get(), RSItems.CREATIVE_WIRELESS_CRAFTING_MONITOR.get()); }
    }
}
