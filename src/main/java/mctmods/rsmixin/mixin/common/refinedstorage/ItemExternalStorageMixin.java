package mctmods.rsmixin.mixin.common.refinedstorage;

import mctmods.rsmixin.Config;

import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.apiimpl.storage.externalstorage.ItemExternalStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Supplier;

@Mixin(value = ItemExternalStorage.class, remap = false) public abstract class ItemExternalStorageMixin {
    @Shadow @Final private Supplier<IItemHandler> handlerSupplier;

    @Inject(method = "extract(Lnet/minecraft/world/item/ItemStack;IILcom/refinedmods/refinedstorage/api/util/Action;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true) private void rsmixin$drainOverstackedSlots(ItemStack stack, int size, int flags, Action action, CallbackInfoReturnable<ItemStack> cir) {
        if (!Config.ENABLE_OVERSTACK_EXTRACTION_FIX.get()) { return; }
        if (stack.isEmpty()) { cir.setReturnValue(stack); return; }
        IItemHandler handler = handlerSupplier.get();
        if (handler == null) { cir.setReturnValue(ItemStack.EMPTY); return; }
        int remaining = size;
        ItemStack received = ItemStack.EMPTY;
        for (int i = 0; i < handler.getSlots() && remaining > 0; ++i) {
            ItemStack slot = handler.getStackInSlot(i);
            if (slot.isEmpty() || !API.instance().getComparer().isEqual(slot, stack, flags)) { continue; }
            if (!received.isEmpty() && !API.instance().getComparer().isEqual(slot, received, IComparer.COMPARE_NBT)) { continue; }
            if (action == Action.SIMULATE) {
                ItemStack got = handler.extractItem(i, remaining, true);
                if (!got.isEmpty()) {
                    int obtainable = got.getCount() >= got.getMaxStackSize() ? Math.min(remaining, slot.getCount()) : got.getCount();
                    if (received.isEmpty()) {
                        received = got.copy();
                        received.setCount(obtainable);
                    }
                    else { received.grow(obtainable); }
                    remaining -= obtainable;
                }
            }
            else {
                ItemStack got;
                while (remaining > 0 && !(got = handler.extractItem(i, remaining, false)).isEmpty()) {
                    if (received.isEmpty()) { received = got.copy(); }
                    else { received.grow(got.getCount()); }
                    remaining -= got.getCount();
                }
            }
        }
        cir.setReturnValue(received);
    }
}
