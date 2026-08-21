package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeCrafterManager;
import com.raoulvdberge.refinedstorage.container.ContainerCrafterManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ContainerCrafterManager.class, remap = false) public abstract class ContainerCrafterManagerMixin {
    @Shadow private NetworkNodeCrafterManager crafterManager;

    @Inject(method = "transferStackInSlot", at = @At("HEAD"), cancellable = true, remap = true) private void rsmixin$guardInactiveInsert(EntityPlayer player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!Config.enableCrafterManagerInsertGuard) { return; }
        if (index < 9 * 4 && (crafterManager == null || !crafterManager.isActive())) { cir.setReturnValue(ItemStack.EMPTY); }
    }
}
