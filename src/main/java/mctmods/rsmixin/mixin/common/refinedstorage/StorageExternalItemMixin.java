package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.storage.externalstorage.StorageExternalItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Supplier;

@Mixin(value = StorageExternalItem.class, remap = false) public abstract class StorageExternalItemMixin {
    @Shadow private Supplier<IItemHandler> handlerSupplier;

    @Inject(method = "extract(Lnet/minecraft/item/ItemStack;IILcom/raoulvdberge/refinedstorage/api/util/Action;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"), cancellable = true) private void rsmixin$overstackAwareExtract(ItemStack stack, int size, int flags, Action action, CallbackInfoReturnable<ItemStack> cir) {
        if (!Config.enableOverstackExtractionFix) { return; }

        IItemHandler handler = handlerSupplier.get();
        if (handler == null) {
            cir.setReturnValue(null);
            return;
        }

        int remaining = size;
        ItemStack received = null;

        for (int i = 0; i < handler.getSlots() && remaining > 0; ++i) {
            ItemStack slot = handler.getStackInSlot(i);
            if (slot.isEmpty()) { continue; }
            if (received == null) {
                if (!API.instance().getComparer().isEqual(slot, stack, flags)) { continue; }
            }
            else if (!API.instance().getComparer().isEqualNoQuantity(slot, received)) { continue; }

            if (action == Action.SIMULATE) {
                ItemStack got = handler.extractItem(i, remaining, true);
                if (got.isEmpty()) { continue; }
                int credit = got.getCount();
                if (credit < remaining && credit == got.getMaxStackSize() && slot.getCount() > credit) { credit = Math.min(remaining, slot.getCount()); }
                if (received == null) { received = ItemHandlerHelper.copyStackWithSize(got, credit); }
                else { received.grow(credit); }
                remaining -= credit;
            }
            else {
                while (remaining > 0) {
                    ItemStack current = handler.getStackInSlot(i);
                    if (current.isEmpty()) { break; }
                    if (received != null && !API.instance().getComparer().isEqualNoQuantity(current, received)) { break; }
                    ItemStack got = handler.extractItem(i, remaining, false);
                    if (got.isEmpty()) { break; }
                    if (received == null) { received = got.copy(); }
                    else { received.grow(got.getCount()); }
                    remaining -= got.getCount();
                }
            }
        }

        cir.setReturnValue(received);
    }
}
